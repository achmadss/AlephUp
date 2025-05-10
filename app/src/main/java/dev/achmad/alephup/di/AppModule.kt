package dev.achmad.alephup.di

import dev.achmad.alephup.device.BatteryOptimizationHelper
import dev.achmad.alephup.device.BootCompletedPreference
import dev.achmad.alephup.device.WifiMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { WifiMonitor(androidContext()) }
    single { BatteryOptimizationHelper(androidContext()) }
    single { BootCompletedPreference(get()) }
}