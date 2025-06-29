package dev.achmad.alephup.ui.checkin

import android.app.NotificationManager
import android.content.Context
import dev.achmad.alephup.R
import dev.achmad.alephup.base.MainActivity
import dev.achmad.core.device.notification.NotificationHelper
import dev.achmad.core.util.extension.injectLazy
import dev.achmad.data.auth.Auth
import dev.achmad.data.auth.AuthState
import dev.achmad.data.checkin.CheckIn
import dev.achmad.data.checkin.CheckInResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CheckInNotifier(
    context: Context,
) {
    private val notificationHelper by injectLazy<NotificationHelper>()
    private val checkIn by injectLazy<CheckIn>()
    private val auth by injectLazy<Auth>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val checkInResultNotificationData = NotificationHelper.Data(
        channelId = NOTIFICATION_CHANNEL_ID,
        title = context.getString(R.string.check_in_success),
        text = "",
        smallIconResId = R.drawable.ic_attendance_success,
        context = context,
        pendingIntent = notificationHelper.createActivityPendingIntent(
            requestCode = 0,
            activityClass = MainActivity::class.java,
            context = context
        ),
        onGoing = true
    )

    init {
        scope.launch {
            checkIn.checkInResultSharedFlow.collect { result ->
                notifyCheckInResult(result)
            }
        }
        scope.launch {
            auth.authState.collect { authState ->
                if (authState is AuthState.SignedOut) {
                    notificationHelper.cancel(CHECK_IN_NOTIFICATION_ID)
                }
            }
        }
    }

    private fun notifyCheckInResult(result: CheckInResult) {
        when(result) {
            CheckInResult.Success -> {
                notificationHelper.notify(
                    notificationId = CHECK_IN_NOTIFICATION_ID,
                    notification = notificationHelper.createNotification(checkInResultNotificationData)
                )
            }
            CheckInResult.HttpError -> {
                notificationHelper.notify(
                    notificationId = CHECK_IN_NOTIFICATION_ID,
                    notification = notificationHelper.createNotification(
                        checkInResultNotificationData.copy(
                            title = "Failed to connect to server", // TODO copy
                            smallIconResId = R.drawable.ic_attendance_error,
                        )
                    )
                )
            }
            CheckInResult.InvalidSSID -> {
                notificationHelper.notify(
                    notificationId = CHECK_IN_NOTIFICATION_ID,
                    notification = notificationHelper.createNotification(
                        checkInResultNotificationData.copy(
                            title = "Not connected to an Aleph Wifi", // TODO copy
                            smallIconResId = R.drawable.ic_attendance_error,
                        )
                    )
                )
            }
            else -> Unit
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "check_in_channel"
        const val CHECK_IN_NOTIFICATION_ID = 1002

        fun createNotificationChannelConfig() = NotificationHelper.Channel(
            id = NOTIFICATION_CHANNEL_ID,
            name = "Check-in Status", // TODO copy
            description = "Information about the most recent attempt to check-in using Wi-Fi connection", // TODO copy
            importance = NotificationManager.IMPORTANCE_LOW,
            showBadge = false,
        )
    }


}