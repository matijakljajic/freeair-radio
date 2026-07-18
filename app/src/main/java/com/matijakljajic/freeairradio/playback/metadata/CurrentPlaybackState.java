package com.matijakljajic.freeairradio.playback.metadata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.model.Station;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CurrentPlaybackState {

    public enum PlaybackStatus {
        IDLE,
        CONNECTING,
        PAUSED,
        PLAYING,
        ERROR
    }

    public interface Listener {
        void onPlaybackStateChanged(@Nullable Station station,
                                    @Nullable NowPlaying nowPlaying,
                                    @NonNull PlaybackStatus playbackStatus);
    }

    private static final CurrentPlaybackState INSTANCE = new CurrentPlaybackState();

    @NonNull
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    @Nullable
    private Station currentStation;
    @Nullable
    private NowPlaying currentNowPlaying;
    @NonNull
    private PlaybackStatus playbackStatus = PlaybackStatus.IDLE;

    private CurrentPlaybackState() {
    }

    @NonNull
    public static CurrentPlaybackState getInstance() {
        return INSTANCE;
    }

    public void addListener(@NonNull Listener listener) {
        listeners.addIfAbsent(listener);
        notifyListener(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    @Nullable
    public Station getCurrentStation() {
        return currentStation;
    }

    @Nullable
    public NowPlaying getCurrentNowPlaying() {
        return currentNowPlaying;
    }

    @NonNull
    public PlaybackStatus getPlaybackStatus() {
        return playbackStatus;
    }

    public void setCurrentStation(@Nullable Station station) {
        PlaybackStatus nextStatus = station == null ? PlaybackStatus.IDLE : PlaybackStatus.CONNECTING;
        if (Objects.equals(currentStation, station)
                && currentNowPlaying == null
                && playbackStatus == nextStatus) {
            return;
        }

        currentStation = station;
        currentNowPlaying = null;
        playbackStatus = nextStatus;
        notifyListeners();
    }

    public void setCurrentNowPlaying(@NonNull Station station, @Nullable NowPlaying nowPlaying) {
        if (!isCurrentStation(station)) {
            return;
        }
        if (Objects.equals(currentNowPlaying, nowPlaying)) {
            return;
        }

        currentNowPlaying = nowPlaying;
        notifyListeners();
    }

    public void setPlaybackStatus(@NonNull Station station, @NonNull PlaybackStatus playbackStatus) {
        if (!isCurrentStation(station)) {
            return;
        }
        if (this.playbackStatus == playbackStatus) {
            return;
        }

        this.playbackStatus = playbackStatus;
        notifyListeners();
    }

    public void clear() {
        if (currentStation == null
                && currentNowPlaying == null
                && playbackStatus == PlaybackStatus.IDLE) {
            return;
        }

        currentStation = null;
        currentNowPlaying = null;
        playbackStatus = PlaybackStatus.IDLE;
        notifyListeners();
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            notifyListener(listener);
        }
    }

    private void notifyListener(@NonNull Listener listener) {
        listener.onPlaybackStateChanged(currentStation, currentNowPlaying, playbackStatus);
    }

    private boolean isCurrentStation(@NonNull Station station) {
        return currentStation != null && currentStation.getId().equals(station.getId());
    }
}
