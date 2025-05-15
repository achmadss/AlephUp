package dev.achmad.data.di

import dev.achmad.data.attendance.AttendancePreference
import dev.achmad.data.attendance.PostAttendance
import org.koin.dsl.module

val dataModule = module {
    single { AttendancePreference(get()) }
    single { PostAttendance(get()) }
}