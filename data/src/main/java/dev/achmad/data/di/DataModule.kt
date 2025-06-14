package dev.achmad.data.di

import dev.achmad.data.auth.Auth
import dev.achmad.data.auth.google.GoogleAuth
import dev.achmad.data.checkin.CheckIn
import dev.achmad.data.checkin.CheckInPreference
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { CheckInPreference() }
    single { CheckIn() }
    single<Auth> { GoogleAuth(androidContext()) }
}