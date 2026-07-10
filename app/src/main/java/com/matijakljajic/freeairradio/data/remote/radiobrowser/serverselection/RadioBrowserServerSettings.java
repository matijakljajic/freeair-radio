package com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class RadioBrowserServerSettings {

    private static final String PREFS_NAME = "radio_browser_server_settings";
    private static final String KEY_PREFERRED_BASE_URL = "preferred_base_url";

    @NonNull
    private final SharedPreferences sharedPreferences;

    public RadioBrowserServerSettings(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    public String getPreferredBaseUrl() {
        String preferredBaseUrl = sharedPreferences.getString(KEY_PREFERRED_BASE_URL, null);
        if (preferredBaseUrl == null) {
            return null;
        }

        String trimmedValue = preferredBaseUrl.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    public void setPreferredBaseUrl(@Nullable String baseUrl) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            editor.remove(KEY_PREFERRED_BASE_URL);
        } else {
            editor.putString(KEY_PREFERRED_BASE_URL, baseUrl.trim());
        }
        editor.apply();
    }
}
