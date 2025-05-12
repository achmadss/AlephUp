package dev.achmad.alephup.ui.onboarding

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.achmad.alephup.R
import dev.achmad.alephup.ui.components.CardSection
import dev.achmad.alephup.ui.components.CardSectionItem
import dev.achmad.alephup.ui.home.HomeScreen
import dev.achmad.alephup.ui.util.rememberMultiplePermissionsState

object OnBoardingScreen: Screen {
    private fun readResolve(): Any = OnBoardingScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val requiredPermissions = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .systemBarsPadding()
                    .padding(
                        top = 64.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    text = stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(36.dp))
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    text = stringResource(R.string.onboarding_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    text = stringResource(R.string.onboarding_note),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(36.dp))
                CardSection(title = stringResource(R.string.onboarding_label_required)) {
                    CardSectionItem(
                        text = stringResource(R.string.onboarding_permission_location),
                        description = stringResource(R.string.onboarding_permission_location_description),
                        isGranted = requiredPermissions.allGranted.value,
                        onRequestPermission = {
                            requiredPermissions.requestPermissions.invoke()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.align(Alignment.End),
                    enabled = requiredPermissions.allGranted.value,
                    onClick = {
                        navigator.replace(HomeScreen)
                    },
                    contentPadding = PaddingValues(
                        start = 64.dp,
                        end = 48.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_button_next),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }

    }
}