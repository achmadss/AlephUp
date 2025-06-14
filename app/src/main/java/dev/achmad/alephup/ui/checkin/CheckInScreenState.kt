package dev.achmad.alephup.ui.checkin

import dev.achmad.core.device.wifi.WifiConnectionInfo
import dev.achmad.data.checkin.CheckInResult

data class CheckInScreenState(
    val wifiConnectionInfo: WifiConnectionInfo? = null,
    val result: CheckInResult? = null,
    val loading: Boolean = true,
)