package dev.achmad.alephup.base

import android.Manifest
import android.app.Application
import android.os.Build
import android.util.Log
import dev.achmad.alephup.base.preferences.ApplicationPreferences
import dev.achmad.alephup.base.service.AttendanceService
import dev.achmad.alephup.di.appModule
import dev.achmad.alephup.util.arePermissionsAllowed
import dev.achmad.core.device.notification.NotificationHelper
import dev.achmad.core.di.coreModule
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.data.di.dataModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE

class MainApplication: Application() {

    companion object {
        val requiredPermissions = mutableListOf<String>()
        val backgroundPermissions = mutableListOf<String>()
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            logger(
                object: Logger() {
                    override fun display(level: Level, msg: MESSAGE) {
                        when (level) {
                            Level.DEBUG -> Log.d(null, msg)
                            Level.INFO -> Log.i(null, msg)
                            Level.WARNING -> Log.w(null, msg)
                            Level.ERROR -> Log.e(null, msg)
                            Level.NONE -> Log.v(null, msg)
                        }
                    }
                }
            )
            androidContext(this@MainApplication)
            modules(
                listOf(
                    appModule,
                    coreModule,
                    dataModule,
                )
            )
        }

        requiredPermissions.addAll(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
            )
        )

        val applicationPreferences by injectLazy<ApplicationPreferences>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && applicationPreferences.runInBackgroundOnBoot().get()) {
            backgroundPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (!arePermissionsAllowed(backgroundPermissions)) {
            applicationPreferences.runInBackgroundOnBoot().set(false)
        }

        // notification channels
        val notificationHelper by injectLazy<NotificationHelper>()
        notificationHelper.createNotificationChannels(
            listOf(
                AttendanceService.createNotificationChannelConfig()
                // add more if needed
            )
        )
    }
}