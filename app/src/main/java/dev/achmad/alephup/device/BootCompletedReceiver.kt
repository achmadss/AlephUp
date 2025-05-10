package dev.achmad.alephup.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.achmad.core.util.inject

/**
 * BroadcastReceiver to start the WifiMonitorService when the device boots
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        const val KEY_SERVICE_ENABLED = "service_enabled"
        const val KEY_TARGET_BSSID = "target_bssid"

        private val preference = inject<BootCompletedPreference>()
        
        /**
         * Save service settings to start on boot
         */
        fun saveServiceSettings(
            enabled: Boolean,
            targetBssid: String? = null
        ) {
            if (targetBssid == null) return
            with(preference) {
                serviceEnabled().set(enabled)
                targetBSSID().set(targetBssid)
            }
        }
        
        /**
         * Clear service settings to prevent auto-start
         */
        fun clearServiceSettings() {
            with(preference) {
                serviceEnabled().set(false)
                targetBSSID().set("")
            }
        }
        
        /**
         * Get service settings
         */
        fun getServiceSettings(): ServiceSettings {
            return ServiceSettings(
                enabled = preference.serviceEnabled().get(),
                targetBssid = preference.targetBSSID().get()
            )
        }
    }
    
    data class ServiceSettings(
        val enabled: Boolean,
        val targetBssid: String?
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settings = getServiceSettings()
            
            if (settings.enabled && !settings.targetBssid.isNullOrBlank()) {
                // Wait a bit to ensure system is fully booted
                Thread.sleep(5000)
                
                // Start the service
                WifiMonitorService.startService(
                    context,
                    settings.targetBssid
                )
            }
        }
    }
}