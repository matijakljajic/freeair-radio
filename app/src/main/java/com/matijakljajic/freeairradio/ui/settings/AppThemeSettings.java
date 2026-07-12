package com.matijakljajic.freeairradio.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class AppThemeSettings {

    private static final String PREFS_NAME = "app_theme_settings";
    private static final String KEY_NIGHT_MODE = "night_mode";

    @IntDef({
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface NightMode {
    }

    @NonNull
    private final SharedPreferences sharedPreferences;

    public AppThemeSettings(@NonNull Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NightMode
    public int getNightMode() {
        return sharedPreferences.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setNightMode(@NightMode int nightMode) {
        sharedPreferences.edit().putInt(KEY_NIGHT_MODE, nightMode).apply();
    }

    public void applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(getNightMode());
    }
}
