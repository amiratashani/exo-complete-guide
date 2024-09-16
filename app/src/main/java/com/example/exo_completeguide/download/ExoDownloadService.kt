package com.example.exo_completeguide.download

import android.app.Notification
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.example.exo_completeguide.ExoManager
import com.example.exo_completeguide.R
import kotlin.random.Random

private const val JOB_ID = 1

@UnstableApi
class ExoDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.exo_download_notification_channel_name,
    0) {

    private var random = Random.nextInt()

    private lateinit var customDownloadNotification: CustomNotification

    override fun getDownloadManager(): DownloadManager {
        val downloadManager: DownloadManager = ExoManager.getDownloadManager(this)
//        val downloadNotificationHelper: DownloadNotificationHelper =
//            ExoManager.getDownloadNotificationHelper(this)
        customDownloadNotification = CustomNotification(this)
        downloadManager.addListener(
            TerminalStateNotificationHelper(
                this, customDownloadNotification, Random.nextInt()
            )
        )
        return downloadManager
    }

    override fun getScheduler(): Scheduler? {
        return if (Util.SDK_INT >= 21) PlatformScheduler(this, JOB_ID) else null
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>, notMetRequirements: Int
    ): Notification {
        customDownloadNotification = CustomNotification(this)
//        val notificationId = downloads.firstOrNull()?.request?.id
//        val notification = customDownloadNotification.createNotification(downloads.first())
//        val notificationManager = NotificationManagerCompat.from(this)
        val download = if (downloads.isEmpty()) null else downloads.first()
        return customDownloadNotification.createNotification(download)

//        return ExoManager.getDownloadNotificationHelper( /* context= */this)
//            .buildProgressNotification( /* context= */
//                this,
//                R.drawable.ic_download,  /* contentIntent= */
//                null,  /* message= */
//                null,
//                downloads,
//                notMetRequirements
//            )
    }



    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 2
        const val DOWNLOAD_NOTIFICATION_CHANNEL_ID: String = "download_channel"
    }
}