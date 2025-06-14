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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import dev.achmad.alephup.R

object SignInScreen: Screen {
    private fun readResolve(): Any = SignInScreen

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val viewModel = viewModel<SignInScreenViewModel>()
        val loading by viewModel.loading.collectAsState()

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
                text = "Sign-in to AlephUp", // TODO copy
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
                        viewModel.signIn(context)
                    }
                }
            ) {
                if (!loading) {
                    Text(
                        text = "Sign-in with Google", // TODO copy
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