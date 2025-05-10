package dev.achmad.alephup.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.achmad.alephup.device.BatteryOptimizationHelper
import dev.achmad.alephup.device.BootCompletedReceiver
import dev.achmad.alephup.device.WifiMonitorService
import dev.achmad.alephup.ui.util.PermissionState
import dev.achmad.alephup.ui.util.rememberPermissionState
import dev.achmad.core.TARGET_BSSID
import dev.achmad.core.util.inject

object HomeScreen: Screen {

    private fun readResolve(): Any = HomeScreen

    private const val REQUEST_BATTERY_OPTIMIZATION = 1001

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val activity = LocalActivity.currentOrThrow
        val lifecycleOwner = LocalLifecycleOwner.current
        val batteryOptimizationHelper = remember { inject<BatteryOptimizationHelper>() }
        val viewModel = viewModel<HomeViewModel>()
        val state by viewModel.state.collectAsState()
        val serviceEnabled = viewModel.serviceEnabled.collectAsState().value

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

        fun handleService() {
            if (!notificationPermission.isGranted.value) {
                notificationPermission.requestPermission()
                return
            }
            if (!WifiMonitorService.isRunning.value) {
                WifiMonitorService.startService(
                    context = context,
                    targetBssid = state.wifiConnectionInfo?.bssid
                )
            } else {
                WifiMonitorService.stopService(context)
            }
        }

        fun handleBoot() {
            if (!notificationPermission.isGranted.value) {
                notificationPermission.requestPermission()
                return
            }
            if (!serviceEnabled) {
                BootCompletedReceiver.saveServiceSettings(
                    enabled = true,
                    targetBssid = TARGET_BSSID
                )
            } else {
                BootCompletedReceiver.clearServiceSettings()
            }
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.updateScreenState {
                        it.copy(
                            isIgnoringBatteryOptimization = batteryOptimizationHelper.isIgnoringBatteryOptimizations(),
                        )
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Scaffold(

        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.wifiConnectionInfo?.toString() ?: "Not connected"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Excluded from battery optimization: ${state.isIgnoringBatteryOptimization}"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Service running: ${WifiMonitorService.isRunning.value}"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Service on boot enabled: $serviceEnabled"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (!batteryOptimizationHelper.isIgnoringBatteryOptimizations()) {
                            val intent = batteryOptimizationHelper.requestBatteryOptimizationExclusion()
                            activity.startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION)
                        }
                    }
                ) {
                    Text("Optimize Battery")
                }
                Button(
                    onClick = {
                        val intent = batteryOptimizationHelper.openBatteryOptimizationSettings()
                        activity.startActivity(intent)
                    }
                ) {
                    Text("Open settings")
                }
                Button(
                    onClick = { handleService() }
                ) {
                    Text(if (!WifiMonitorService.isRunning.value) "Start service" else "Stop service")
                }
                Button(
                    onClick = { handleBoot() }
                ) {
                    Text(if (!serviceEnabled) "Start service on boot" else "Stop service on boot")
                }
            }
        }
    }

}