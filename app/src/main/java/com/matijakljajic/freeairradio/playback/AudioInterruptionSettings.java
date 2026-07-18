package com.matijakljajic.freeairradio.playback;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class AudioInterruptionSettings {

    private static final String PREFS_NAME = "audio_interruption_settings";
    private static final String KEY_RESPECT_INTERRUPTION_FOCUS = "respect_interruption_focus";

    @NonNull
    private final SharedPreferences sharedPreferences;

    public AudioInterruptionSettings(@NonNull Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean shouldRespectAudioInterruptions() {
        return sharedPreferences.getBoolean(KEY_RESPECT_INTERRUPTION_FOCUS, true);
    }

    public void setRespectAudioInterruptions(boolean respectAudioInterruptions) {
        sharedPreferences.edit()
                .putBoolean(KEY_RESPECT_INTERRUPTION_FOCUS, respectAudioInterruptions)
                .apply();
    }
}
