package dev.achmad.alephup.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.achmad.core.REQUEST_BATTERY_OPTIMIZATION_REQUEST_CODE
import dev.achmad.core.device.battery_optimization.BatteryOptimizationHelper
import dev.achmad.core.util.extension.inject

class PermissionState internal constructor(
    val isGranted: MutableState<Boolean>,
    val requestPermission: () -> Unit
)

class MultiplePermissionsState internal constructor(
    val permissions: Map<String, Boolean>,
    val requiredPermissionsGranted: State<Boolean>,
    val requestPermissions: () -> Unit
)

@Composable
fun rememberPermissionState(
    permission: String,
): PermissionState {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val permissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted.value = isGranted
    }

    return remember(permission) {
        PermissionState(
            isGranted = permissionGranted,
            requestPermission = {
                if (activity != null && !permissionGranted.value) {
                    launcher.launch(permission)
                }
            }
        )
    }
}

@Composable
fun rememberMultiplePermissionsState(
    permissions: List<String>,
    requiredPermissions: List<String> = permissions
): MultiplePermissionsState {
    val context = LocalContext.current
    val permissionResults = remember {
        mutableStateMapOf<String, Boolean>().apply {
            permissions.forEach { permission ->
                val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                put(permission, granted)
            }
        }
    }

    val requiredPermissionsGranted = remember {
        derivedStateOf {
            permissionResults
                .filter { it.key in requiredPermissions }.values
                .all { it }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        resultMap.forEach { (permission, granted) ->
            permissionResults[permission] = granted
        }
    }

    return remember(permissions) {
        MultiplePermissionsState(
            permissions = permissionResults,
            requiredPermissionsGranted = requiredPermissionsGranted,
            requestPermissions = {
                launcher.launch(permissions.toTypedArray())
            }
        )
    }
}

fun Context.arePermissionsAllowed(
    permissions: List<String>
): Boolean {
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}


@Composable
fun rememberBackgroundLocationPermissionState(): PermissionState {
    val activity = LocalActivity.currentOrThrow
    val applicationContext = activity.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current

    val backgroundLocationPermission = remember {
        PermissionState(
            isGranted = mutableStateOf(false),
            requestPermission = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", applicationContext.packageName, null)
                }
                activity.startActivity(intent)
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    backgroundLocationPermission.isGranted.value =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContextCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return backgroundLocationPermission
}

@Composable
fun rememberNotificationPermissionState(): PermissionState {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        }
        else -> {
            PermissionState(
                isGranted = remember { mutableStateOf(true) },
                requestPermission = {}
            )
        }
    }
}

@Composable
fun rememberIgnoreBatteryOptimizationPermissionState(): PermissionState {
    val activity = LocalActivity.currentOrThrow
    val lifecycleOwner = LocalLifecycleOwner.current
    val batteryOptimizationHelper = remember { inject<BatteryOptimizationHelper>() }
    val ignoreBatteryOptimizationPermission = remember {
        PermissionState(
            isGranted = mutableStateOf(false),
            requestPermission = {
                activity.startActivityForResult(
                    batteryOptimizationHelper.requestBatteryOptimizationExclusion(),
                    REQUEST_BATTERY_OPTIMIZATION_REQUEST_CODE
                )
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    ignoreBatteryOptimizationPermission.isGranted.value =
                        batteryOptimizationHelper.isIgnoringBatteryOptimizations()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return ignoreBatteryOptimizationPermission
}