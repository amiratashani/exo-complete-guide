package com.example.exo_completeguide

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.offline.DownloadService
import com.example.exo_completeguide.data.getTelewebionHls
import com.example.exo_completeguide.data.getTelewebionLive
import com.example.exo_completeguide.data.getTelewebionLivePlayList
import com.example.exo_completeguide.data.getTelewebionPlayListHls
import com.example.exo_completeguide.databinding.ActivityMainBinding
import com.example.exo_completeguide.download.DownloadTracker
import com.example.exo_completeguide.download.ExoDownloadService


@OptIn(UnstableApi::class)
class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private var downloadTracker: DownloadTracker? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(viewBinding.root)
        startDownloadService();
        updateWindowInset()
        downloadTracker =  ExoManager.getDownloadTracker(baseContext);

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
                val video = getTelewebionHls()[0]
                val mediaItemBuilder = MediaItem.Builder()
                mediaItemBuilder
                    .setUri(Uri.parse(video.uri))
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(video.name).build())
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                val renderersFactory: RenderersFactory =
                    ExoManager.buildRenderersFactory( /* context= */
                        baseContext, false
                    )
                downloadTracker?.toggleDownload(supportFragmentManager, mediaItemBuilder.build(), renderersFactory);
            }

            btn6.isEnabled = false
            btn6.setOnClickListener {

            }

        }
    }

    private fun updateWindowInset() {
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startDownloadService() {
        // Starting the service in the foreground causes notification flicker if there is no scheduled
        // action. Starting it in the background throws an exception if the app is in the background too
        // (e.g. if device screen is locked).
        try {
            DownloadService.start(this, ExoDownloadService::class.java)
        } catch (e: IllegalStateException) {
            DownloadService.startForeground(this, ExoDownloadService::class.java)
        }
    }


}