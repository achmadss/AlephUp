package dev.achmad.alephup.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.achmad.alephup.R
import dev.achmad.data.attendance.work.ResetAndMaybePostAttendanceJob
import dev.achmad.alephup.ui.components.AppBar
import dev.achmad.alephup.ui.components.AppBarActions
import dev.achmad.alephup.ui.settings.screens.SettingsScreen
import dev.achmad.alephup.util.extension.collectAsState
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.data.attendance.AttendancePreference
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object HomeScreen: Screen {

    private fun readResolve(): Any = HomeScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val activity = LocalActivity.currentOrThrow
        val applicationContext = activity.applicationContext
        val lifecycleOwner = LocalLifecycleOwner.current
        val viewModel = viewModel<HomeScreenViewModel>()
        val state by viewModel.state.collectAsState()
        val attendancePreference by remember { injectLazy<AttendancePreference>() }
        val attended by attendancePreference.attended().collectAsState()
        val lastAttendance by attendancePreference.lastAttendance().collectAsState()
        val lastAttendanceString = when {
            lastAttendance == LocalDate.MIN -> "Never"
            else -> lastAttendance.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when(event) {
                    Lifecycle.Event.ON_CREATE -> {
                        ResetAndMaybePostAttendanceJob.scheduleNow(applicationContext)
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(R.string.app_name),
                    backgroundColor = Color.Transparent,
                    actions = {
                        AppBarActions(
                            actions = listOf(
                                AppBar.Action(
                                    title = "Settings", // TODO copy
                                    icon = Icons.Default.Settings,
                                    onClick = {
                                        navigator.push(SettingsScreen)
                                    },
                                )
                            )
                        )
                    }
                )
            }
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val wifiName = state.wifiConnectionInfo?.ssid
                    Text(
                        text = when {
                            wifiName != null -> stringResource(R.string.connected_to, wifiName)
                            else -> stringResource(R.string.not_connected)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (state.loading) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("")
                    }
                    if (!state.loading) {
                        Icon(
                            modifier = Modifier.size(40.dp),
                            imageVector = when {
                                attended -> Icons.Default.Check
                                else -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = ButtonDefaults.textButtonColors().contentColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        when {
                            attended -> {
                                Text(
                                    text = "You have attended today" // TODO copy
                                )
                            }
                            else -> {
                                Text(
                                    text = "You have not attended today" // TODO copy
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}