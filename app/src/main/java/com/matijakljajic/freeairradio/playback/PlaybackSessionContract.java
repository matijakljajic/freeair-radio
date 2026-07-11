package com.matijakljajic.freeairradio.playback;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.matijakljajic.freeairradio.data.model.Station;

public final class PlaybackSessionContract {

    public static final String ACTION_PLAY_STATION = "com.matijakljajic.freeairradio.action.PLAY_STATION";
    public static final String ACTION_STOP_PLAYBACK = "com.matijakljajic.freeairradio.action.STOP_PLAYBACK";
    public static final String EXTRA_STATION = "com.matijakljajic.freeairradio.extra.STATION";

    private PlaybackSessionContract() {
    }

    @NonNull
    public static Intent createPlayIntent(@NonNull Context context, @NonNull Station station) {
        return new Intent(context, RadioPlaybackService.class)
                .setAction(ACTION_PLAY_STATION)
                .putExtra(EXTRA_STATION, station);
    }

    @NonNull
    public static Intent createStopIntent(@NonNull Context context) {
        return new Intent(context, RadioPlaybackService.class)
                .setAction(ACTION_STOP_PLAYBACK);
    }
}
