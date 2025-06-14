package dev.achmad.alephup.ui.checkin

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.achmad.core.device.wifi.WifiHelper
import dev.achmad.core.device.wifi.WifiState
import dev.achmad.core.util.extension.inject
import dev.achmad.data.auth.Auth
import dev.achmad.data.checkin.CheckIn
import dev.achmad.data.checkin.CheckInResult
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CheckInScreenModel(
    shouldCheckIn: Boolean = false,
    private val wifiHelper: WifiHelper = inject(),
    private val checkIn: CheckIn = inject(),
    private val auth: Auth = inject(),
): StateScreenModel<CheckInScreenState>(CheckInScreenState()) {

    init {
        observeWifiChanges()
        observeCheckInResult()
        if (shouldCheckIn) retryCheckIn()
    }

    fun getCurrentUser() = auth.getCurrentUser()

    private fun observeWifiChanges() = screenModelScope.launch {
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

    private fun observeCheckInResult() = screenModelScope.launch {
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

    fun retryCheckIn() {
        checkIn.execute()
    }

}