package com.example.exo_completeguide

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ErrorMessageProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.AdsConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.RepeatMode
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ads.AdsLoader
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import com.example.exo_completeguide.data.Video
import com.example.exo_completeguide.databinding.ActivityPlayerBinding
import kotlin.math.max


@OptIn(UnstableApi::class)
open class PlayerActivity : AppCompatActivity(), ControllerVisibilityListener {
    // Saved instance state keys.
    private val KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters"
    private val KEY_SERVER_SIDE_ADS_LOADER_STATE = "server_side_ads_loader_state"
    private val KEY_ITEM_INDEX = "item_index"
    private val KEY_POSITION = "position"
    private val KEY_AUTO_PLAY = "auto_play"

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    var player: ExoPlayer? = null
    var playerView: PlayerView? = null
    private var dataSourceFactory: DataSource.Factory? = null

    // For ad playback only.
    private var clientSideAdsLoader: AdsLoader? = null
    private var serverSideAdsLoaderState: ImaServerSideAdInsertionMediaSource.AdsLoader.State? =
        null
    private var serverSideAdsLoader: ImaServerSideAdInsertionMediaSource.AdsLoader? = null
    private var isShowingTrackSelectionDialog = false
    private var selectTracksButton: Button? = null
    private var mediaItems: List<MediaItem>? = null
    private var trackSelectionParameters: TrackSelectionParameters? = null
    private var lastSeenTracks: Tracks? = null
    private var startAutoPlay = false
    private var startItemIndex = 0
    private var startPosition: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(viewBinding.root)
        updateWindowInset()

        //Create data source factory
        dataSourceFactory = ExoManager.getDataSourceFactory(this)


        viewBinding.selectTracksButton.apply {
            selectTracksButton = this
            setOnClickListener {
                if (!isShowingTrackSelectionDialog && TrackSelectionDialog.willHaveContent(player!!)) {
                    isShowingTrackSelectionDialog = true
                    val trackSelectionDialog =
                        TrackSelectionDialog.createForPlayer(player!!) { dismissedDialog -> isShowingTrackSelectionDialog = false }
                    trackSelectionDialog.show(supportFragmentManager, null)
                }
            }
        }

        viewBinding.playerView.apply {
            playerView = this
            setControllerVisibilityListener(this@PlayerActivity)
            setErrorMessageProvider(PlayerErrorMessageProvider())
            requestFocus()
        }



