package dev.achmad.core.device.wifi

/**
 * Data class representing Wi-Fi connection information.
 */
data class WifiConnectionInfo(
    val ssid: String = "",
    val bssid: String = "", // MAC address
    val ipAddress: String = "",
    val linkSpeed: Int = 0, // Mbps
    val frequency: Int = 0, // MHz
    val signalStrength: Int = 0, // Calculated signal level (typically 0-99 or similar range)
    val networkId: Int = -1,
    val capabilities: String = "", // Formatted string of capabilities
    val connected: Boolean = false
)