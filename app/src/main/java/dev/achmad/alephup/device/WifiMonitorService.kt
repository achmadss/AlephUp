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
import dev.achmad.core.network.GET
import dev.achmad.core.network.NetworkHelper
import dev.achmad.core.network.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Foreground service that monitors Wi-Fi connections and executes tasks when connected to specific networks
 */
class WifiMonitorService : Service(), WifiMonitor.WifiConnectionListener {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "wifi_monitor_channel"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TAG = "AlephUp:WifiMonitorWakeLock"
        
        // Intent actions
        const val ACTION_START_SERVICE = "dev.achmad.alephup.ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "dev.achmad.alephup.ACTION_STOP_SERVICE"
        
        // Intent extras
        const val EXTRA_TARGET_BSSID = "target_bssid"

        var isRunning = mutableStateOf(false)
        
        /**
         * Start the Wi-Fi monitor service
         */
        fun startService(context: Context, targetBssid: String? = null) {
            val intent = Intent(context, WifiMonitorService::class.java).apply {
                action = ACTION_START_SERVICE
                putExtra(EXTRA_TARGET_BSSID, targetBssid)
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
    
    private val wifiMonitor: WifiMonitor by inject()
    private val networkHelper: NetworkHelper = dev.achmad.core.util.inject()

    private var targetBssid: String? = null
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        wifiMonitor.addListener(this)
        
        // Create and acquire wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire()
        }
        
        // Start monitoring Wi-Fi connections
        wifiMonitor.startMonitoring()

    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                // Get target Wi-Fi information
                targetBssid = intent.getStringExtra(EXTRA_TARGET_BSSID)

                // Start as a foreground service
                startForeground(NOTIFICATION_ID, createNotification())
                isRunning.value = true
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }
        
        // Check current connection
        val currentWifi = wifiMonitor.currentWifiInfo.value
        if (currentWifi.isConnected) {
            checkAndExecuteTasksForWifi(currentWifi)
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
        
        // Clean up Wi-Fi monitor
        wifiMonitor.removeListener(this)
        wifiMonitor.stopMonitoring()
        
        // Cancel coroutines
        serviceScope.cancel()

        isRunning.value = false
        
        super.onDestroy()
    }
    
    override fun onWifiConnected(wifiInfo: WifiConnectionInfo) {
        checkAndExecuteTasksForWifi(wifiInfo)
        updateNotification("Connected to: ${wifiInfo.ssid}")
    }
    
    override fun onWifiDisconnected() {
        updateNotification("Not connected to Wi-Fi")
    }
    
    override fun onWifiInfoChanged(wifiInfo: WifiConnectionInfo) {
        // Only perform actions if the network matches our target
//        checkAndExecuteTasksForWifi(wifiInfo)
    }
    
    override fun onWifiCapabilitiesChanged(capabilities: String) {
        // Not needed for this implementation
    }
    
    /**
     * Check if the Wi-Fi connection matches our target and execute tasks if it does
     */
    private fun checkAndExecuteTasksForWifi(wifiInfo: WifiConnectionInfo) {
        val isTargetNetwork = when {
            targetBssid != null && wifiInfo.bssid == targetBssid -> true
            else -> false
        }
        
        if (isTargetNetwork) {
            // Execute your tasks here
            executeBackgroundTasks(wifiInfo)
        }
    }
    
    /**
     * Execute your background tasks when connected to the target Wi-Fi
     */
    private fun executeBackgroundTasks(wifiInfo: WifiConnectionInfo) {
        serviceScope.launch {
            // TODO: Implement your specific background tasks here
            // For example:
            // - Sync data with a server
            // - Perform local processing
            // - Check for updates
            // - etc.
            networkHelper.client.newCall(
                GET("https://achmad.dev")
            ).await()
        }
    }
    
    /**
     * Create notification channel for foreground service (required for Android O+)
     */
    private fun createNotificationChannel() {
        val name = "Wi-Fi Monitor"
        val descriptionText = "Monitors Wi-Fi connections to perform background tasks"
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
            "Monitoring Wi-Fi connections"
        }
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AlephUp Wi-Fi Monitor")
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
            .setContentTitle("AlephUp Wi-Fi Monitor")
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