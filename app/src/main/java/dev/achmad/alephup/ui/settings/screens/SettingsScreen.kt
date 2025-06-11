package dev.achmad.alephup.ui.settings.screens

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.achmad.alephup.R
import dev.achmad.alephup.base.service.AttendanceService
import dev.achmad.alephup.base.preferences.ApplicationPreferences
import dev.achmad.alephup.ui.settings.Preference
import dev.achmad.alephup.ui.settings.PreferenceScreen
import dev.achmad.alephup.util.PermissionState
import dev.achmad.alephup.util.rememberBackgroundLocationPermissionState
import dev.achmad.alephup.util.rememberIgnoreBatteryOptimizationPermissionState
import dev.achmad.alephup.util.rememberNotificationPermissionState
import dev.achmad.core.util.extension.injectLazy

@OptIn(ExperimentalMaterial3Api::class)
object SettingsScreen: Screen {
    private fun readResolve(): Any = SettingsScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val applicationPreferences by remember { injectLazy<ApplicationPreferences>() }
        val backgroundLocationPermission = rememberBackgroundLocationPermissionState()
        val notificationPermission = rememberNotificationPermissionState()

        PreferenceScreen(
            title = "Settings", // TODO copy
            onBackPressed = {
                navigator.pop()
            },
            itemsProvider = {
                listOf(
                    getPermissionGroup(
                        backgroundLocationPermission = backgroundLocationPermission,
                        notificationPermission = notificationPermission,
                    ),
                    getBackgroundServiceGroup(
                        applicationPreferences = applicationPreferences,
                        backgroundLocationPermission = backgroundLocationPermission,
                        notificationPermission = notificationPermission,
                    ),
                    getBatteryOptimizationGroup(),
                )
            },
        )
    }

    @Composable
    private fun getPermissionGroup(
        backgroundLocationPermission: PermissionState,
        notificationPermission: PermissionState,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.permission_group_title),
            preferenceItems = listOf(
                Preference.PreferenceItem.PermissionPreference(
                    permissionState = backgroundLocationPermission,
                    title = stringResource(R.string.allow_location_permission_all_the_time),
                    subtitle = stringResource(R.string.required_for_wifi_monitoring),
                ),
                Preference.PreferenceItem.PermissionPreference(
                    permissionState = notificationPermission,
                    title = stringResource(R.string.allow_notifications),
                    subtitle = stringResource(R.string.required_for_background_notification),
                ),
            ),
        )
    }

    @Composable
    private fun getBackgroundServiceGroup(
        applicationPreferences: ApplicationPreferences,
        backgroundLocationPermission: PermissionState,
        notificationPermission: PermissionState,
    ): Preference.PreferenceGroup {
        val applicationContext = LocalActivity.currentOrThrow.applicationContext
        return Preference.PreferenceGroup(
            title = stringResource(R.string.background_service),
            preferenceItems = listOf(
                Preference.PreferenceItem.BasicSwitchPreference(
                    value = AttendanceService.isRunning,
                    title = stringResource(R.string.run_in_background),
                    enabled = backgroundLocationPermission.isGranted.value && notificationPermission.isGranted.value,
                    onValueChanged = { newValue ->
                        if (newValue) AttendanceService.startService(applicationContext)
                        else AttendanceService.stopService(applicationContext)
                        true
                    }
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = applicationPreferences.runInBackgroundOnBoot(),
                    title = stringResource(R.string.start_service_on_boot),
                    subtitle = stringResource(R.string.start_service_on_boot_description),
                    enabled = backgroundLocationPermission.isGranted.value && notificationPermission.isGranted.value,
                ),
                Preference.PreferenceItem.InfoPreference(
                    title = stringResource(R.string.allow_required_permission_to_enable_background_service)
                )
            )
        )
    }

    @Composable
    private fun getBatteryOptimizationGroup(): Preference.PreferenceGroup {
        val ignoreBatteryOptimizationPermission = rememberIgnoreBatteryOptimizationPermissionState()
        return Preference.PreferenceGroup(
            title = stringResource(R.string.battery_optimization),
            preferenceItems = listOf(
                Preference.PreferenceItem.PermissionPreference(
                    permissionState = ignoreBatteryOptimizationPermission,
                    title = stringResource(R.string.excluded_from_battery_optimization),
                    subtitle = stringResource(R.string.excluded_from_battery_optimization_description),
                )
            )
        )
    }
}
