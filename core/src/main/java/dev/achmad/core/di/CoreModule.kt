package dev.achmad.core.di

import dev.achmad.core.network.NetworkHelper
import dev.achmad.core.preference.AndroidPreferenceStore
import dev.achmad.core.preference.PreferenceStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<PreferenceStore> { AndroidPreferenceStore(androidContext()) }
    single<NetworkHelper> { NetworkHelper(androidContext(), true) }
}