package com.fdossena.speedtest.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
// Using the root package MainActivity as a fallback/assumption for the service target
import com.fdossena.speedtest.ui.MainActivity 
import com.mr.speed.test.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "mr_speed_alerts_v5"
        private const val NOTIFICATION_ID = 1001
    }

    private val soundUri: Uri by lazy {
        // Assuming R.raw.notification exists and is correctly compiled in the module's resources.
        Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.notification}")
    }

    fun showNetworkAlert() {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_START_TEST", true)
        }

        // Using standard PendingIntent creation logic for robustness
        val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            @Suppress("DEPRECATION")
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.speed_test)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.notif_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_tile_speedtest,
                context.getString(R.string.notif_action_run),
                pendingIntent
            )

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                // Permission handled in MainActivity or via runtime request
            }
        }
    }

    fun showAdminAlertNotification(title: String, message: String) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_ADMIN_CHAT", true)
        }

        val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(context, (System.currentTimeMillis() % 10000).toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            @Suppress("DEPRECATION")
            PendingIntent.getActivity(context, (System.currentTimeMillis() % 10000).toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notifId = (System.currentTimeMillis() % 100000).toInt()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.speed_test)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notifId, builder.build())
            } catch (e: SecurityException) {
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = "Alerts when a new network connection is detected"
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}