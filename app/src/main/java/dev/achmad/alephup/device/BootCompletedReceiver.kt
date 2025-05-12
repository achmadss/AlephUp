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
        const val KEY_SERVICE_ENABLED_ON_BOOT = "service_enabled_on_boot"

        private val preference = inject<BootCompletedPreference>()
        
        /**
         * Save service settings to start on boot
         */
        fun saveServiceSettings(
            enabled: Boolean
        ) {
            with(preference) {
                serviceEnabledOnBoot().set(enabled)
            }
        }
        
        /**
         * Clear service settings to prevent auto-start
         */
        fun clearServiceSettings() {
            with(preference) {
                serviceEnabledOnBoot().set(false)
            }
        }

    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            
            if (preference.serviceEnabledOnBoot().get()) {
                // Wait a bit to ensure system is fully booted
                Thread.sleep(5000)
                WifiMonitorService.startService(context)
            }
        }
    }
}