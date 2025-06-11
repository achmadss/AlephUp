package dev.achmad.data.attendance

import dev.achmad.core.BASE_URL
import dev.achmad.core.SSID_TARGET
import dev.achmad.core.network.GET
import dev.achmad.core.network.NetworkHelper
import dev.achmad.core.network.await
import dev.achmad.core.util.extension.injectLazy
import okio.IOException
import java.time.LocalDate

sealed interface PostAttendanceResult {
    data object Success: PostAttendanceResult
    data object InvalidSSID: PostAttendanceResult
    data object HttpError: PostAttendanceResult
}

class PostAttendance(
    private val networkHelper: NetworkHelper
) {
    suspend fun await(ssid: String): PostAttendanceResult {
//        if (bssid !in BSSID_TARGETS) return
        if (ssid != SSID_TARGET) return PostAttendanceResult.InvalidSSID
        val attendancePreference by injectLazy<AttendancePreference>()
        val lastAttendancePreference = attendancePreference.lastAttendance()
        val attended = attendancePreference.attended()
        if (!attended) {
            try {
                val response = networkHelper.client.newCall(
                    GET(BASE_URL) // TODO use real url
                ).await()
                if (!response.isSuccessful) {
                    return PostAttendanceResult.HttpError
                }
                lastAttendancePreference.set(LocalDate.now())
            } catch (e: IOException) {
                return PostAttendanceResult.HttpError
            }
        }
        return PostAttendanceResult.Success
    }
}