package dev.achmad.alephup.ui.settings.screens

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.achmad.alephup.R
import dev.achmad.alephup.base.MainApplication.Companion.requiredPermissions
import dev.achmad.alephup.base.preferences.ApplicationPreferences
import dev.achmad.alephup.ui.checkin.CheckInService
import dev.achmad.alephup.ui.auth.SignInScreen
import dev.achmad.alephup.ui.settings.Preference
import dev.achmad.alephup.ui.settings.PreferenceScreen
import dev.achmad.alephup.util.MultiplePermissionsState
import dev.achmad.alephup.util.PermissionState
import dev.achmad.alephup.util.extension.rememberFirebaseUser
import dev.achmad.alephup.util.rememberIgnoreBatteryOptimizationPermissionState
import dev.achmad.alephup.util.rememberMultiplePermissionsState
import dev.achmad.alephup.util.rememberNotificationPermissionState
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.data.auth.GoogleAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
object SettingsScreen: Screen {
    private fun readResolve(): Any = SettingsScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var loading by remember { mutableStateOf(false) }
        val applicationPreferences by remember { injectLazy<ApplicationPreferences>() }
        val locationPermissions = rememberMultiplePermissionsState(requiredPermissions)
        val notificationPermission = rememberNotificationPermissionState()

        PreferenceScreen(
            title = "Settings", // TODO copy
            loading = loading,
            onBackPressed = {
                navigator.pop()
            },
            itemsProvider = {
                listOf(
                    getPermissionGroup(
                        locationPermissions = locationPermissions,
                        notificationPermission = notificationPermission,
                    ),
                    getBackgroundServiceGroup(
                        applicationPreferences = applicationPreferences,
                        locationPermissions = locationPermissions,
                        notificationPermission = notificationPermission,
                    ),
                    getBatteryOptimizationGroup(),
                    getAccountGroup(
                        onSignOut = { loading = true }
                    ),
                )
            },
        )
    }

    @Composable
    private fun getPermissionGroup(
        locationPermissions: MultiplePermissionsState,
        notificationPermission: PermissionState,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.permission_group_title),
            preferenceItems = listOf(
                Preference.PreferenceItem.MultiplePermissionPreference(
                    permissionState = locationPermissions,
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
        locationPermissions: MultiplePermissionsState,
        notificationPermission: PermissionState,
    ): Preference.PreferenceGroup {
        val applicationContext = LocalActivity.currentOrThrow.applicationContext
        return Preference.PreferenceGroup(
            title = stringResource(R.string.background_service),
            preferenceItems = listOf(
                Preference.PreferenceItem.BasicSwitchPreference(
                    value = CheckInService.isRunning,
                    title = stringResource(R.string.run_in_background),
                    subtitle = stringResource(R.string.run_in_background_description),
                    enabled = locationPermissions.isAllPermissionsGranted() && notificationPermission.isGranted.value,
                    onValueChanged = { newValue ->
                        if (newValue) CheckInService.startService(applicationContext)
                        else CheckInService.stopService(applicationContext)
                        true
                    }
                ),
                // hide run on boot because this needs location permission all the time
//                Preference.PreferenceItem.SwitchPreference(
//                    preference = applicationPreferences.runInBackgroundOnBoot(),
//                    title = stringResource(R.string.start_service_on_boot),
//                    subtitle = stringResource(R.string.start_service_on_boot_description),
//                    enabled = locationPermissions.isAllPermissionsGranted() && notificationPermission.isGranted.value,
//                ),
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

    @Composable
    private fun getAccountGroup(
        onSignOut: () -> Unit,
    ): Preference.PreferenceGroup {
        val context = LocalActivity.currentOrThrow.applicationContext
        val navigator = LocalNavigator.currentOrThrow
        val user = rememberFirebaseUser()
        val scope = rememberCoroutineScope()
        val googleAuth by remember { injectLazy<GoogleAuth>() }
        return Preference.PreferenceGroup(
            title = stringResource(R.string.account),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.sign_out),
                    titleColor = Color.Red,
                    subtitle = stringResource(
                        id = R.string.signed_in_as,
                        user?.displayName ?: "Unknown Username", // TODO copy
                        user?.email ?: "Unknown Email", // TODO copy
                    ),
                    onClick = {
                        scope.launch {
                            onSignOut()
                            CheckInService.stopService(context)
                            googleAuth.signOut()
                            navigator.popUntilRoot()
                            navigator.replace(SignInScreen)
                        }
                    }
                )
            )
        )
    }

}
