package dev.achmad.alephup.base.preferences

import dev.achmad.alephup.base.receiver.BootReceiver
import dev.achmad.core.preference.PreferenceStore

class ApplicationPreferences(
    private val preferenceStore: PreferenceStore
) {
    fun runInBackgroundOnBoot() = preferenceStore.getBoolean(BootReceiver.KEY_RUN_IN_BACKGROUND_ON_BOOT)
}