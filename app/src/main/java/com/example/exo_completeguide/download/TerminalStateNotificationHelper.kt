package com.example.exo_completeguide.download


import android.content.Context
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.example.exo_completeguide.R


@UnstableApi
class TerminalStateNotificationHelper(
    private val context: Context,
//    private val notificationHelper: DownloadNotificationHelper,
    private val notificationHelper: CustomNotification,
    firstNotificationId: Int
) : DownloadManager.Listener {
    private var nextNotificationId = firstNotificationId
    override fun onDownloadChanged(
        downloadManager: DownloadManager,
        download: Download,
        finalException: Exception?
    ) {
        val notification = if (download.state === Download.STATE_COMPLETED) {
            notificationHelper.createNotification(download, R.drawable.ic_download_done)

//            notificationHelper.customDownloadNotification(
//                context,
//                R.drawable.ic_download_done,  /* contentIntent= */
//                null,
//                Util.fromUtf8Bytes(download.request.data)
//            )
        } else if (download.state === Download.STATE_FAILED) {
            notificationHelper.createNotification(download,  R.drawable.ic_download_done)

//            notificationHelper.buildDownloadFailedNotification(
//                context,
//                R.drawable.ic_download_done,  /* contentIntent= */
//                null,
//                Util.fromUtf8Bytes(download.request.data)
//            )
        }else {
            return
        }
        NotificationUtil.setNotification(
            context,
            download.request.id.toIntOrNull() ?: nextNotificationId++,
            notification
        )
    }


}