        if (savedInstanceState != null) {
            trackSelectionParameters =
                TrackSelectionParameters.fromBundle(
                    savedInstanceState.getBundle(
                        KEY_TRACK_SELECTION_PARAMETERS
                    )!!
                )
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY)
            startItemIndex = savedInstanceState.getInt(KEY_ITEM_INDEX)
            startPosition = savedInstanceState.getLong(KEY_POSITION)
            restoreServerSideAdsLoaderState(savedInstanceState)
        } else {
            trackSelectionParameters = TrackSelectionParameters.Builder( /* context= */this).build()
            clearStartPosition()
        }
    }

    private fun updateWindowInset() {
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * @return Whether initialization was successful.
     */
    private fun initializePlayer(): Boolean {
        val intent = intent
        if (player == null) {
            //Create Media Items
            mediaItems = createMediaItems(intent)
            if (mediaItems!!.isEmpty()) {
                return false
            }

//            lastSeenTracks = Tracks.EMPTY

            //Create Media Items
            val mediaSourceFactory = createMediaSourceFactory()

            //Create Player Builder
            val playerBuilder = ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)

            //Create Player
            player = playerBuilder.build()
            player!!.trackSelectionParameters = trackSelectionParameters!!
            player!!.addListener(PlayerEventListener())
            player!!.addAnalyticsListener(EventLogger())
            player!!.setAudioAttributes(AudioAttributes.DEFAULT,  true)
            player!!.playWhenReady = startAutoPlay
            playerView!!.player = player
            configurePlayerWithServerSideAdsLoader()
        }
        val haveStartPosition = startItemIndex != C.INDEX_UNSET
        if (haveStartPosition) {
            player!!.seekTo(startItemIndex, startPosition)
        }
        player!!.setMediaItems(mediaItems!!, !haveStartPosition)
        player!!.prepare()
        player!!.repeatMode = parseRepeatModeExtra("ALL")
        updateButtonVisibility()
        return true
    }

    private fun createMediaItems(intent: Intent): List<MediaItem> {
        val videos = intent.getParcelableArrayListExtra<Video>(VIDEOS_KEY)
        val mediaItem = mutableListOf<MediaItem>()
        videos?.forEach { video ->
            val mediaItemBuilder = MediaItem.Builder()
            val uri = Uri.parse(video.uri)
            val adaptiveMimeType =
                Util.getAdaptiveMimeTypeForContentType(Util.inferContentType(uri))
            mediaItemBuilder
                .setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(video.name).build())
                .setMimeType(adaptiveMimeType)

            if (video.adTagUri != null) {
                mediaItemBuilder.setAdsConfiguration(
                    AdsConfiguration.Builder(Uri.parse(video.adTagUri)).build()
                )
            }
            if (video.subtitleConfiguration != null) {
                mediaItemBuilder.setSubtitleConfigurations(
                    emptyList()
                )
            }

            mediaItem.add(mediaItemBuilder.build())
        }
        return mediaItem
    }

    // User controls
    private fun updateButtonVisibility() {
        selectTracksButton!!.isEnabled =
            player != null && TrackSelectionDialog.willHaveContent(player!!)
    }


    private fun createMediaSourceFactory(): MediaSource.Factory {

        //Create Drm Manager
        val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
        drmSessionManagerProvider.setDrmHttpDataSourceFactory(
            ExoManager.getHttpDataSourceFactory(this)
        )

        //Create server ad loader
        val serverSideAdLoaderBuilder: ImaServerSideAdInsertionMediaSource.AdsLoader.Builder =
            ImaServerSideAdInsertionMediaSource.AdsLoader.Builder(this, playerView!!)
        if (serverSideAdsLoaderState != null) {
            serverSideAdLoaderBuilder.setAdsLoaderState(serverSideAdsLoaderState!!)
        }
        serverSideAdsLoader = serverSideAdLoaderBuilder.build()
        val imaServerSideAdInsertionMediaSourceFactory: ImaServerSideAdInsertionMediaSource.Factory =
            ImaServerSideAdInsertionMediaSource.Factory(
                serverSideAdsLoader!!,
                DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory!!)
            )


        //Create client ad loader
        val adsLoaderProvider = AdsLoader.Provider { adsConfiguration: AdsConfiguration ->
            getClientSideAdsLoader(adsConfiguration)
        }

        return DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory!!)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)
            .setLocalAdInsertionComponents(
                adsLoaderProvider,
                playerView!!
            )
            .setServerSideAdInsertionMediaSourceFactory(imaServerSideAdInsertionMediaSourceFactory)
    }

    private fun getClientSideAdsLoader(adsConfiguration: AdsConfiguration): AdsLoader {
        // The ads loader is reused for multiple playbacks, so that ad playback can resume.
        if (clientSideAdsLoader == null) {
            clientSideAdsLoader = ImaAdsLoader.Builder( /* context= */this).build()
        }
        clientSideAdsLoader?.setPlayer(player)
        return clientSideAdsLoader!!
    }

    private fun configurePlayerWithServerSideAdsLoader() {
        serverSideAdsLoader!!.setPlayer(player!!)
    }


    private fun releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters()
            updateStartPosition()
            releaseServerSideAdsLoader()
            player!!.release()
            player = null
            playerView!!.player = null
            mediaItems = emptyList()
        }
        if (clientSideAdsLoader != null) {
            clientSideAdsLoader!!.setPlayer(null)
        } else {
            playerView!!.adViewGroup.removeAllViews()
        }
    }

    private fun releaseServerSideAdsLoader() {
        serverSideAdsLoaderState = serverSideAdsLoader!!.release()
        serverSideAdsLoader = null
    }

    private fun releaseClientSideAdsLoader() {
        if (clientSideAdsLoader != null) {
            clientSideAdsLoader!!.release()
            clientSideAdsLoader = null
            playerView!!.adViewGroup.removeAllViews()
        }
    }

    private fun updateStartPosition() {
        if (player != null) {
            startAutoPlay = player!!.playWhenReady
            startItemIndex = player!!.currentMediaItemIndex
            startPosition = max(0, player!!.contentPosition)
        }
    }

    private fun updateTrackSelectorParameters() {
        if (player != null) {
            trackSelectionParameters = player!!.trackSelectionParameters
        }
    }

    private fun clearStartPosition() {
        startAutoPlay = true
        startItemIndex = C.INDEX_UNSET
        startPosition = C.TIME_UNSET
    }

    public override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer()
            if (playerView != null) {
                playerView!!.onResume()
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer()
            if (playerView != null) {
                playerView!!.onResume()
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= 23) {
            if (playerView != null) {
                playerView!!.onPause()
            }
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            if (playerView != null) {
                playerView!!.onPause()
            }
            releasePlayer()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        releaseClientSideAdsLoader()
    }


    private fun showControls() {
        viewBinding.controlsRoot.visibility = View.VISIBLE
    }

    private fun showToast(messageId: Int) {
        showToast(getString(messageId))
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val VIDEOS_KEY = "VIDEOS_KEY"
    }

    inner class PlayerEventListener : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
            if (playbackState == Player.STATE_ENDED) {
                showControls()
            }
            updateButtonVisibility()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e("Amir", "onPlayerError: $error")
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player?.seekToDefaultPosition()
                player?.prepare()
            } else {
                updateButtonVisibility()
                showControls()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateButtonVisibility()
            if (tracks === lastSeenTracks) {
                return
            }
            if (tracks.containsType(C.TRACK_TYPE_VIDEO)
                && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO,  /* allowExceedsCapabilities= */true)
            ) {
                showToast(R.string.error_unsupported_video)
            }
            if (tracks.containsType(C.TRACK_TYPE_AUDIO)
                && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO,  /* allowExceedsCapabilities= */true)
            ) {
                showToast(R.string.error_unsupported_audio)
            }
            lastSeenTracks = tracks
        }
    }

    private fun parseRepeatModeExtra(repeatMode: String): @RepeatMode Int {
        return when (repeatMode) {
            "OFF" -> Player.REPEAT_MODE_OFF
            "ONE" -> Player.REPEAT_MODE_ONE
            "ALL" -> Player.REPEAT_MODE_ALL
            else -> throw IllegalArgumentException(
                "Argument $repeatMode does not match any of the repeat modes: OFF|ONE|ALL"
            )
        }
    }

    private fun restoreServerSideAdsLoaderState(savedInstanceState: Bundle) {
        val adsLoaderStateBundle =
            savedInstanceState.getBundle(KEY_SERVER_SIDE_ADS_LOADER_STATE)
        if (adsLoaderStateBundle != null) {
            serverSideAdsLoaderState =
                ImaServerSideAdInsertionMediaSource.AdsLoader.State.fromBundle(adsLoaderStateBundle)
        }
    }

    inner class PlayerErrorMessageProvider : ErrorMessageProvider<PlaybackException> {
        // Using decoder exceptions
        override fun getErrorMessage(e: PlaybackException): Pair<Int, String> {
            var errorString: String = getString(R.string.error_generic)
            val cause = e.cause
            if (cause is MediaCodecRenderer.DecoderInitializationException) {
                // Special case for decoder initialization failures.
                val decoderInitializationException =
                    cause
                if (decoderInitializationException.codecInfo == null) {
                    if (decoderInitializationException.cause is MediaCodecUtil.DecoderQueryException) {
                        errorString = getString(R.string.error_querying_decoders)
                    } else if (decoderInitializationException.secureDecoderRequired) {
                        errorString =
                            getString(
                                R.string.error_no_secure_decoder,
                                decoderInitializationException.mimeType
                            )
                    } else {
                        errorString =
                            getString(
                                R.string.error_no_decoder,
                                decoderInitializationException.mimeType
                            )
                    }
                } else {
                    errorString =
                        getString(
                            R.string.error_instantiating_decoder,
                            decoderInitializationException.codecInfo!!.name
                        )
                }
            }
            return Pair.create(0, errorString)
        }
    }

    override fun onVisibilityChanged(visibility: Int) {
        viewBinding.controlsRoot.visibility = visibility
    }


}