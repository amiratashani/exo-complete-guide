package com.example.exo_completeguide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadManager
import com.example.exo_completeguide.data.getTelewebionHls
import com.example.exo_completeguide.data.getTelewebionLive
import com.example.exo_completeguide.data.getTelewebionLivePlayList
import com.example.exo_completeguide.data.getTelewebionPlayListHls
import com.example.exo_completeguide.databinding.ActivityMainBinding
import com.example.exo_completeguide.download.DownloadTracker


@OptIn(UnstableApi::class)
class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(viewBinding.root)
        updateWindowInset()


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

        }
    }

    private fun updateWindowInset() {
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }



}