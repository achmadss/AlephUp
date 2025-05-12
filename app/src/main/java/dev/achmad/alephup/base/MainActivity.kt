package dev.achmad.alephup.base

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.ScreenTransition
import dev.achmad.alephup.device.BootCompletedPreference
import dev.achmad.alephup.ui.home.HomeScreen
import dev.achmad.alephup.ui.onboarding.OnBoardingScreen
import dev.achmad.alephup.ui.theme.AlephUpTheme
import dev.achmad.alephup.ui.util.arePermissionsAllowed
import dev.achmad.core.util.inject
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

class MainActivity : ComponentActivity() {

    private val bootCompletedPreference: BootCompletedPreference = inject()

    private val hasRequiredPermissions: Boolean
        get() {
            return arePermissionsAllowed(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && bootCompletedPreference.serviceEnabledOnBoot().get()) {
                    listOf(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                    )
                } else {
                    listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                    )
                }

            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(
            if (hasRequiredPermissions) savedInstanceState else null
        )
        enableEdgeToEdge()
        setContent {
            AlephUpTheme {
                val slideDistance = rememberSlideDistance()
                Navigator(
                    screen = when {
                        hasRequiredPermissions -> HomeScreen
                        else -> OnBoardingScreen
                    },
                    disposeBehavior = NavigatorDisposeBehavior(
                        disposeNestedNavigators = false,
                        disposeSteps = true
                    )
                ) { navigator ->
                    ScreenTransition(
                        navigator = navigator,
                        transition = {
                            materialSharedAxisX(
                                forward = navigator.lastEvent != StackEvent.Pop,
                                slideDistance = slideDistance,
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (hasRequiredPermissions) {
            super.onSaveInstanceState(outState)
        }
    }

}
