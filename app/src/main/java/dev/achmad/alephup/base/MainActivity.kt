package dev.achmad.alephup.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.ScreenTransition
import dev.achmad.alephup.base.MainApplication.Companion.requiredPermissions
import dev.achmad.alephup.ui.auth.SignInScreen
import dev.achmad.alephup.ui.checkin.CheckInScreen
import dev.achmad.alephup.ui.checkin.CheckInService
import dev.achmad.alephup.ui.onboarding.OnBoardingScreen
import dev.achmad.alephup.ui.theme.AlephUpTheme
import dev.achmad.alephup.util.arePermissionsAllowed
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.data.auth.Auth
import dev.achmad.data.auth.AuthState
import dev.achmad.data.checkin.CheckInPreference
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(
            if (arePermissionsAllowed(requiredPermissions)) savedInstanceState else null
        )
        enableEdgeToEdge()
        setContent {
            AlephUpTheme {
                val slideDistance = rememberSlideDistance()
                val auth by remember { injectLazy<Auth>() }
                val checkInPreference by remember { injectLazy<CheckInPreference>() }
                val authState = auth.authState.collectAsState().value

                Navigator(
                    screen = when {
                        !arePermissionsAllowed(requiredPermissions) -> OnBoardingScreen
                        authState is AuthState.SignedOut -> SignInScreen
                        else -> CheckInScreen()
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
                        content = {
                            LaunchedEffect(authState) {
                                when(it) {
                                    is OnBoardingScreen -> Unit
                                    is SignInScreen -> {
                                        if (authState is AuthState.SignedIn) {
                                            navigator.popUntilRoot()
                                            navigator.replace(CheckInScreen(shouldCheckIn = true))
                                        }
                                    }
                                    else -> {
                                        if (authState is AuthState.SignedOut) {
                                            navigator.popUntilRoot()
                                            navigator.replace(SignInScreen)
                                            CheckInService.stopService(applicationContext)
                                            checkInPreference.lastCheckedIn().delete()
                                        }
                                    }
                                }
                            }
                            it.Content()
                        }
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (arePermissionsAllowed(requiredPermissions)) {
            super.onSaveInstanceState(outState)
        }
    }

}
