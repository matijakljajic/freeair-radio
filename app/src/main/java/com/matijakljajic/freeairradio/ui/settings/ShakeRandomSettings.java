package com.matijakljajic.freeairradio.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class ShakeRandomSettings {

    private static final String PREFS_NAME = "shake_random_settings";
    private static final String KEY_ENABLED = "enabled";

    @NonNull
    private final SharedPreferences sharedPreferences;

    public ShakeRandomSettings(@NonNull Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return sharedPreferences.getBoolean(KEY_ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        sharedPreferences.edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
    }
}
