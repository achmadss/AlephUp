package dev.achmad.data.attendance

import dev.achmad.core.BASE_URL
import dev.achmad.core.BSSID_TARGETS
import dev.achmad.core.network.GET
import dev.achmad.core.network.NetworkHelper
import dev.achmad.core.network.await
import dev.achmad.core.util.extension.injectLazy
import java.time.LocalDate

class PostAttendance(
    private val networkHelper: NetworkHelper
) {
    suspend fun await(bssid: String) {
        if (bssid !in BSSID_TARGETS) return
        val attendancePreference by injectLazy<AttendancePreference>()
        val lastAttendancePreference = attendancePreference.lastAttendance()
        val attendedPreference = attendancePreference.attended()
        val today = LocalDate.now()
        if (lastAttendancePreference.get() != today && !attendedPreference.get()) {
            val response = networkHelper.client.newCall(
                GET(BASE_URL) // TODO use real url
            ).await()
            if (response.isSuccessful) {
                lastAttendancePreference.set(today)
                attendedPreference.set(true)
            }
        }
    }
}