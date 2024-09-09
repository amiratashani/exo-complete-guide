package com.example.exo_completeguide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.example.exo_completeguide.data.getTelewebionHls
import com.example.exo_completeguide.data.getTelewebionLive
import com.example.exo_completeguide.data.getTelewebionLivePlayList
import com.example.exo_completeguide.data.getTelewebionPlayListHls
import com.example.exo_completeguide.databinding.ActivityMainBinding
import com.example.exo_completeguide.download.ExoDownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(UnstableApi::class)
class MainActivity : AppCompatActivity() {
    private lateinit var downloadManager: DownloadManager

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var selectedVideo : Int = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            startDownload()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(viewBinding.root)
        updateWindowInset()
        downloadManager = ExoManager.getDownloadManager(this)


        viewBinding.apply {
            btn1.setOnClickListener {
                val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                intent.putParcelableArrayListExtra(PlayerActivity.VIDEOS_KEY, getTelewebionHls())
                startActivity(intent)
            }

            btn2.setOnClickListener {
                val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                intent.putParcelableArrayListExtra(PlayerActivity.VIDEOS_KEY, getTelewebionPlayListHls())
                startActivity(intent)
            }

            btn3.setOnClickListener {
                val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                intent.putParcelableArrayListExtra(PlayerActivity.VIDEOS_KEY, getTelewebionLive())
                startActivity(intent)
            }

            btn4.setOnClickListener {
                val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                intent.putParcelableArrayListExtra(PlayerActivity.VIDEOS_KEY, getTelewebionLivePlayList())
                startActivity(intent)
            }

            btn5.setOnClickListener {

            }

            btn6.isEnabled = false
            btn6.setOnClickListener {
            }

            btn7.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    checkNotificationPermission()
                } else {
                    startDownload()
                }
            }

            btn8.setOnClickListener {
                var downloadId = ""
                when(selectedVideo){
                    1 -> {downloadId = "-448633644"}
                    2 -> {downloadId = "-1623720552"}
                    3 -> {downloadId = "1379405696"}
                    4 -> {downloadId = "330729129"}
                }
               pauseDownload(downloadId)
            }

            btn9.setOnClickListener{
                var downloadId = ""
                when(selectedVideo){
                    1 -> {downloadId = "-448633644"}
                    2 -> {downloadId = "-1623720552"}
                    3 -> {downloadId = "1379405696"}
                    4 -> {downloadId = "330729129"}
                }
                resumeDownload(downloadId)
            }

            btn10.setOnClickListener{
                var downloadId = ""
                when(selectedVideo){
                    1 -> {downloadId = "-448633644"}
                    2 -> {downloadId = "-1623720552"}
                    3 -> {downloadId = "1379405696"}
                    4 -> {downloadId = "330729129"}
                }
                deleteDownload(downloadId)
            }

            radioGroup.setOnCheckedChangeListener{ group, checkedId ->
                val radioButton = findViewById<RadioButton>(checkedId)
                val selectedText = radioButton.text
                when(selectedText){
                    "Video 1" -> selectedVideo = 1
                    "Video 2" -> selectedVideo = 2
                    "Video 3" -> selectedVideo = 3
                    "Video 4" -> selectedVideo = 4
                }

            }

            downloadManager.addListener(object : DownloadManager.Listener{
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?
                ) {
                    super.onDownloadChanged(downloadManager, download, finalException)
                    if(download.state == Download.STATE_COMPLETED){
                        Log.d("TAG-sahar", "111111111111onDownloadChanged: COMPETED")
                    }
                    CoroutineScope(Dispatchers.Default).launch {
                        while(download.state == Download.STATE_DOWNLOADING){
                            if (download.percentDownloaded >= 0 && download.stopReason != Download.STATE_COMPLETED) {
                                Log.d("TAG-sahar", "Progress: ${download.percentDownloaded}%")
                                Log.d("TAG-sahar", "Progress: ${download.bytesDownloaded}")
                                Log.d("TAG-sahar", "Progress: ${download.state}")
                            }
                            delay(100)
                        }
                    }
                }

                override fun onDownloadRemoved(
                    downloadManager: DownloadManager,
                    download: Download
                ) {
                    super.onDownloadRemoved(downloadManager, download)
                }

                override fun onDownloadsPausedChanged(
                    downloadManager: DownloadManager,
                    downloadsPaused: Boolean
                ) {
                    super.onDownloadsPausedChanged(downloadManager, downloadsPaused)
                }

                override fun onIdle(downloadManager: DownloadManager) {
                    super.onIdle(downloadManager)
                }
            })
        }
    }

    private fun updateWindowInset() {
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startDownload(){
        var contentUri = ""
        var downloadId = ""
        when(selectedVideo){
            1 ->{
                //2.86 MB
                contentUri = "https://dl.telewebion.com/c144517c-10bf-4d21-820b-b8c612851da1/0xddad9a8/480p/"
                downloadId = "-448633644"
            }
            2 ->{
                //45.53 MB
                contentUri = "https://dl.telewebion.com/c144517c-10bf-4d21-820b-b8c612851da1/0xddb6afb/480p/"
                downloadId = "-1623720552"
            }
            3 ->{
                //1.25 GB
                contentUri = "https://dl.telewebion.com/c144517c-10bf-4d21-820b-b8c612851da1/0xdd98ea6/1080p/"
                downloadId = "1379405696"
            }
            4 ->{
                //34.14 MB
                contentUri = "https://dl.telewebion.com/c144517c-10bf-4d21-820b-b8c612851da1/0xdd9550e/480p/"
                downloadId = "330729129"
            }
        }
        val downloadRequest = DownloadRequest.Builder(downloadId, Uri.parse(contentUri)).build()
        DownloadService.sendAddDownload(
            this@MainActivity,
            ExoDownloadService::class.java,
            downloadRequest,
            true
        )
    }



    private fun pauseDownload(downloadId: String){
        DownloadService.sendSetStopReason(
            this@MainActivity,
            ExoDownloadService::class.java,
            downloadId,
            Download.FAILURE_REASON_UNKNOWN,
            true)
    }

    private fun resumeDownload(downloadId: String){
        DownloadService.sendSetStopReason(
            this@MainActivity,
            ExoDownloadService::class.java,
            downloadId,
            Download.STOP_REASON_NONE,
            true
        )
    }

    private fun deleteDownload(downloadId: String){
        downloadManager.removeDownload(downloadId)
    }

    private fun getAllDownloads(): MutableList<Download>{
        return downloadManager.currentDownloads
    }

    private fun checkNotificationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(this, "Notification permission already granted", Toast.LENGTH_SHORT).show()
                startDownload()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                Toast.makeText(this, "Notification permission is needed to show notifications", Toast.LENGTH_SHORT).show()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private suspend fun monitorDownloadProgress() {
        CoroutineScope(Dispatchers.IO).launch {
            downloadManager.currentDownloads.forEach { download ->
                while(download.state == Download.STATE_DOWNLOADING){
                    if (download.percentDownloaded >= 0) {
                        Log.d("TAG-sahar", "Progress: ${download.percentDownloaded}%")
                        Log.d("TAG-sahar", "Progress: ${download.bytesDownloaded}")
                        Log.d("TAG-sahar", "Progress: ${download.state}")
                    }
                    delay(1000)
                }
            }
        }
    }

}