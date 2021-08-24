package com.app.tourguide.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.Locale;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.N;

/**
 * Created by Suraj Bahadur on 17-Aug-20.
 */
public class LocaleManager {

    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_SPANISH = "es";
    private static final String LANGUAGE_KEY = "app_language";

    private final SharedPreferences prefs;

    public LocaleManager(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public Context setLocale(Context c) {
        return updateResources(c, getLanguage());
    }

    public Context setNewLocale(Context c, String language) {
        persistLanguage(language);
        return updateResources(c, language);
    }

    public String getLanguage() {
        return prefs.getString(LANGUAGE_KEY, LANGUAGE_ENGLISH);
    }

    @SuppressLint("ApplySharedPref")
    private void persistLanguage(String language) {
        // use commit() instead of apply(), because sometimes we kill the application process immediately
        // which will prevent apply() to finish
        prefs.edit().putString(LANGUAGE_KEY, language).commit();
    }

    private Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (isAtLeastVersion(JELLY_BEAN_MR1)) {

            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale);
        }

        return context;
    }

    public static Locale getLocale(Resources res) {
        Configuration config = res.getConfiguration();
        return isAtLeastVersion(N) ? config.getLocales().get(0) : config.locale;
    }

    private static boolean isAtLeastVersion(int version) {
        return Build.VERSION.SDK_INT >= version;
    }
}