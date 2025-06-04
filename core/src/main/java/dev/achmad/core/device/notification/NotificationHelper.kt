package dev.achmad.core.device.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat

class NotificationHelper(
    private val context: Context,
) {

    private val TAG = "NotificationHelper"

    /**
     * Data class to hold configuration for a notification.
     */
    data class Data(
        val channelId: String,
        val title: CharSequence,
        val text: CharSequence,
        @DrawableRes val smallIconResId: Int,
        val context: Context,
        val pendingIntent: PendingIntent? = null,
        val onGoing: Boolean = true, // Typical for foreground services
        val autoCancel: Boolean = true, // Usually false for foreground services
        val priority: Int = NotificationCompat.PRIORITY_LOW // For pre-Oreo
    )

    /**
     * Data class to hold configuration for a notification channel.
     */
    data class Channel(
        val id: String,
        val name: CharSequence,
        val description: String?,
        val importance: Int = NotificationManager.IMPORTANCE_LOW, // Default to LOW for foreground services
        val showBadge: Boolean = false, // Default to no badge for ongoing services
        // Add other channel properties like sound, vibration, lightColor if needed
    )

    /**
     * Creates a notification channel if running on Android Oreo (API 26) or higher.
     * This should ideally be called from your Application's `onCreate()` method.
     *
     * @param notificationChannelConfig [Channel] Configuration for the channel.
     */
    fun createNotificationChannels(
        channels: List<Channel>
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(
            channels.map { config ->
                NotificationChannel(
                    config.id,
                    config.name,
                    config.importance
                ).apply {
                    config.description?.let { description = it }
                    setShowBadge(config.showBadge)
                    // Configure other channel settings here if added to ChannelConfig
                    // e.g., setSound(null, null) for silent channels
                }
            }
        )
    }

    /**
     * Builds a Notification object.
     *
     * @param notificationData [NotificationHelper.Data]
     */
    fun createNotification(
        notificationData: Data
    ) = createNotification(
        channelId = notificationData.channelId,
        title = notificationData.title,
        text = notificationData.text,
        smallIconResId = notificationData.smallIconResId,
        context = notificationData.context,
        pendingIntent = notificationData.pendingIntent,
        onGoing = notificationData.onGoing,
        autoCancel = notificationData.autoCancel,
        priority = notificationData.priority
    )

    /**
     * Builds a Notification object.
     *
     * @param channelId The ID of the notification channel to use (must exist on API 26+).
     * @param title The title of the notification.
     * @param text The main text content of the notification.
     * @param smallIconResId The resource ID of the small icon for the notification.
     * @param context Context
     * @param pendingIntent Optional PendingIntent to be fired when the notification is tapped.
     * @param onGoing Whether this notification is for an ongoing event (like a foreground service).
     * @param autoCancel Whether the notification should be automatically dismissed when tapped.
     * @param priority Priority for pre-Oreo devices (use channel importance for Oreo+).
     * @return A Notification object.
     */
    fun createNotification(
        channelId: String,
        title: CharSequence,
        text: CharSequence,
        @DrawableRes smallIconResId: Int,
        context: Context = this.context,
        pendingIntent: PendingIntent? = null,
        onGoing: Boolean = true, // Typical for foreground services
        autoCancel: Boolean = false, // Usually false for foreground services
        priority: Int = NotificationCompat.PRIORITY_LOW // For pre-Oreo
    ): Notification {
        if (smallIconResId == 0) {
            Log.e(TAG, "Small icon resource ID is 0. Notification will likely not show.")
            throw IllegalArgumentException("smallIconResId is not a valid drawable resource")
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(smallIconResId)
            .setPriority(priority) // For pre-Oreo
            .setOngoing(onGoing)
            .setAutoCancel(autoCancel)

        pendingIntent?.let {
            builder.setContentIntent(it)
        }

        // You can add more builder options here:
        // .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.large_icon))
        // .addAction(R.drawable.ic_action, "Action Text", actionPendingIntent)
        // .setStyle(NotificationCompat.BigTextStyle().bigText("Longer text here..."))

        return builder.build()
    }

    /**
     * Displays or updates a notification.
     *
     * @param notificationId The ID for this notification. If a notification with this ID already exists, it will be updated.
     * @param notification The [Notification] object to display.
     */
    fun notify(
        notificationId: Int,
        notification: Notification
    ) {
        if (notificationId == 0) {
            Log.w(TAG, "Notification ID is 0. This is allowed but can be problematic for startForeground.")
            // startForeground requires a non-zero ID.
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Notification posted/updated with ID: $notificationId")
    }

    /**
     * Helper to create a basic [PendingIntent] to launch an Activity.
     *
     * @param requestCode A private request code for the sender
     * @param activityClass The Activity class to launch.
     * @param context Context
     * @param intentFlags Flags for the intent (e.g., Intent.FLAG_ACTIVITY_SINGLE_TOP).
     * @return A [PendingIntent].
     */
    fun createActivityPendingIntent(
        requestCode: Int,
        activityClass: Class<*>, // e.g., MainActivity::class.java
        context: Context = this.context,
        intentFlags: Int = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    ): PendingIntent {
        val intent = Intent(context, activityClass).apply {
            flags = intentFlags
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}