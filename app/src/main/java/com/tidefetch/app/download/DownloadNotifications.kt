package com.tidefetch.app.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.tidefetch.app.MainActivity
import com.tidefetch.app.R

object DownloadNotifications {
    const val channelId = "media_downloads"
    const val foregroundId = 4101
    const val completionId = 4102

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun active(
        context: Context,
        title: String,
        progress: Float? = null,
    ) = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_monochrome)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(title)
        .setContentIntent(contentIntent(context))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setProgress(
            100,
            ((progress ?: 0f) * 100).toInt().coerceIn(0, 100),
            progress == null,
        )
        .addAction(
            0,
            context.getString(R.string.cancel),
            cancelIntent(context),
        )
        .build()

    fun completed(
        context: Context,
        fileName: String,
        uri: Uri,
        mimeType: String,
    ) =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.download_complete))
            .setContentText(fileName)
            .setContentIntent(mediaIntent(context, uri, mimeType))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

    private fun mediaIntent(context: Context, uri: Uri, mimeType: String): PendingIntent {
        // Route through TideFetch so it remains the fallback destination when
        // the device has no activity capable of viewing this media type.
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_MEDIA
            setDataAndType(uri, mimeType)
            clipData = android.content.ClipData.newUri(context.contentResolver, "TideFetch media", uri)
            addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        return PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelIntent(context: Context): PendingIntent {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL
        }
        return PendingIntent.getService(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
