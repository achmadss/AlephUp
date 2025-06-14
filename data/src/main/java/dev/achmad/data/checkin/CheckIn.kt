package dev.achmad.data.checkin

import dev.achmad.core.Constants
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
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

    // source of truth
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

        // small delay for UX
        delay(500)

        // validate SSID
        if (ssid != Constants.Device.Wifi.SSID_TARGET) {
            emit(CheckInResult.InvalidSSID)
            return@flow
        }

        // check if already checked in today
        if (checkInPreference.checkedInToday()) {
            emit(CheckInResult.Success)
            return@flow
        }

        // perform network check-in
        try {
            val response = networkHelper.client.newCall(
                GET("${Constants.Network.BASE_URL}?ssid=$ssid") // TODO real endpoint
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