package dev.achmad.alephup.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.achmad.alephup.device.BootCompletedPreference
import dev.achmad.alephup.device.WifiConnectionInfo
import dev.achmad.alephup.device.WifiMonitor
import dev.achmad.alephup.device.WifiMonitorService
import dev.achmad.core.TARGET_BSSID
import dev.achmad.core.network.GET
import dev.achmad.core.network.NetworkHelper
import dev.achmad.core.network.await
import dev.achmad.core.util.inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeScreenState(
    val wifiConnectionInfo: WifiConnectionInfo? = null,
    val isIgnoringBatteryOptimization: Boolean = false,
    val serviceEnabled: Boolean = false,
)

class HomeViewModel(
    private val wifiMonitor: WifiMonitor = inject(),
    private val bootCompletedPreference: BootCompletedPreference = inject(),
    private val networkHelper: NetworkHelper = inject()
): ViewModel() {

    private val tag: String = this::class.java.simpleName

    private val mutableState = MutableStateFlow(HomeScreenState())
    val state = mutableState.asStateFlow()

    val serviceEnabled = bootCompletedPreference.serviceEnabled().stateIn(viewModelScope)

    init {
        if (!WifiMonitorService.isRunning.value) {
            wifiMonitor.startMonitoring()
        }
        observeWifiChanges()
    }

    private fun observeWifiChanges() = viewModelScope.launch {
        wifiMonitor.getWifiInfoFlow().collect { wifiInfo ->
            mutableState.update { it.copy(wifiConnectionInfo = wifiInfo) }
            if (!WifiMonitorService.isRunning.value) {
                checkAndExecuteTasksForWifi(wifiInfo)
            }
        }
    }
    private fun checkAndExecuteTasksForWifi(wifiInfo: WifiConnectionInfo) {
        val isTargetNetwork = when {
            wifiInfo.bssid == TARGET_BSSID -> true
            else -> false
        }

        if (isTargetNetwork) {
            attend()
        }
    }

    private fun attend() = viewModelScope.launch {
        networkHelper.client.newCall(
            GET("https://achmad.dev")
        ).await()
    }


    fun updateScreenState(
        transform: (HomeScreenState) -> HomeScreenState
    ) = mutableState.update(transform)

    override fun onCleared() {
        super.onCleared()
        wifiMonitor.stopMonitoring()
    }

}