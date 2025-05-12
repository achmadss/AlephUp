package dev.achmad.alephup.ui.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.achmad.alephup.device.BatteryOptimizationHelper
import dev.achmad.alephup.device.BootCompletedPreference
import dev.achmad.alephup.device.WifiConnectionInfo
import dev.achmad.alephup.device.WifiMonitor
import dev.achmad.alephup.device.WifiMonitorService
import dev.achmad.alephup.device.WifiState
import dev.achmad.core.util.inject
import dev.achmad.data.attendance.AttendancePreference
import dev.achmad.data.attendance.PostAttendance
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
    private val batteryOptimizationHelper: BatteryOptimizationHelper = inject(),
    bootCompletedPreference: BootCompletedPreference = inject(),
    attendancePreference: AttendancePreference = inject(),
): ViewModel() {

    private val mutableState = MutableStateFlow(HomeScreenState())
    val state = mutableState.asStateFlow()

    val serviceEnabledOnBoot = bootCompletedPreference.serviceEnabledOnBoot().stateIn(viewModelScope)
    val lastAttendance = attendancePreference.lastAttendance().stateIn(viewModelScope)

    init {
        observeWifiChanges()
    }

    private fun observeWifiChanges() = viewModelScope.launch {
        wifiMonitor.getWifiStateFlow().collect { wifiState ->
            when(wifiState) {
                is WifiState.Init,
                is WifiState.Disconnected -> mutableState.update { HomeScreenState() }
                is WifiState.Connected -> {
                    val wifiInfo = wifiState.wifiInfo
                    mutableState.update {
                        it.copy(
                            wifiConnectionInfo = wifiInfo
                        )
                    }
                    if (!WifiMonitorService.isRunning.value) {
                        PostAttendance.await(wifiInfo.bssid)
                    }
                }
                is WifiState.InfoChanged -> {
                    mutableState.update {
                        it.copy(
                            wifiConnectionInfo = wifiState.wifiInfo
                        )
                    }
                }
            }
        }
    }

    fun updateBatteryOptimization() {
        mutableState.update {
            it.copy(
                isIgnoringBatteryOptimization = batteryOptimizationHelper.isIgnoringBatteryOptimizations(),
            )
        }
    }

    fun requestBatteryOptimizationExclusionIntent(): Intent {
        return batteryOptimizationHelper.requestBatteryOptimizationExclusion()
    }

    fun openBatteryOptimizationSettingsIntent(): Intent {
        return batteryOptimizationHelper.openBatteryOptimizationSettings()
    }

}