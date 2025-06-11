package dev.achmad.alephup.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import dev.achmad.alephup.R

object LoginScreen: Screen {
    private fun readResolve(): Any = LoginScreen

    @Composable
    override fun Content() {
        val viewModel = viewModel<LoginScreenViewModel>()
        var textFieldEmailValue by remember { mutableStateOf("") }
        var textFieldPasswordValue by remember { mutableStateOf("") }
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
                painter = painterResource(R.drawable.logo),
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
            OutlinedTextField(
                value = textFieldEmailValue,
                onValueChange = {
                    textFieldEmailValue = it
                },
                label = {
                    Text("Email") // TODO copy
                },
                placeholder = {
                    Text("Enter your aleph email") // TODO copy
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                    )
                },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = textFieldPasswordValue,
                onValueChange = {
                    textFieldPasswordValue = it
                },
                label = {
                    Text("Password") // TODO copy
                },
                placeholder = {
                    Text("Enter your password") // TODO copy
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Password,
                        contentDescription = null,
                    )
                },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                modifier = Modifier
                    .width(TextFieldDefaults.MinWidth),
                onClick = {
                    // TODO
                }
            ) {
                Text(
                    text = "Sign-in",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = {
                    // TODO
                }
            ) {
                Text(
                    text = "I need help with my account",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
        }
    }
}