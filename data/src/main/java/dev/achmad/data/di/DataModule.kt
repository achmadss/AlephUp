package dev.achmad.data.di

import dev.achmad.data.attendance.AttendancePreference
import dev.achmad.data.attendance.PostAttendance
import dev.achmad.data.auth.GoogleAuth
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { AttendancePreference() }
    single { PostAttendance() }
    single { GoogleAuth(androidContext()) }
}