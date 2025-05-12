package dev.achmad.alephup.ui.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class PermissionState internal constructor(
    val permission: String,
    val isGranted: State<Boolean>,
    val requestPermission: () -> Unit
)

class MultiplePermissionsState internal constructor(
    val permissions: Map<String, Boolean>,
    val allGranted: State<Boolean>,
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
            permission = permission,
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
    permissions: List<String>
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

    val allGranted = remember {
        derivedStateOf {
            permissionResults.values.all { it }
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
            allGranted = allGranted,
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
