package com.example.exo_completeguide

import android.content.Context
import android.net.http.HttpEngine
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpEngineDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.datasource.cronet.CronetUtil
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.ExtensionRendererMode
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.example.exo_completeguide.download.ExoDownloadService
import org.chromium.net.CronetEngine
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.Executors


private const val ALLOW_CRONET_FOR_NETWORKING = true
private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

@UnstableApi
object ExoManager {

    var downloadCache: Cache? = null
    var downloadManager: DownloadManager? = null
    var databaseProvider: DatabaseProvider? = null
    var downloadDirectory: File? = null
    var downloadNotificationHelper: DownloadNotificationHelper? = null

    private var dataSourceFactory: DataSource.Factory? = null
    private var httpDataSourceFactory: DataSource.Factory? = null

    @Synchronized
    fun getDataSourceFactory(context: Context): DataSource.Factory {
        var context = context
        if (dataSourceFactory == null) {
            context = context.applicationContext
            val upstreamFactory =
                DefaultDataSource.Factory(
                    context,
                    getHttpDataSourceFactory(context)
                )
            dataSourceFactory =
                buildReadOnlyCacheDataSource(
                    upstreamFactory,
                    getDownloadCache(context)
                )
        }
        return dataSourceFactory!!
    }

    private fun buildReadOnlyCacheDataSource(
        upstreamFactory: DataSource.Factory, cache: Cache
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }


    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        ensureDownloadManagerInitialized(context)
        return downloadManager!!
    }

    @Synchronized
    private fun ensureDownloadManagerInitialized(context: Context) {
        if (downloadManager == null) {
            downloadManager = DownloadManager(
                context,
                getDatabaseProvider(context),
                getDownloadCache(context),
                getHttpDataSourceFactory(context),
                Executors.newFixedThreadPool( /* nThreads= */6)
            )
        }
    }

    @Synchronized
    fun getHttpDataSourceFactory(context: Context): DataSource.Factory {
        var context = context
        if (httpDataSourceFactory != null) {
            return httpDataSourceFactory!!
        }
        context = context.applicationContext
        if (Build.VERSION.SDK_INT >= 34) {
            val httpEngine = HttpEngine.Builder(context).build()
            httpDataSourceFactory =
                HttpEngineDataSource.Factory(httpEngine, Executors.newSingleThreadExecutor())
            return httpDataSourceFactory!!
        }
        if (ALLOW_CRONET_FOR_NETWORKING) {
            val cronetEngine: CronetEngine? = CronetUtil.buildCronetEngine(context)
            if (cronetEngine != null) {
                httpDataSourceFactory =
                    CronetDataSource.Factory(cronetEngine, Executors.newSingleThreadExecutor())
                return httpDataSourceFactory!!
            }
        }
        // The device doesn't support HttpEngine or we don't want to allow Cronet, or we failed to
        // instantiate a CronetEngine.
        val cookieManager: CookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        CookieHandler.setDefault(cookieManager)
        httpDataSourceFactory = DefaultHttpDataSource.Factory()
        return httpDataSourceFactory!!
    }

    @Synchronized
    private fun getDatabaseProvider(context: Context): DatabaseProvider {
        if (databaseProvider == null) {
            databaseProvider = StandaloneDatabaseProvider(context)
        }
        return databaseProvider!!
    }

    @Synchronized
    private fun getDownloadCache(context: Context): Cache {
        if (downloadCache == null) {
            val downloadContentDirectory: File =
                File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
            downloadCache = SimpleCache(
                downloadContentDirectory, NoOpCacheEvictor(), getDatabaseProvider(context)
            )
        }
        return downloadCache!!
    }

    @Synchronized
    private fun getDownloadDirectory(context: Context): File {
        if (downloadDirectory == null) {
            downloadDirectory = context.getExternalFilesDir(null)
            if (downloadDirectory == null) {
                downloadDirectory = context.filesDir
            }
        }
        return downloadDirectory!!
    }

    @Synchronized
    fun getDownloadNotificationHelper(
        context: Context?
    ): DownloadNotificationHelper {
        if (downloadNotificationHelper == null) {
            downloadNotificationHelper =
                DownloadNotificationHelper(
                    context!!,
                    ExoDownloadService.DOWNLOAD_NOTIFICATION_CHANNEL_ID
                )
        }
        return downloadNotificationHelper!!
    }


    fun buildRenderersFactory(context: Context): RenderersFactory {
        val extensionRendererMode: @ExtensionRendererMode Int = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        return DefaultRenderersFactory(context.applicationContext).setExtensionRendererMode(extensionRendererMode)
    }


}