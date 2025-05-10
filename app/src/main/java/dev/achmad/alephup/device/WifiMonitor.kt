package dev.achmad.alephup.device

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
 * Data class representing Wi-Fi connection information
 */
data class WifiConnectionInfo(
    val ssid: String = "",
    val bssid: String = "", // MAC address
    val ipAddress: String = "",
    val linkSpeed: Int = 0,
    val frequency: Int = 0,
    val signalStrength: Int = 0,
    val networkId: Int = -1,
    val capabilities: String = "",
    val isConnected: Boolean = false
)

/**
 * Helper class to monitor Wi-Fi connections and provide callbacks for network changes
 */
@SuppressLint("MissingPermission")
class WifiMonitor(private val context: Context) {
    
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _currentWifiInfo = MutableStateFlow(WifiConnectionInfo())
    val currentWifiInfo: StateFlow<WifiConnectionInfo> = _currentWifiInfo
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wifiReceiver: BroadcastReceiver? = null
    
    /**
     * Interface for Wi-Fi connection change callbacks
     */
    interface WifiConnectionListener {
        fun onWifiConnected(wifiInfo: WifiConnectionInfo)
        fun onWifiDisconnected()
        fun onWifiInfoChanged(wifiInfo: WifiConnectionInfo)
        fun onWifiCapabilitiesChanged(capabilities: String)
    }
    
    private val listeners = mutableListOf<WifiConnectionListener>()
    
    /**
     * Add a listener for Wi-Fi connection changes
     */
    fun addListener(listener: WifiConnectionListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    /**
     * Remove a listener for Wi-Fi connection changes
     */
    fun removeListener(listener: WifiConnectionListener) {
        listeners.remove(listener)
    }
    
    /**
     * Start monitoring Wi-Fi connections
     */
    fun startMonitoring() {
        // Modern API approach (preferred for Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
                
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    updateWifiInfo()
                }
                
                override fun onLost(network: Network) {
                    _currentWifiInfo.update { it.copy(isConnected = false) }
                    notifyDisconnected()
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    updateWifiInfo()
                    _currentWifiInfo.value.let { 
                        notifyCapabilitiesChanged(it.capabilities)
                    }
                }
            }
            
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
            
