package dev.achmad.data.checkin

import dev.achmad.core.BASE_URL
import dev.achmad.core.SSID_TARGET
import dev.achmad.core.device.wifi.WifiHelper
import dev.achmad.core.device.wifi.WifiState
import dev.achmad.core.network.GET
import dev.achmad.core.network.NetworkHelper
import dev.achmad.core.network.await
import dev.achmad.core.util.extension.injectLazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.IOException
import java.time.LocalDate

sealed interface CheckInResult {
    data object Loading : CheckInResult
    data object Success : CheckInResult
    data object InvalidSSID : CheckInResult
    data object HttpError : CheckInResult
}

class CheckIn {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val wifiHelper by injectLazy<WifiHelper>()
    private val networkHelper by injectLazy<NetworkHelper>()
    private val checkInPreference by injectLazy<CheckInPreference>()

    // Single source of truth
    private val _checkInResult = MutableSharedFlow<CheckInResult>(
        replay = 1, // StateFlow behavior - always has latest value
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // StateFlow-like behavior: always get the latest state
    val checkInResultStateFlow = _checkInResult
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = CheckInResult.Loading
        )

    // SharedFlow behavior: only new emissions
    val checkInResultSharedFlow = _checkInResult
        .drop(1) // Skip the initial/replay value
        .shareIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            replay = 0
        )

    init {
        scope.launch {
            wifiHelper.getWifiStateFlow()
                .filterIsInstance<WifiState.Connected>()
                .distinctUntilChangedBy { it.wifiInfo.ssid }
                .collect { wifiState ->
                    execute(wifiState.wifiInfo.ssid)
                }
        }
    }

    fun execute(ssid: String) {
        scope.launch {
            executeFlow(ssid).collect { result ->
                _checkInResult.emit(result)
            }
        }
    }

    private fun executeFlow(ssid: String): Flow<CheckInResult> = flow {
        emit(CheckInResult.Loading)

        // Small delay for UX
        delay(500)

        // Validate SSID
        if (ssid != SSID_TARGET) {
            emit(CheckInResult.InvalidSSID)
            return@flow
        }

        // Check if already checked in today
        if (checkInPreference.checkedInToday()) {
            emit(CheckInResult.Success)
            return@flow
        }

        // Perform network check-in
        try {
            val response = networkHelper.client.newCall(
                GET("$BASE_URL?ssid=$ssid")
            ).await()

            if (response.isSuccessful) {
                checkInPreference.lastCheckedIn().set(LocalDate.now())
                emit(CheckInResult.Success)
            } else {
                emit(CheckInResult.HttpError)
            }
        } catch (e: IOException) {
            emit(CheckInResult.HttpError)
        }
    }

}