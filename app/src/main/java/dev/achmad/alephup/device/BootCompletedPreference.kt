package dev.achmad.alephup.device

import dev.achmad.core.preference.PreferenceStore

class BootCompletedPreference(
    private val preferenceStore: PreferenceStore
) {
    fun serviceEnabledOnBoot() = preferenceStore.getBoolean(BootCompletedReceiver.KEY_SERVICE_ENABLED_ON_BOOT)
}