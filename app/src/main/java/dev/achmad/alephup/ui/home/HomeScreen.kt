package dev.achmad.alephup.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.achmad.alephup.R
import dev.achmad.alephup.device.BootCompletedReceiver
import dev.achmad.alephup.device.WifiMonitorService
import dev.achmad.alephup.ui.components.CardSection
import dev.achmad.alephup.ui.components.CardSectionItem
import dev.achmad.alephup.ui.components.CardSectionButton
import dev.achmad.alephup.ui.components.ToggleItem
import dev.achmad.alephup.ui.util.PermissionState
import dev.achmad.alephup.ui.util.rememberPermissionState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object HomeScreen: Screen {

    private fun readResolve(): Any = HomeScreen

    private const val REQUEST_BATTERY_OPTIMIZATION = 1001

    @Composable
    override fun Content() {
        val activity = LocalActivity.currentOrThrow
        val applicationContext = activity.applicationContext
        val lifecycleOwner = LocalLifecycleOwner.current
        val scrollState = rememberScrollState()
        val viewModel = viewModel<HomeViewModel>()
        val state by viewModel.state.collectAsState()
        val serviceEnabledOnBoot = viewModel.serviceEnabledOnBoot.collectAsState().value
        val lastAttendance = viewModel.lastAttendance.collectAsState().value
        val lastAttendanceString = when {
            lastAttendance == LocalDate.MIN -> "Never"
            else -> lastAttendance.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        }
        var backgroundPermissionGranted by remember { mutableStateOf(false) }
        val notificationPermission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                PermissionState(
                    permission = "android.permission.POST_NOTIFICATIONS",
                    isGranted = remember { mutableStateOf(true) },
                    requestPermission = {}
                )
            }
        }
        val toggleServiceEnabled by remember {
            derivedStateOf {
                backgroundPermissionGranted && notificationPermission.isGranted.value
            }
        }

        fun toggleBackgroundService() {
            if (!notificationPermission.isGranted.value) {
                notificationPermission.requestPermission()
                return
            }
            if (!WifiMonitorService.isRunning.value) {
                WifiMonitorService.startService(applicationContext)
            } else {
                WifiMonitorService.stopService(applicationContext)
            }
        }

        fun toggleBoot() {
            if (!notificationPermission.isGranted.value) {
                notificationPermission.requestPermission()
                return
            }
            if (!serviceEnabledOnBoot) {
                BootCompletedReceiver.saveServiceSettings(true)
            } else {
                BootCompletedReceiver.clearServiceSettings()
            }
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when(event) {
                    Lifecycle.Event.ON_CREATE -> {
                        ResetAndMaybePostAttendanceJob.scheduleNow(applicationContext)
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        backgroundPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContextCompat.checkSelfPermission(
                                applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true
                        viewModel.updateBatteryOptimization()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Scaffold { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                val wifiName = state.wifiConnectionInfo?.ssid
                Text(
                    text = when {
                        wifiName != null -> stringResource(R.string.connected_to, wifiName)
                        else -> stringResource(R.string.not_connected)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Last attendance: $lastAttendanceString"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.enable_optional_settings)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CardSection(title = stringResource(R.string.background_service)) {
                    CardSectionItem(
                        text = stringResource(R.string.allow_location_permission_all_the_time),
                        description = stringResource(R.string.required_for_wifi_monitoring),
                        isGranted = backgroundPermissionGranted,
                        onRequestPermission = {
                            if (!backgroundPermissionGranted) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", applicationContext.packageName, null)
                                }
                                activity.startActivity(intent)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CardSectionItem(
                        text = stringResource(R.string.allow_notifications),
                        description = stringResource(R.string.required_for_background_notification),
                        isGranted = notificationPermission.isGranted.value,
                        onRequestPermission = {
                            notificationPermission.requestPermission.invoke()
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ToggleItem(
                        text = stringResource(R.string.start_service_on_boot),
                        description = stringResource(R.string.start_service_on_boot_description),
                        isChecked = serviceEnabledOnBoot,
                        onCheckedChange = { toggleBoot() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CardSectionButton(
                        text = if (!WifiMonitorService.isRunning.value)
                            stringResource(R.string.enable_background_service)
                        else
                            stringResource(R.string.disable_background_service),
                        onClick = { toggleBackgroundService() },
                        enabled = toggleServiceEnabled,
                        backgroundColor = if (WifiMonitorService.isRunning.value) Color.Red else null,
                        contentColor = if (WifiMonitorService.isRunning.value) Color.White else null
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                CardSection(title = stringResource(R.string.battery_optimization)) {
                    CardSectionItem(
                        text = stringResource(R.string.excluded_from_battery_optimization),
                        description = stringResource(R.string.excluded_from_battery_optimization_description),
                        isGranted = state.isIgnoringBatteryOptimization,
                        onRequestPermission = {
                            if (!state.isIgnoringBatteryOptimization) {
                                activity.startActivityForResult(
                                    viewModel.requestBatteryOptimizationExclusionIntent(),
                                    REQUEST_BATTERY_OPTIMIZATION
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CardSectionButton(
                        text = stringResource(R.string.open_battery_optimization_settings),
                        onClick = {
                            activity.startActivity(viewModel.openBatteryOptimizationSettingsIntent())
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

}