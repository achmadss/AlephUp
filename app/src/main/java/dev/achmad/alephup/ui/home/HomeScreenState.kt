package dev.achmad.alephup.ui.home

import dev.achmad.core.device.wifi.WifiConnectionInfo
import dev.achmad.data.attendance.PostAttendanceResult

data class HomeScreenState(
    val wifiConnectionInfo: WifiConnectionInfo? = null,
    val result: PostAttendanceResult? = null,
    val loading: Boolean = true,
)