package dev.achmad.alephup.ui.home

import dev.achmad.core.device.wifi.WifiConnectionInfo

data class HomeScreenState(
    val wifiConnectionInfo: WifiConnectionInfo? = null,
    val loading: Boolean = false,
)