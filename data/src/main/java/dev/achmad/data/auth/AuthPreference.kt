package dev.achmad.data.auth

import dev.achmad.core.preference.BasePreference
import dev.achmad.core.preference.PreferenceStore
import dev.achmad.core.util.extension.inject

class AuthPreference(
    private val preferenceStore: PreferenceStore = inject()
): BasePreference() {
    fun token() = preferenceStore.getString(getPrefKey("token"))
}