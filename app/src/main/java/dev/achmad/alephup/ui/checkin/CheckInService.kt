package dev.achmad.alephup.ui.checkin

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.data.checkin.CheckIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that monitors Wi-Fi connections and executes tasks when connected to specific networks
 */
class CheckInService: Service() {

    private val checkIn by injectLazy<CheckIn>()

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notifier: CheckInNotifier

    override fun onCreate() {
        super.onCreate()

        // initiate notifier
        notifier = CheckInNotifier(this)

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
        // start notifying check-in results
        serviceScope.launch {
            checkIn.checkInResultSharedFlow.collect { result ->
                notifier.notifyCheckInResult(result)
            }
        }

        startForeground(
            CheckInNotifier.SERVICE_NOTIFICATION_ID,
            notifier.serviceNotification,
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
        serviceScope.cancel() // cancel coroutines
        isRunning = false
        super.onDestroy()
    }

    companion object {
        private const val WAKE_LOCK_TAG = "AlephUp:CheckInWakeLock"

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