package dev.achmad.alephup.di

import dev.achmad.alephup.base.preferences.ApplicationPreferences
import dev.achmad.alephup.ui.checkin.CheckInNotifier
import dev.achmad.core.device.battery_optimization.BatteryOptimizationHelper
import dev.achmad.core.device.notification.NotificationHelper
import dev.achmad.core.device.wifi.WifiHelper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { WifiHelper(androidContext()) }
    single { BatteryOptimizationHelper(androidContext()) }
    single { ApplicationPreferences(get()) }
    single { NotificationHelper(androidContext()) }
    single(createdAtStart = true) { CheckInNotifier(androidContext()) }
}