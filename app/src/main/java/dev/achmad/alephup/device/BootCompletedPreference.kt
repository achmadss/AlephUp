package dev.achmad.alephup.device

import dev.achmad.core.preference.PreferenceStore

class BootCompletedPreference(
    private val preferenceStore: PreferenceStore
) {
    fun serviceEnabled() = preferenceStore.getBoolean(BootCompletedReceiver.KEY_SERVICE_ENABLED)
    fun targetBSSID() = preferenceStore.getString(BootCompletedReceiver.KEY_TARGET_BSSID)
}