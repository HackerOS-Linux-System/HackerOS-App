package com.hackeros.app.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hackeros.app.MainActivity
import com.hackeros.app.R
import com.hackeros.app.data.model.Language
import com.hackeros.app.data.model.ReleaseInfo

/**
 * Owns the "new release" notification channel and posts notifications when the background
 * release-check worker (see [com.hackeros.app.worker.ReleaseCheckWorker]) detects a version
 * that's newer than the last one the user has seen.
 */
object NotificationHelper {

    const val CHANNEL_ID = "hackeros_releases"
    private const val NOTIFICATION_ID = 2001

    fun ensureChannel(context: Context, language: Language) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val t = getTranslations(language)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                t.notif_channel_name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = t.notif_channel_desc
            }
            manager.createNotificationChannel(channel)
        } else {
            // Keep the channel's user-visible name/description in sync with the current
            // in-app language (Android allows updating name/description of an existing channel).
            existing.name = t.notif_channel_name
            existing.description = t.notif_channel_desc
            manager.createNotificationChannel(existing)
        }
    }

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showNewReleaseNotification(context: Context, release: ReleaseInfo, language: Language) {
        if (!hasPermission(context)) return
        ensureChannel(context, language)

        val t = getTranslations(language)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "${t.notif_new_release_body} ${release.version} — ${release.description}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(t.notif_new_release_title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
