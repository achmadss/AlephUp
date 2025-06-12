package dev.achmad.alephup.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.achmad.alephup.R
import dev.achmad.alephup.ui.home.HomeScreen
import dev.achmad.alephup.util.extension.rememberFirebaseUser
import dev.achmad.alephup.util.toast
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.data.auth.GoogleAuth
import kotlinx.coroutines.launch

object LoginScreen: Screen {
    private fun readResolve(): Any = LoginScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val googleAuth by remember { injectLazy<GoogleAuth>() }
        var loading by remember { mutableStateOf(false) }
        var validEmail by remember { mutableStateOf(true) }
        val user = rememberFirebaseUser()

        LaunchedEffect(user) {
            if (user != null && validEmail) {
                navigator.popUntilRoot()
                navigator.replace(HomeScreen)
                return@LaunchedEffect
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                modifier = Modifier
                    .height(36.dp),
                contentScale = ContentScale.FillHeight,
                painter = painterResource(R.drawable.aleph_logo_white),
                contentDescription = "logo"
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                modifier = Modifier,
                text = "Sign-in to AlephUp",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier
                    .width(TextFieldDefaults.MinWidth),
                enabled = !loading,
                onClick = {
                    if (!loading) {
                        scope.launch {
                            loading = true
                            googleAuth.signInWithGoogle(
                                filterByAuthorized = true,
                                onValidEmail = {
                                    validEmail = true
                                },
                                onInvalidEmail = {
                                    validEmail = false
                                    googleAuth.signOut()
                                    context.toast("Only Aleph Email is allowed")
                                }
                            ) ?: googleAuth.signInWithGoogle(
                                filterByAuthorized = false,
                                onValidEmail = {
                                    validEmail = true
                                },
                                onInvalidEmail = {
                                    validEmail = false
                                    googleAuth.signOut()
                                    context.toast("Only Aleph Email is allowed")
                                }
                            )
                            loading = false
                        }
                    }
                }
            ) {
                if (!loading) {
                    Text(
                        text = "Sign-in with Google",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = LocalContentColor.current
                    )
                }
            }
        }
    }
}