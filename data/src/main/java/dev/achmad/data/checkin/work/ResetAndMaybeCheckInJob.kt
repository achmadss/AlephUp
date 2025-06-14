package dev.achmad.data.checkin.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import dev.achmad.core.device.wifi.WifiHelper
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.core.util.extension.workManager
import dev.achmad.data.auth.Auth
import dev.achmad.data.checkin.CheckIn
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ResetAndMaybeCheckInJob(
    context: Context,
    workerParams: WorkerParameters
): CoroutineWorker(context, workerParams) {

    private val checkIn by injectLazy<CheckIn>()
    private val auth by injectLazy<Auth>()
    private val wifiHelper by injectLazy<WifiHelper>()

    override suspend fun doWork(): Result {
        val wifiInfo = wifiHelper.currentWifiInfo
        if (wifiInfo.connected && auth.isSignedIn()) {
            checkIn.execute()
        }
        return Result.success()
    }

    companion object {

        private const val RESET_CHECKED_IN_JOB = "RESET_CHECKED_IN_JOB"

        fun scheduleNow(context: Context) {
            val workManager = context.workManager
            workManager.cancelAllWorkByTag(RESET_CHECKED_IN_JOB)

            val now = LocalDateTime.now()
            val targetTime = now.toLocalDate()
                .plusDays(1)
                .atTime(0, 0, 0, 0)
            val firstRun = when {
                now.isBefore(targetTime) -> targetTime
                else -> targetTime.plusDays(1)
            }
            val delayMinutes = Duration.between(now, firstRun).toMinutes()

            val workRequest = PeriodicWorkRequestBuilder<ResetAndMaybeCheckInJob>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                flexTimeIntervalUnit = TimeUnit.MILLISECONDS
            )
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag(RESET_CHECKED_IN_JOB)
                .build()

            workManager.enqueueUniquePeriodicWork(
                RESET_CHECKED_IN_JOB,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

    }

}