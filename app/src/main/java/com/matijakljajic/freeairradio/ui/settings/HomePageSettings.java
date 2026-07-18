package com.matijakljajic.freeairradio.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.matijakljajic.freeairradio.ui.homepage.HomePageSource;

public final class HomePageSettings {

    private static final String PREFS_NAME = "homepage_settings";
    private static final String KEY_DEFAULT_SOURCE = "default_source";

    @NonNull
    private final SharedPreferences sharedPreferences;

    public HomePageSettings(@NonNull Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public HomePageSource getDefaultSource() {
        String savedSource = sharedPreferences.getString(KEY_DEFAULT_SOURCE, null);
        if (savedSource == null) {
            return HomePageSource.NOW_POPULAR;
        }

        try {
            return HomePageSource.valueOf(savedSource);
        } catch (IllegalArgumentException exception) {
            return HomePageSource.NOW_POPULAR;
        }
    }

    public void setDefaultSource(@NonNull HomePageSource source) {
        sharedPreferences.edit()
                .putString(KEY_DEFAULT_SOURCE, source.name())
                .apply();
    }
}
