package dev.achmad.alephup.base.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.achmad.alephup.R
import dev.achmad.alephup.base.MainActivity
import dev.achmad.core.device.notification.NotificationHelper
import dev.achmad.core.device.wifi.WifiHelper
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.data.attendance.PostAttendance
import dev.achmad.data.attendance.PostAttendanceResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that monitors Wi-Fi connections and executes tasks when connected to specific networks
 */
class AttendanceService: Service() {

    private val wifiHelper by injectLazy<WifiHelper>()
    private val notificationHelper by injectLazy<NotificationHelper>()
    private val postAttendance by injectLazy<PostAttendance>()

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var initialNotificationData: NotificationHelper.Data
    private lateinit var postAttendanceNotificationData: NotificationHelper.Data

    override fun onCreate() {
        super.onCreate()

        initialNotificationData = NotificationHelper.Data(
            channelId = NOTIFICATION_CHANNEL_ID,
            title = getString(R.string.run_in_background_notification),
            text = "",
            smallIconResId = R.drawable.ic_run_in_background,
            context = this,
            pendingIntent = notificationHelper.createActivityPendingIntent(
                requestCode = 0,
                activityClass = MainActivity::class.java,
                context = this
            ),
            onGoing = true
        )

        postAttendanceNotificationData = NotificationHelper.Data(
            channelId = NOTIFICATION_CHANNEL_ID,
            title = getString(R.string.post_attendance_success),
            text = "",
            smallIconResId = R.drawable.ic_attendance_success,
            context = this,
            pendingIntent = notificationHelper.createActivityPendingIntent(
                requestCode = 0,
                activityClass = MainActivity::class.java,
                context = this
            ),
            onGoing = true
        )

        // Create and acquire wake lock
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire(10*60*1000L) // timeout in 10 minutes
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            postAttendance.resultEvents.collect { result ->
                when(result) {
                    PostAttendanceResult.HttpError -> {
                        notificationHelper.notify(
                            POST_ATTENDANCE_NOTIFICATION_ID,
                            notificationHelper.createNotification(
                                postAttendanceNotificationData.copy(
                                    smallIconResId = R.drawable.ic_attendance_error,
                                    title = "Failed to connect to server",
                                )
                            )
                        )
                    }
                    PostAttendanceResult.InvalidSSID -> {
                        notificationHelper.notify(
                            POST_ATTENDANCE_NOTIFICATION_ID,
                            notificationHelper.createNotification(
                                postAttendanceNotificationData.copy(
                                    smallIconResId = R.drawable.ic_attendance_error,
                                    title = "Not connected to an Aleph Wifi",
                                )
                            )
                        )
                    }
                    PostAttendanceResult.Success -> {
                        notificationHelper.notify(
                            POST_ATTENDANCE_NOTIFICATION_ID,
                            notificationHelper.createNotification(postAttendanceNotificationData)
                        )
                    }
                    else -> Unit
                }
            }
        }

        startForeground(
            SERVICE_NOTIFICATION_ID,
            notificationHelper.createNotification(initialNotificationData)
        )

        isRunning = true
        
        // If service is killed, restart it
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        // Release wake lock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        serviceScope.cancel() // cancel coroutines
        isRunning = false
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "attendance_channel"
        private const val SERVICE_NOTIFICATION_ID = 1001
        private const val POST_ATTENDANCE_NOTIFICATION_ID = 1002
        private const val WAKE_LOCK_TAG = "AlephUp:AttendanceWakeLock"

        var isRunning by mutableStateOf(false)

        /**
         * Start the Wi-Fi monitor service
         */
        fun startService(context: Context) {
            val intent = Intent(context, AttendanceService::class.java)
            context.startForegroundService(intent)
        }

        /**
         * Stop the Wi-Fi monitor service
         */
        fun stopService(context: Context) {
            val intent = Intent(context, AttendanceService::class.java)
            context.stopService(intent)
        }

        fun createNotificationChannelConfig() = NotificationHelper.Channel(
            id = NOTIFICATION_CHANNEL_ID,
            name = "Background Attendance Service",
            description = "Verify Wi-Fi connection in the background to attend automatically",
            importance = NotificationManager.IMPORTANCE_LOW,
            showBadge = false,
        )
    }
}