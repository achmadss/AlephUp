package dev.achmad.alephup.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.achmad.core.device.wifi.WifiHelper
import dev.achmad.core.device.wifi.WifiState
import dev.achmad.core.util.extension.inject
import dev.achmad.data.attendance.AttendancePreference
import dev.achmad.data.attendance.PostAttendance
import dev.achmad.data.attendance.PostAttendanceResult
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
        observePostAttendanceResult()
    }

    private fun observeWifiChanges() = viewModelScope.launch {
        wifiHelper.getWifiStateFlow().collect { wifiState ->
            when(wifiState) {
                is WifiState.Init -> mutableState.update { HomeScreenState() }
                is WifiState.Disconnected -> mutableState.update {
                    it.copy(
                        wifiConnectionInfo = null,
                        result = null,
                        loading = false,
                    )
                }
                is WifiState.Connected -> {
                    val wifiInfo = wifiState.wifiInfo
                    mutableState.update {
                        it.copy(wifiConnectionInfo = wifiInfo)
                    }
                }
                is WifiState.InfoChanged -> {
                    mutableState.update {
                        it.copy(wifiConnectionInfo = wifiState.wifiInfo)
                    }
                }
            }
        }
    }

    private fun observePostAttendanceResult() = viewModelScope.launch {
        postAttendance.result.collect { result ->
            val loading = result is PostAttendanceResult.Loading
            mutableState.update {
                it.copy(
                    result = result,
                    loading = loading
                )
            }
        }
    }

    fun retryPostAttendance() {
        if (attendancePreference.attended()) return
        val wifiInfo = state.value.wifiConnectionInfo ?: return
        postAttendance.execute(wifiInfo.ssid)
    }

}