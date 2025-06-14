package dev.achmad.data.checkin

import dev.achmad.core.preference.BasePreference
import dev.achmad.core.preference.PreferenceStore
import dev.achmad.core.util.extension.inject
import java.time.LocalDate

class CheckInPreference(
    private val preferenceStore: PreferenceStore = inject()
): BasePreference() {
    fun lastCheckedIn() = preferenceStore.getObject<LocalDate>(
        key = getPrefKey("last_attendance"),
        defaultValue = LocalDate.MIN,
        serializer = { it.toString() },
        deserializer = { LocalDate.parse(it) }
    )

    // could be enhanced to check api instead
    fun checkedInToday() = lastCheckedIn().get().isEqual(LocalDate.now())
}