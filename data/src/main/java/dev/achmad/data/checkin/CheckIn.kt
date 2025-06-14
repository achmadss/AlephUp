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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okio.IOException
import java.time.LocalDate

sealed interface CheckInResult {
    data object Loading: CheckInResult
    data object Success: CheckInResult
    data object InvalidSSID: CheckInResult
    data object HttpError: CheckInResult
}

class CheckIn {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channel = Channel<String>(capacity = Channel.UNLIMITED)

    private val wifiHelper by injectLazy<WifiHelper>()
    private val networkHelper by injectLazy<NetworkHelper>()
    private val checkInPreference by injectLazy<CheckInPreference>()

    private val _checkInResultStateFlow = MutableStateFlow<CheckInResult>(CheckInResult.Loading)
    val checkInResultStateFlow = _checkInResultStateFlow.asStateFlow()

    private val _checkInResultSharedFlow = MutableSharedFlow<CheckInResult>(replay = 0)
    val checkInResultSharedFlow = _checkInResultSharedFlow.asSharedFlow()

    init {
        scope.launch {
            wifiHelper.getWifiStateFlow().collect { wifiState ->
                if (wifiState is WifiState.Connected) {
                    execute(wifiState.wifiInfo.ssid)
                }
            }
        }
        scope.launch {
            for (ssid in channel) {
                executeFlow(ssid).collect { result ->
                    _checkInResultStateFlow.value = result
                    _checkInResultSharedFlow.emit(result)
                }
            }
        }
    }

    fun execute(ssid: String) {
        channel.trySend(ssid)
    }

    private fun executeFlow(ssid: String): Flow<CheckInResult> = flow {
        emit(CheckInResult.Loading)
        delay(500)

        if (ssid != SSID_TARGET) {
            emit(CheckInResult.InvalidSSID)
            return@flow
        }

        val lastCheckedInPreference = checkInPreference.lastCheckedIn()
        val checkedInToday = checkInPreference.checkedInToday()

        if (!checkedInToday) {
            try {
                val response = networkHelper.client.newCall(
                    GET(BASE_URL.plus("?ssid=$ssid"))
                ).await()

                if (!response.isSuccessful) {
                    emit(CheckInResult.HttpError)
                    return@flow
                }

                lastCheckedInPreference.set(LocalDate.now())
            } catch (e: IOException) {
                emit(CheckInResult.HttpError)
                return@flow
            }
        }

        emit(CheckInResult.Success)
    }
}
