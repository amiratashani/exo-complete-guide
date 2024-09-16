package com.example.exo_completeguide.download

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService

@OptIn(UnstableApi::class)
class CustomNotification(
    private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val CHANNEL_ID = "download_channel"

    fun createNotification(
        download: Download?,
        icon: Int = android.R.drawable.stat_sys_download
    ): Notification {
        if(download == null) return NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(icon).build()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(download.request.id)
            .setContentText(getNotificationText(download))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(download.state == Download.STATE_DOWNLOADING)
            .setAutoCancel(download.state == Download.STATE_COMPLETED)

        if (download.state == Download.STATE_DOWNLOADING) {
            builder.setProgress(100, download.percentDownloaded.toInt(), false)

            val contentText = "میزان دانلود شده: ${download.percentDownloaded.toInt()}% "
            builder.setStyle(NotificationCompat.BigTextStyle()
                .bigText(contentText))
                .setContentText(contentText)
        }

        if (download.state == Download.STATE_DOWNLOADING) {
            builder.addAction(android.R.drawable.ic_media_pause, "توقف دانلود", getPauseIntent(download))
        } else if (download.state == Download.STATE_QUEUED) {
            builder.addAction(android.R.drawable.ic_media_play, "ادامه دانلود", getResumeIntent(download))
        }
//        val  notification = builder.build()
//        NotificationUtil.setNotification(context, download.request.id.toInt(), notification);
        return builder.build()
    }

    // Update the notification based on the download state and progress
//    fun updateNotification(download: Download) {
//        val notification = createNotification(download)
//        notificationManager.notify(download.request.id.hashCode(), notification)
//    }

    private fun getNotificationText(download: Download): String {
        return when (download.state) {
            Download.STATE_COMPLETED -> "دانلود تکمیل شد"
            Download.STATE_DOWNLOADING -> "درحال دانلود..."
            Download.STATE_QUEUED -> "پردازش دانلود"
            Download.STATE_FAILED -> "خطا در دانلود"
            else -> "نمی دونم چی شده :)"
        }
    }

    private fun getPauseIntent(download: Download): PendingIntent {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = "PAUSE_DOWNLOAD"
            putExtra("KEY_DOWNLOAD_ID", download.request.id)
        }
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_MUTABLE)
    }

    private fun getResumeIntent(download: Download): PendingIntent {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = "RESUME_DOWNLOAD"
            putExtra("KEY_DOWNLOAD_ID", download.request.id)
        }
        return PendingIntent.getService(context, 0, intent,  PendingIntent.FLAG_MUTABLE)
    }


}