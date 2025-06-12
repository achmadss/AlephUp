package dev.achmad.core.device.wifi

/**
 * Represents the various states of Wi-Fi connectivity that can be emitted by the [WifiHelper].
 */
sealed interface WifiState {
    data object Init : WifiState
    /** Indicates that Wi-Fi has connected, providing current connection details. */
    data class Connected(val wifiInfo: WifiConnectionInfo) : WifiState
    /** Indicates that Wi-Fi has disconnected. */
    data object Disconnected : WifiState
    /** Indicates that Wi-Fi connection information has changed while remaining connected. */
    data class InfoChanged(val wifiInfo: WifiConnectionInfo) : WifiState
}