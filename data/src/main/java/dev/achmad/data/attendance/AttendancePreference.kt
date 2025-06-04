package dev.achmad.data.attendance

import dev.achmad.core.preference.PreferenceStore
import dev.achmad.core.util.extension.inject
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

    // could be enhanced to check api instead
    fun attended() = preferenceStore.getBoolean("attended", false)
}