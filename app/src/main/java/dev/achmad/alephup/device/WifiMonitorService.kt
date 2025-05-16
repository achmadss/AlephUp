package dev.achmad.alephup.device

import android.R
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.compose.runtime.mutableStateOf
import dev.achmad.alephup.base.MainActivity
import dev.achmad.alephup.util.NotificationHelper
import dev.achmad.core.util.injectLazy
import dev.achmad.data.attendance.PostAttendance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that monitors Wi-Fi connections and executes tasks when connected to specific networks
 */
class WifiMonitorService: Service() {

    private val wifiMonitor by injectLazy<WifiMonitor>()
    private val notificationHelper by injectLazy<NotificationHelper>()
    private val postAttendance by injectLazy<PostAttendance>()

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationData = NotificationHelper.Data(
        channelId = NOTIFICATION_CHANNEL_ID,
        title = "AlephUp",
        text = getStatus(),
        smallIconResId = R.drawable.ic_dialog_info,
        context = this,
        pendingIntent = notificationHelper.createActivityPendingIntent(
            requestCode = 0,
            activityClass = MainActivity::class.java,
            context = this
        ),
        onGoing = true,
    )
    
    override fun onCreate() {
        super.onCreate()
        // Create and acquire wake lock
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire(10*60*1000L) // timeout in 10 minutes
        }
        isRunning.value = true
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForeground(
                    NOTIFICATION_ID,
                    notificationHelper.createNotification(notificationData)
                )
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }

        serviceScope.launch {
            wifiMonitor.getWifiStateFlow().collect { wifiState ->
                notificationHelper.notify(
                    NOTIFICATION_ID,
                    notificationHelper.createNotification(
                        notificationData.copy(
                            text = getStatus()
                        )
                    )
                )
                when(wifiState) {
                    is WifiState.Connected -> {
                        serviceScope.launch {
                            val wifiInfo = wifiState.wifiInfo
                            postAttendance.await(wifiInfo.bssid)
                        }
                    }
                    else -> Unit
                }
            }
        }
        
        // If service is killed, restart it
        return START_STICKY
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
        isRunning.value = false
        super.onDestroy()
    }

    private fun getStatus() = when {
        wifiMonitor.currentWifiInfo.value.isConnected -> "Connected to: ${wifiMonitor.currentWifiInfo.value.ssid}"
        else -> "Not Connected"
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "wifi_monitor_channel"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TAG = "AlephUp:WifiMonitorWakeLock"

        // Intent actions
        const val ACTION_START_SERVICE = "dev.achmad.alephup.ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "dev.achmad.alephup.ACTION_STOP_SERVICE"

        var isRunning = mutableStateOf(false)

        /**
         * Start the Wi-Fi monitor service
         */
        fun startService(context: Context) {
            val intent = Intent(context, WifiMonitorService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            context.startForegroundService(intent)
        }

        /**
         * Stop the Wi-Fi monitor service
         */
        fun stopService(context: Context) {
            val intent = Intent(context, WifiMonitorService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
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