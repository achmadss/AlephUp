package dev.achmad.data.attendance.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import dev.achmad.core.device.wifi.WifiHelper
import dev.achmad.core.util.extension.workManager
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.data.attendance.AttendancePreference
import dev.achmad.data.attendance.PostAttendance
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ResetAndMaybePostAttendanceJob(
    context: Context,
    workerParams: WorkerParameters
): CoroutineWorker(context, workerParams) {

    private val postAttendance by injectLazy<PostAttendance>()
    private val wifiHelper by injectLazy<WifiHelper>()

    override suspend fun doWork(): Result {
        val wifiInfo = wifiHelper.currentWifiInfo.value
        if (wifiInfo.connected) {
//            postAttendance.await(wifiInfo.bssid)
            postAttendance.await(wifiInfo.ssid)
        }
        return Result.success()
    }

    companion object {

        private const val RESET_ATTENDANCE_JOB = "RESET_ATTENDANCE_JOB"

        fun scheduleNow(context: Context) {
            val workManager = context.workManager
            workManager.cancelAllWorkByTag(RESET_ATTENDANCE_JOB)

            val now = LocalDateTime.now()
            val targetTime = now.toLocalDate()
                .plusDays(1)
                .atTime(0, 0, 0, 0)
            val firstRun = when {
                now.isBefore(targetTime) -> targetTime
                else -> targetTime.plusDays(1)
            }
            val delayMinutes = Duration.between(now, firstRun).toMinutes()

            val workRequest = PeriodicWorkRequestBuilder<ResetAndMaybePostAttendanceJob>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                flexTimeIntervalUnit = TimeUnit.MILLISECONDS
            )
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag(RESET_ATTENDANCE_JOB)
                .build()

            workManager.enqueueUniquePeriodicWork(
                RESET_ATTENDANCE_JOB,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

    }

}