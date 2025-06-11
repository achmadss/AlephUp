package dev.achmad.alephup.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.achmad.alephup.base.service.AttendanceService
import dev.achmad.core.device.wifi.WifiHelper
import dev.achmad.core.device.wifi.WifiState
import dev.achmad.core.util.extension.inject
import dev.achmad.data.attendance.AttendancePreference
import dev.achmad.data.attendance.PostAttendance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeScreenViewModel(
    private val wifiHelper: WifiHelper = inject(),
    private val postAttendance: PostAttendance = inject(),
    private val attendancePreference: AttendancePreference = inject()
): ViewModel() {

    private val mutableState = MutableStateFlow(HomeScreenState())
    val state = mutableState.asStateFlow()

    init {
        observeWifiChanges()
    }

    fun tryPostAttendance() = viewModelScope.launch {
        if (attendancePreference.attended()) return@launch
        val wifiInfo = mutableState.value.wifiConnectionInfo ?: return@launch
        mutableState.update { it.copy(loading = true) }
        delay(500) // TODO remove delay
        val result = postAttendance.await(wifiInfo.ssid)
        mutableState.update {
            it.copy(
                result = result,
                loading = false
            )
        }
    }

    private fun observeWifiChanges() = viewModelScope.launch {
        wifiHelper.getWifiStateFlow().collect { wifiState ->
            when(wifiState) {
                is WifiState.Disconnected -> mutableState.update { HomeScreenState(loading = false) }
                is WifiState.Connected -> {
                    val wifiInfo = wifiState.wifiInfo
                    mutableState.update { it.copy(wifiConnectionInfo = wifiInfo) }
                    if (!AttendanceService.isRunning) {
                        tryPostAttendance()
                    }
                }
                is WifiState.InfoChanged -> {
                    mutableState.update { it.copy(wifiConnectionInfo = wifiState.wifiInfo) }
                }
            }
        }
    }

}