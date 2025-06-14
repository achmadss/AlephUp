package dev.achmad.alephup.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.ScreenTransition
import dev.achmad.alephup.base.MainApplication.Companion.requiredPermissions
import dev.achmad.alephup.ui.auth.SignInScreen
import dev.achmad.alephup.ui.checkin.CheckInScreen
import dev.achmad.alephup.ui.onboarding.OnBoardingScreen
import dev.achmad.alephup.ui.theme.AlephUpTheme
import dev.achmad.alephup.util.arePermissionsAllowed
import dev.achmad.alephup.util.extension.rememberFirebaseUser
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
                val user = rememberFirebaseUser()
                Navigator(
                    screen = when {
                        !arePermissionsAllowed(requiredPermissions) -> OnBoardingScreen
                        user == null -> SignInScreen
                        else -> CheckInScreen
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
        if (arePermissionsAllowed(requiredPermissions)) {
            super.onSaveInstanceState(outState)
        }
    }

}
