package dev.achmad.data.attendance

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

sealed interface PostAttendanceResult {
    data object Loading: PostAttendanceResult
    data object Success: PostAttendanceResult
    data object InvalidSSID: PostAttendanceResult
    data object HttpError: PostAttendanceResult
}

class PostAttendance {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channel = Channel<String>(capacity = Channel.UNLIMITED)

    private val wifiHelper by injectLazy<WifiHelper>()
    private val networkHelper by injectLazy<NetworkHelper>()
    private val attendancePreference by injectLazy<AttendancePreference>()

    private val mutableResult = MutableStateFlow<PostAttendanceResult>(PostAttendanceResult.Loading)
    val result = mutableResult.asStateFlow()

    private val mutableResultEvents = MutableSharedFlow<PostAttendanceResult>(replay = 0)
    val resultEvents = mutableResultEvents.asSharedFlow()

    init {
        // Collect Wi-Fi changes
        scope.launch {
            wifiHelper.getWifiStateFlow().collect { wifiState ->
                if (wifiState is WifiState.Connected) {
                    execute(wifiState.wifiInfo.ssid)
                }
            }
        }

        // Collect retry requests (both from Wi-Fi and manual retry)
        scope.launch {
            for (ssid in channel) {
                executeFlow(ssid).collect { result ->
                    mutableResult.value = result
                    mutableResultEvents.emit(result)
                }
            }
        }
    }

    fun execute(ssid: String) {
        channel.trySend(ssid)
    }

    private fun executeFlow(ssid: String): Flow<PostAttendanceResult> = flow {
        emit(PostAttendanceResult.Loading)
        delay(500)

        if (ssid != SSID_TARGET) {
            emit(PostAttendanceResult.InvalidSSID)
            return@flow
        }

        val lastAttendancePreference = attendancePreference.lastAttendance()
        val attended = attendancePreference.attended()

        if (!attended) {
            try {
                val response = networkHelper.client.newCall(
                    GET(BASE_URL.plus("?ssid=$ssid"))
                ).await()

                if (!response.isSuccessful) {
                    emit(PostAttendanceResult.HttpError)
                    return@flow
                }

                lastAttendancePreference.set(LocalDate.now())
            } catch (e: IOException) {
                emit(PostAttendanceResult.HttpError)
                return@flow
            }
        }

        emit(PostAttendanceResult.Success)
    }
}
