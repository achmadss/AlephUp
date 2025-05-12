package dev.achmad.alephup.device

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import dev.achmad.alephup.base.MainActivity
import dev.achmad.core.util.inject
import dev.achmad.data.attendance.PostAttendance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that monitors Wi-Fi connections and executes tasks when connected to specific networks
 */
class WifiMonitorService : Service() {

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
    }
    
    private val wifiMonitor = inject<WifiMonitor>()
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Create and acquire wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire(10*60*1000L) // timeout in 10 minutes
        }

    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForeground(NOTIFICATION_ID, createNotification())
                isRunning.value = true
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }

        serviceScope.launch {
            wifiMonitor.getWifiStateFlow().collect { wifiState ->
                when(wifiState) {
                    is WifiState.Connected -> {
                        val wifiInfo = wifiState.wifiInfo
                        updateNotification("Connected to: ${wifiInfo.ssid}")
                        serviceScope.launch {
                            PostAttendance.await(wifiInfo.bssid)
                        }
                    }
                    is WifiState.Disconnected -> {
                        updateNotification("Not connected to Wi-Fi")
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
        
        // Cancel coroutines
        serviceScope.cancel()

        isRunning.value = false
        
        super.onDestroy()
    }
    
    /**
     * Create notification channel for foreground service (required for Android O+)
     */
    private fun createNotificationChannel() {
        val name = "Background Attendance Service"
        val descriptionText = "Verify Wi-Fi connection in the background to attend automatically"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(false)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        
        val status = if (wifiMonitor.currentWifiInfo.value.isConnected) {
            "Connected to: ${wifiMonitor.currentWifiInfo.value.ssid}"
        } else {
            "Not Connected"
        }
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AlephUp")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    /**
     * Update the notification with new status
     */
    private fun updateNotification(status: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AlephUp")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, 
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .build()
            
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}