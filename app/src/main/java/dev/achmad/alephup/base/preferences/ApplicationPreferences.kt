package dev.achmad.alephup.base.preferences

import dev.achmad.core.preference.BasePreference
import dev.achmad.core.preference.PreferenceStore

class ApplicationPreferences(
    private val preferenceStore: PreferenceStore
): BasePreference() {
    fun runInBackgroundOnBoot() = preferenceStore.getBoolean(getPrefKey("run_in_background_on_boot"))
}