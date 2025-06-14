package dev.achmad.data.di

import dev.achmad.data.checkin.CheckInPreference
import dev.achmad.data.checkin.CheckIn
import dev.achmad.data.auth.GoogleAuth
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { CheckInPreference() }
    single { CheckIn() }
    single { GoogleAuth(androidContext()) }
}