package dev.achmad.alephup.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.achmad.core.device.wifi.WifiHelper
import dev.achmad.core.device.wifi.WifiState
import dev.achmad.core.util.extension.inject
import dev.achmad.data.auth.Auth
import dev.achmad.data.checkin.CheckIn
import dev.achmad.data.checkin.CheckInPreference
import dev.achmad.data.checkin.CheckInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CheckInScreenViewModel(
    private val wifiHelper: WifiHelper = inject(),
    private val checkIn: CheckIn = inject(),
    private val checkInPreference: CheckInPreference = inject(),
    private val auth: Auth = inject(),
): ViewModel() {

    private val mutableState = MutableStateFlow(CheckInScreenState())
    val state = mutableState.asStateFlow()

    init {
        observeWifiChanges()
        observePostAttendanceResult()
    }

    fun getCurrentUser() = auth.getCurrentUser()

    private fun observeWifiChanges() = viewModelScope.launch {
        wifiHelper.getWifiStateFlow().collect { wifiState ->
            when(wifiState) {
                is WifiState.Init -> mutableState.update { CheckInScreenState() }
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
        checkIn.checkInResultStateFlow.collect { result ->
            val loading = result is CheckInResult.Loading
            mutableState.update {
                it.copy(
                    result = result,
                    loading = loading
                )
            }
        }
    }

    fun retryPostAttendance() {
        if (checkInPreference.checkedInToday()) return
        val wifiInfo = state.value.wifiConnectionInfo ?: return
        checkIn.execute(wifiInfo.ssid)
    }

}