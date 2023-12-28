package androidx.utils.player.tool

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.media3.common.util.UnstableApi
import androidx.utils.player.MainActivity
import androidx.utils.player.R

@UnstableApi object NotificationTool {
    private const val CHANNEL_ID = "player_channel"
    const val NOTIFICATION_ID = 123

    fun showNotification(context: Context) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        val builder = getNotificationBuilder(context, notificationManagerCompat)
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
    }

    fun getNotificationBuilder(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat = NotificationManagerCompat.from(context)
    ): NotificationCompat.Builder {
        ensureNotificationChannel(context, notificationManagerCompat)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.baseline_add_reaction_24)
            .setContentTitle(context.getString(R.string.notification_content_title))
            .setContentText(context.getString(R.string.notification_content_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .also { builder -> TaskStackBuilder.create(context).run {
                addNextIntent(Intent(context, MainActivity::class.java))
                getPendingIntent(0, if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0 or PendingIntent.FLAG_UPDATE_CURRENT)
            }?.let { builder.setContentIntent(it) } }
    }

    private fun ensureNotificationChannel(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat
    ) {
        if (Build.VERSION.SDK_INT < 26 || notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }
        notificationManagerCompat.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }
}