package dev.achmad.core.device.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update

/**
 * Helper class to monitor Wi-Fi connections and provide a Flow of [WifiState] changes.
 *
 * **Permissions Required:**
 * - `ACCESS_WIFI_STATE`: To access Wi-Fi state and information.
 * - `ACCESS_NETWORK_STATE`: To access network state and connectivity.
 * - `ACCESS_FINE_LOCATION` (Android 8.0+): For obtaining SSID and BSSID. Location services
 *   must also be enabled on the device.
 *
 * **Note on Android S (API 31+):** Access to `WifiInfo` fields like SSID, BSSID, link speed, etc.,
 * may be restricted or return placeholder values if the app does not have specific privileged
 * permissions (`NETWORK_SETTINGS`, `NETWORK_SETUP_WIZARD`) or `ACCESS_FINE_LOCATION`.
 * This class attempts to fetch the best available information.
 */
@SuppressLint("MissingPermission") // Caller is responsible for ensuring permissions.
class WifiHelper(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _currentWifiInfo = MutableStateFlow(WifiConnectionInfo())
    /**
     * A [StateFlow] providing the most recent [WifiConnectionInfo].
     * This is updated by the internal monitoring logic and can be observed directly,
     * though [getWifiStateFlow] is recommended for event-driven updates.
     */
    val currentWifiInfo: StateFlow<WifiConnectionInfo> = _currentWifiInfo

    /**
     * Queries system services to get current Wi-Fi details and updates `_currentWifiInfo`.
     * Returns the updated [WifiConnectionInfo].
     */
    @Suppress("DEPRECATION") // For WifiInfo and some ConnectivityManager.getNetworkInfo calls
    private fun refreshAndGetCurrentWifiInfo(): WifiConnectionInfo {
        val systemWifiInfo: WifiInfo?
        var isActuallyConnected = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    isActuallyConnected = true
                    systemWifiInfo = wifiManager.connectionInfo // May have restrictions on S+
                } else {
                    systemWifiInfo = null // Not a Wi-Fi network
                }
            } else {
                systemWifiInfo = null // No active network
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            if (networkInfo?.isConnected == true) {
                isActuallyConnected = true
                systemWifiInfo = wifiManager.connectionInfo
            } else {
                systemWifiInfo = null
            }
        }

        if (isActuallyConnected && systemWifiInfo != null &&
            systemWifiInfo.ssid != null && systemWifiInfo.ssid != WifiManager.UNKNOWN_SSID &&
            systemWifiInfo.bssid != null && systemWifiInfo.bssid != "00:00:00:00:00:00" // Common invalid BSSID
        ) {
            var ssid = systemWifiInfo.ssid
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length - 1)
            }
            // Re-check UNKNOWN_SSID after stripping quotes
            if (ssid == WifiManager.UNKNOWN_SSID) {
                _currentWifiInfo.update { WifiConnectionInfo() } // Default disconnected
                return _currentWifiInfo.value
            }


            val ipAddress = intToIpAddress(systemWifiInfo.ipAddress)
            val frequency = systemWifiInfo.frequency

            val rssi = systemWifiInfo.rssi
            // WifiManager.calculateSignalLevel returns an int from 0 to numLevels-1.
            // Using 100 levels to approximate percentage (0-99).
            val signalStrength = if (rssi != Int.MIN_VALUE && rssi != -127) { // Check for invalid RSSI
                WifiManager.calculateSignalLevel(rssi, 100)
            } else {
                0 // Default for invalid/unknown RSSI
            }

            val updatedInfo = WifiConnectionInfo(
                ssid = ssid,
                bssid = systemWifiInfo.bssid ?: "", // Should not be null due to earlier check
                ipAddress = ipAddress,
                linkSpeed = systemWifiInfo.linkSpeed,
                frequency = frequency,
                signalStrength = signalStrength,
                networkId = systemWifiInfo.networkId,
                capabilities = getWifiCapabilitiesString(systemWifiInfo, signalStrength),
                connected = true
            )
            _currentWifiInfo.update { updatedInfo }
        } else {
            // Not connected or invalid/insufficient Wi-Fi info
            _currentWifiInfo.update { WifiConnectionInfo() } // Default disconnected state
        }
        return _currentWifiInfo.value
    }

    /**
     * Generates a formatted string summarizing Wi-Fi capabilities.
     */
    private fun getWifiCapabilitiesString(wifiInfo: WifiInfo, calculatedSignalStrength: Int): String {
        val speed = wifiInfo.linkSpeed.takeIf { it != -1 }?.toString() ?: "N/A"
        val freq = wifiInfo.frequency.takeIf { it > 0 }?.toString() ?: "N/A"
        return "Speed: $speed Mbps, Frequency: $freq MHz, Signal: $calculatedSignalStrength%"
    }

    /**
     * Converts an integer representation of an IP address to its string format.
     */
    private fun intToIpAddress(ipAddress: Int): String {
        if (ipAddress == 0) return "" // Common for disconnected or unassigned IP
        return "${ipAddress and 0xff}.${ipAddress shr 8 and 0xff}." +
                "${ipAddress shr 16 and 0xff}.${ipAddress shr 24 and 0xff}"
    }

    /**
     * Provides a [Flow] that emits [WifiState] updates.
     *
     * The flow initiates Wi-Fi monitoring when it is collected and stops monitoring when the
     * collection is cancelled. Each new collector will start its own monitoring instance.
     * To share a single monitoring stream across multiple observers, apply operators like
     * `shareIn` or `stateIn` to the Flow returned by this function.
     *
     * Emitted states:
     * - [WifiState.Connected]: When a Wi-Fi connection is established or detected.
     * - [WifiState.Disconnected]: When the Wi-Fi connection is lost.
     * - [WifiState.InfoChanged]: When properties of an active Wi-Fi connection change (e.g., signal strength).
     */
    fun getWifiStateFlow(): Flow<WifiState> = callbackFlow {
        var previousWifiInfoState = _currentWifiInfo.value // Store initial state for comparison

        val internalNetworkCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    val newInfo = refreshAndGetCurrentWifiInfo()
                    // Ensure this 'available' network is indeed the one we are tracking (Wi-Fi)
                    if (newInfo.connected) {
                        trySend(WifiState.Connected(newInfo))
                    }
                    previousWifiInfoState = newInfo
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    val infoAfterLoss = refreshAndGetCurrentWifiInfo() // Re-evaluate current state
                    // Only send Disconnected if it was previously connected via this monitor's perspective
                    if (!infoAfterLoss.connected && previousWifiInfoState.connected) {
                        trySend(WifiState.Disconnected)
                    }
                    previousWifiInfoState = infoAfterLoss
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    // Ensure it's still a Wi-Fi network; otherwise, treat as lost.
                    if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        val infoAfterCapsChange = refreshAndGetCurrentWifiInfo()
                        if (!infoAfterCapsChange.connected && previousWifiInfoState.connected) {
                            trySend(WifiState.Disconnected)
                        }
                        previousWifiInfoState = infoAfterCapsChange
                        return
                    }

                    val newInfo = refreshAndGetCurrentWifiInfo()
                    if (!previousWifiInfoState.connected && newInfo.connected) {
                        previousWifiInfoState = newInfo
                        trySend(WifiState.Connected(newInfo))
                        return
                    }
                    previousWifiInfoState = newInfo
                    trySend(WifiState.InfoChanged(newInfo))
                }
            }
        } else null

        val internalWifiReceiver = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val newInfo = refreshAndGetCurrentWifiInfo()
                    when {
                        newInfo.connected && !previousWifiInfoState.connected ->
                            trySend(WifiState.Connected(newInfo))
                        !newInfo.connected && previousWifiInfoState.connected ->
                            trySend(WifiState.Disconnected)
                        newInfo.connected && newInfo != previousWifiInfoState ->
                            trySend(WifiState.InfoChanged(newInfo))
                    }
                    previousWifiInfoState = newInfo
                }
            }
        } else null

        // Register the appropriate callback/receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && internalNetworkCallback != null) {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, internalNetworkCallback)
        } else if (internalWifiReceiver != null) {
            val intentFilter = IntentFilter().apply {
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION) // For Wi-Fi adapter state
                addAction(WifiManager.RSSI_CHANGED_ACTION)
            }
            context.registerReceiver(internalWifiReceiver, intentFilter)
        }

        // Emit initial state after registration
        trySend(WifiState.Disconnected)

        // Cleanup: Unregister callbacks when the flow's collector is cancelled
        awaitClose {
            internalNetworkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
            internalWifiReceiver?.let { context.unregisterReceiver(it) }
        }
    }

    /**
     * Checks if the device has an active and validated internet connection through any network.
     */
    fun isConnectedToInternet(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Checks if currently connected to a Wi-Fi network matching all specified criteria.
     * If no criteria (all nulls) are provided, it checks if simply connected to any Wi-Fi.
     * Comparisons for SSID and BSSID are case-insensitive.
     */
    fun isConnectedToNetwork(
        ssid: String? = null,
        bssid: String? = null,
        networkId: Int? = null
    ): Boolean {
        val currentInfo = _currentWifiInfo.value
        if (!currentInfo.connected) return false

        // If no specific criteria, just being connected to Wi-Fi is enough
        if (ssid == null && bssid == null && networkId == null) {
            return true
        }

        val ssidMatch = ssid?.equals(currentInfo.ssid, ignoreCase = true) ?: true // True if null (not specified)
        val bssidMatch = bssid?.equals(currentInfo.bssid, ignoreCase = true) ?: true // True if null
        val networkIdMatch = networkId?.equals(currentInfo.networkId) ?: true // True if null

        return ssidMatch && bssidMatch && networkIdMatch
    }
}