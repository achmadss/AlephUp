package dev.achmad.alephup.ui.checkin

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
import dev.achmad.alephup.ui.checkin.CheckInNotifier.Companion.NOTIFICATION_CHANNEL_ID
import dev.achmad.core.device.notification.NotificationHelper
import dev.achmad.core.util.extension.injectLazy

/**
 * Foreground service that monitors Wi-Fi connections and executes tasks when connected to specific networks
 */
class CheckInService: Service() {

    private val notificationHelper by injectLazy<NotificationHelper>()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        // create and acquire wake lock
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire(10*60*1000L) // timeout in 10 minutes
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val serviceNotification = notificationHelper.createNotification(
            NotificationHelper.Data(
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
        )

        startForeground(
            SERVICE_NOTIFICATION_ID,
            serviceNotification,
        )

        isRunning = true
        
        // if service is killed, DO NOT restart it
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        // release wake lock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        isRunning = false
        super.onDestroy()
    }

    companion object {
        private const val WAKE_LOCK_TAG = "AlephUp:CheckInWakeLock"
        private const val SERVICE_NOTIFICATION_ID = 1001

        var isRunning by mutableStateOf(false)

        /**
         * Start the Wi-Fi monitor service
         */
        fun startService(context: Context) {
            val intent = Intent(context, CheckInService::class.java)
            context.startForegroundService(intent)
        }

        /**
         * Stop the Wi-Fi monitor service
         */
        fun stopService(context: Context) {
            val intent = Intent(context, CheckInService::class.java)
            context.stopService(intent)
        }
    }
}