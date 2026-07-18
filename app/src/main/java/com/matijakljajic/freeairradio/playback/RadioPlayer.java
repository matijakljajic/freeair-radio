package com.matijakljajic.freeairradio.playback;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.matijakljajic.freeairradio.data.model.Station;

public final class RadioPlayer {

    @NonNull
    private final Context appContext;

    public RadioPlayer(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    public void play(@NonNull Station station) {
        startService(PlaybackSessionContract.createPlayIntent(appContext, station), true);
    }

    public void stop() {
        startService(PlaybackSessionContract.createStopIntent(appContext), false);
    }

    public void resume() {
        startService(PlaybackSessionContract.createResumeIntent(appContext), false);
    }

    private void startService(@NonNull android.content.Intent intent, boolean foreground) {
        if (foreground) {
            ContextCompat.startForegroundService(appContext, intent);
            return;
        }
        appContext.startService(intent);
    }
}
