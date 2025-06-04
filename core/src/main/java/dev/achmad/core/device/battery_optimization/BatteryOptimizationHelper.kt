package dev.achmad.core.device.battery_optimization

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Helper class to handle battery optimization exclusion requests
 */
class BatteryOptimizationHelper(private val context: Context) {

    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    /**
     * Check if the app is already excluded from battery optimization
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Request to be excluded from battery optimization
     * This will show a system dialog to the user
     */
    fun requestBatteryOptimizationExclusion(): Intent {
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = "package:${context.packageName}".toUri()
        }
        return intent
    }

    /**
     * Open battery optimization settings for the app
     * User can manually disable optimization
     */
    fun openBatteryOptimizationSettings(): Intent {
        return Intent().apply {
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }
    }

    /**
     * For some manufacturers, additional settings need to be configured
     * Returns an intent to open device-specific settings if available
     */
    fun getManufacturerSpecificSettings(): Intent? {
        return when (Build.MANUFACTURER.lowercase()) {
            "xiaomi" -> Intent().apply {
                component = android.content.ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
                putExtra("package_name", context.packageName)
                putExtra("package_label", getAppLabel())
            }
            "huawei" -> Intent().apply {
                component = android.content.ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            }
            "samsung" -> Intent().apply {
                action = Settings.ACTION_DEVICE_INFO_SETTINGS
            }
            "oppo" -> Intent().apply {
                component = android.content.ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
            "vivo" -> Intent().apply {
                component = android.content.ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
            "oneplus" -> Intent().apply {
                component = android.content.ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            }
            else -> null
        }
    }

    private fun getAppLabel(): String {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
        return packageManager.getApplicationLabel(applicationInfo).toString()
    }
}