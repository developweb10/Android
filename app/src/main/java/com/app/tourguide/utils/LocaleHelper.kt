package com.app.tourguide.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.util.*

/**
 * Created by Suraj Bahadur on 16-Jul-20.
 */
object LocaleHelper {
    const val TAG = "LocaleHelper"

    fun updateLocale(base: Context): Context {
        Preferences.prefs?.getString("app_language", "en").let {
            return if(it!!.isNotEmpty()){
                Log.d("asdfasdfasdf",it.toString())
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                    updateResources(base, it)
                } else {
                    updateResourcesLegacy(base, it)
                }
            }else{
                base
            }
        }

    }

    private fun updateResources(base: Context, language: String): Context{
        val loc = Locale(language)
        Locale.setDefault(loc)
        val configuration = base.resources.configuration
        configuration.setLocale(loc)
        return base.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(base: Context, language: String): Context{
        val locale = Locale(language)
        Locale.setDefault(locale)
        val configuration = base.resources.configuration
        configuration.locale = locale
        configuration.setLayoutDirection(locale)
        base.resources.updateConfiguration(configuration, base.resources.displayMetrics)
        return base
    }

    fun applyOverrideConfiguration(base: Context, overrideConfiguration: Configuration?): Configuration? {
        if (overrideConfiguration != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val uiMode = overrideConfiguration.uiMode
            overrideConfiguration.setTo(base.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        return overrideConfiguration
    }
}