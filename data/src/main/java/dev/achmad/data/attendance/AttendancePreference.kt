package dev.achmad.data.attendance

import dev.achmad.core.preference.PreferenceStore
import dev.achmad.core.util.inject
import java.time.LocalDate

class AttendancePreference(
    private val preferenceStore: PreferenceStore = inject()
) {
    fun lastAttendance() = preferenceStore.getObject<LocalDate>(
        key = "last_attendance",
        defaultValue = LocalDate.MIN,
        serializer = { it.toString() },
        deserializer = { LocalDate.parse(it) }
    )
    fun attended() = preferenceStore.getBoolean("attended", false)
}