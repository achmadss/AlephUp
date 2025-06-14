package dev.achmad.alephup.base.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.achmad.alephup.base.MainApplication.Companion.backgroundPermissions
import dev.achmad.alephup.base.MainApplication.Companion.requiredPermissions
import dev.achmad.alephup.base.preferences.ApplicationPreferences
import dev.achmad.alephup.ui.checkin.CheckInService
import dev.achmad.alephup.util.arePermissionsAllowed
import dev.achmad.core.util.extension.injectLazy

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val preference by injectLazy<ApplicationPreferences>()
            val requiredPermissionsAllowed = context.arePermissionsAllowed(requiredPermissions)
            val backgroundPermissionsAllowed = context.arePermissionsAllowed(backgroundPermissions)
            if (preference.runInBackgroundOnBoot().get() && requiredPermissionsAllowed && backgroundPermissionsAllowed) {
                CheckInService.startService(context)
            }
        }
    }
}