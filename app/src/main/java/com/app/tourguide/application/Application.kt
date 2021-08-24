package com.app.tourguide.application

import Preferences
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.multidex.MultiDex
import com.app.tourguide.BuildConfig
import com.app.tourguide.utils.LocaleManager
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.HttpUrlConnectionDownloader
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import com.tonyodev.fetch2rx.RxFetch
import okhttp3.OkHttpClient
import timber.log.Timber

/**
 * Created by android on 3/11/17.
 */
class Application : Application() {

    var mContext: Context? = null

    override fun attachBaseContext(base: Context?) {
        localManger=LocaleManager(base)
        super.attachBaseContext(base)
        MultiDex.install(this)
    }


    companion object AppContext {
        lateinit var instance: com.app.tourguide.application.Application
        lateinit var localManger: LocaleManager
        fun getContext(): Context {
            return instance
        }
    }

    init {
        instance = this
    }

    private val okHttpDownloader: OkHttpDownloader
        get() {
            val okHttpClient = OkHttpClient.Builder().build()
            return OkHttpDownloader(okHttpClient,
                    Downloader.FileDownloaderType.PARALLEL)
        }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }


        Preferences.initPreferences(this)
        mContext = applicationContext

        val fetchConfiguration = FetchConfiguration.Builder(this)
                .enableRetryOnNetworkGain(true)
                .setDownloadConcurrentLimit(3)
                .setHttpDownloader(HttpUrlConnectionDownloader(Downloader.FileDownloaderType.PARALLEL))
                // OR
                //.setHttpDownloader(getOkHttpDownloader())
                .build()
        Fetch.setDefaultInstanceConfiguration(fetchConfiguration)
        RxFetch.setDefaultRxInstanceConfiguration(fetchConfiguration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localManger.setLocale(this);
    }

}
