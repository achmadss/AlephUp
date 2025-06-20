package dev.achmad.alephup.ui.checkin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AssignmentLate
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.achmad.alephup.R
import dev.achmad.alephup.ui.components.AppBar
import dev.achmad.alephup.ui.components.AppBarActions
import dev.achmad.alephup.ui.settings.screens.SettingsScreen
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.data.checkin.CheckInPreference
import dev.achmad.data.checkin.CheckInResult

data class CheckInScreen(
    val shouldCheckIn: Boolean = false,
): Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { CheckInScreenModel(shouldCheckIn) }
        val state by screenModel.state.collectAsState()
        val checkInPreference by remember { injectLazy<CheckInPreference>() }

        Scaffold(
            topBar = {
                AppBar(
                    titleContent = {
                        Image(
                            modifier = Modifier
                                .height(24.dp),
                            contentScale = ContentScale.FillHeight,
                            painter = painterResource(R.drawable.aleph_logo_white),
                            contentDescription = "logo"
                        )
                    },
                    backgroundColor = Color.Transparent,
                    actions = {
                        AppBarActions(
                            actions = listOf(
                                AppBar.Action(
                                    title = "History", // TODO copy
                                    icon = Icons.Default.History,
                                    onClick = {
                                        // TODO navigate to history screen
                                    }
                                ),
                                AppBar.Action(
                                    title = "Settings", // TODO copy
                                    icon = Icons.Default.Settings,
                                    onClick = {
                                        navigator.push(SettingsScreen)
                                    },
                                ),
                            )
                        )
                    }
                )
            }
        ) { contentPadding ->
            if (state.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .wrapContentSize(Alignment.Center),
                ) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(
                        id = R.string.greet,
                        formatArgs = arrayOf(
                            screenModel.getCurrentUser()?.name ?: "Unknown User"
                        )
                    ),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                val wifiName = state.wifiConnectionInfo?.ssid
                Text(
                    text = when {
                        wifiName != null -> stringResource(R.string.connected_to, wifiName)
                        else -> stringResource(R.string.not_connected)
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
                Icon(
                    modifier = Modifier
                        .size(72.dp),
                    imageVector = when {
                        checkInPreference.checkedInToday() -> Icons.Outlined.AssignmentTurnedIn
                        else -> Icons.Outlined.AssignmentLate
                    },
                    contentDescription = null,
                    tint = ButtonDefaults.textButtonColors().contentColor
                )
                Spacer(modifier = Modifier.height(32.dp))
                when {
                    checkInPreference.checkedInToday() -> {
                        Text(
                            text = stringResource(R.string.check_in_success),
                        )
                    }
                    else -> {
                        Text(
                            text = when(state.result) {
                                CheckInResult.HttpError -> "Failed to connect to server" // TODO copy
                                CheckInResult.InvalidSSID -> "Not connected to an Aleph Wifi" // TODO copy
                                null -> "Please connect to Aleph Wifi" // TODO copy
                                else -> ""
                            },
                        )
                    }
                }
                when(state.result) {
                    CheckInResult.HttpError,
                    CheckInResult.InvalidSSID -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { screenModel.retryCheckIn() }
                        ) {
                            Text("Try again") // TODO copy
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

}