            // Initial update
            updateWifiInfo()
        } else {
            // Legacy approach for older Android versions
            val wifiIntentFilter = IntentFilter().apply {
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(WifiManager.RSSI_CHANGED_ACTION)
            }
            
            wifiReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        WifiManager.NETWORK_STATE_CHANGED_ACTION, 
                        WifiManager.WIFI_STATE_CHANGED_ACTION,
                        WifiManager.RSSI_CHANGED_ACTION -> {
                            updateWifiInfo()
                        }
                    }
                }
            }
            
            context.registerReceiver(wifiReceiver, wifiIntentFilter)
            
            // Initial update
            updateWifiInfo()
        }
    }
    
    /**
     * Stop monitoring Wi-Fi connections
     */
    fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
        
        wifiReceiver?.let {
            context.unregisterReceiver(it)
            wifiReceiver = null
        }

    }
    
    /**
     * Check if the current Wi-Fi connection matches the given SSID
     */
    fun isConnectedToSSID(ssid: String): Boolean {
        return _currentWifiInfo.value.isConnected && _currentWifiInfo.value.ssid == ssid
    }
    
    /**
     * Check if the current Wi-Fi connection matches the given BSSID (MAC address)
     */
    fun isConnectedToBSSID(bssid: String): Boolean {
        return _currentWifiInfo.value.isConnected && _currentWifiInfo.value.bssid == bssid
    }
    
    /**
     * Check if the current Wi-Fi connection matches the given network ID
     */
    fun isConnectedToNetworkId(networkId: Int): Boolean {
        return _currentWifiInfo.value.isConnected && _currentWifiInfo.value.networkId == networkId
    }
    
    /**
     * Check if the current Wi-Fi connection matches any of the specified criteria
     */
    fun isConnectedToNetwork(
        ssid: String? = null,
        bssid: String? = null,
        networkId: Int? = null
    ): Boolean {
        if (!_currentWifiInfo.value.isConnected) return false
        
        return when {
            ssid != null -> _currentWifiInfo.value.ssid == ssid
            bssid != null -> _currentWifiInfo.value.bssid == bssid
            networkId != null -> _currentWifiInfo.value.networkId == networkId
            else -> false
        }
    }
    
    /**
     * Update the current Wi-Fi information
     */
    @Suppress("DEPRECATION")
    private fun updateWifiInfo() {
        val prevInfo = _currentWifiInfo.value
        val newInfo: WifiConnectionInfo
        
        val wifiInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val network = connectivityManager.activeNetwork ?: return
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return
            
            // Check if this is a Wi-Fi connection
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                _currentWifiInfo.update { it.copy(isConnected = false) }
                notifyDisconnected()
                return
            }
            
            connectivityManager.getNetworkInfo(network) ?: return
            wifiManager.connectionInfo
        } else {
            wifiManager.connectionInfo
        }
        
        if (wifiInfo != null) {
            // Process SSID (remove quotes if present)
            var ssid = wifiInfo.ssid
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length - 1)
            }
            
            val ipAddress = intToIpAddress(wifiInfo.ipAddress)
            
            newInfo = WifiConnectionInfo(
                ssid = ssid,
                bssid = wifiInfo.bssid ?: "",
                ipAddress = ipAddress,
                linkSpeed = wifiInfo.linkSpeed,
                frequency = wifiInfo.frequency,
                signalStrength = WifiManager.calculateSignalLevel(wifiInfo.rssi, 100),
                networkId = wifiInfo.networkId,
                capabilities = getWifiCapabilities(wifiInfo),
                isConnected = true
            )
            
            _currentWifiInfo.update { newInfo }
            
            // Notify listeners
            if (!prevInfo.isConnected) {
                notifyConnected(newInfo)
            } else if (prevInfo != newInfo) {
                if (prevInfo.capabilities != newInfo.capabilities) {
                    notifyCapabilitiesChanged(newInfo.capabilities)
                }
                notifyInfoChanged(newInfo)
            }
        } else {
            _currentWifiInfo.update { it.copy(isConnected = false) }
            if (prevInfo.isConnected) {
                notifyDisconnected()
            }
        }
    }
    
    /**
     * Get Wi-Fi capabilities as a string
     */
    @Suppress("DEPRECATION")
    private fun getWifiCapabilities(wifiInfo: WifiInfo): String {
        val capabilities = StringBuilder()
        capabilities.append("Speed: ${wifiInfo.linkSpeed} Mbps")
        capabilities.append(", Frequency: ${wifiInfo.frequency} MHz")
        capabilities.append(", Signal: ${WifiManager.calculateSignalLevel(wifiInfo.rssi, 100)}%")
        return capabilities.toString()
    }
    
    /**
     * Convert integer IP address to string format
     */
    private fun intToIpAddress(ipAddress: Int): String {
        return "${ipAddress and 0xff}.${ipAddress shr 8 and 0xff}." +
                "${ipAddress shr 16 and 0xff}.${ipAddress shr 24 and 0xff}"
    }
    
    /**
     * Notify listeners that Wi-Fi has connected
     */
    private fun notifyConnected(wifiInfo: WifiConnectionInfo) {
        listeners.forEach { it.onWifiConnected(wifiInfo) }
    }
    
    /**
     * Notify listeners that Wi-Fi has disconnected
     */
    private fun notifyDisconnected() {
        listeners.forEach { it.onWifiDisconnected() }
    }
    
    /**
     * Notify listeners that Wi-Fi information has changed
     */
    private fun notifyInfoChanged(wifiInfo: WifiConnectionInfo) {
        listeners.forEach { it.onWifiInfoChanged(wifiInfo) }
    }
    
    /**
     * Notify listeners that Wi-Fi capabilities have changed
     */
    private fun notifyCapabilitiesChanged(capabilities: String) {
        listeners.forEach { it.onWifiCapabilitiesChanged(capabilities) }
    }
    
    /**
     * Get a Flow of Wi-Fi information updates
     */
    fun getWifiInfoFlow(): Flow<WifiConnectionInfo> = callbackFlow {
        val listener = object : WifiConnectionListener {
            override fun onWifiConnected(wifiInfo: WifiConnectionInfo) {
                trySend(wifiInfo)
            }
            
            override fun onWifiDisconnected() {
                trySend(WifiConnectionInfo(isConnected = false))
            }
            
            override fun onWifiInfoChanged(wifiInfo: WifiConnectionInfo) {
//                trySend(wifiInfo)
            }
            
            override fun onWifiCapabilitiesChanged(capabilities: String) {
                // Only send if the capabilities are the only thing that changed
            }
        }
        
        addListener(listener)
        
        // Send initial state
        trySend(_currentWifiInfo.value)
        
        // Clean up when flow collection stops
        awaitClose {
            removeListener(listener)
        }
    }
}