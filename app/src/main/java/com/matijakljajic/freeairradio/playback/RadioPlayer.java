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
        ContextCompat.startForegroundService(
                appContext,
                PlaybackSessionContract.createPlayIntent(appContext, station)
        );
    }

    public void stop() {
        appContext.startService(PlaybackSessionContract.createStopIntent(appContext));
    }
}
