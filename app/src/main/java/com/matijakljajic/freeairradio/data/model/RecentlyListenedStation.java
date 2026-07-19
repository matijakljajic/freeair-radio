package com.matijakljajic.freeairradio.data.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecentlyListenedStation {

    @NonNull
    private final Station station;
    private final long listenedAt;
    @NonNull
    private final List<RecentlyListenedSong> songs;

    public RecentlyListenedStation(@NonNull Station station,
                                   long listenedAt,
                                   @NonNull List<RecentlyListenedSong> songs) {
        this.station = Objects.requireNonNull(station, "station");
        this.listenedAt = listenedAt;
        this.songs = List.copyOf(new ArrayList<>(songs));
    }

    @NonNull
    public Station getStation() {
        return station;
    }

    public long getListenedAt() {
        return listenedAt;
    }

    @NonNull
    public List<RecentlyListenedSong> getSongs() {
        return songs;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RecentlyListenedStation)) {
            return false;
        }
        RecentlyListenedStation that = (RecentlyListenedStation) object;
        return listenedAt == that.listenedAt
                && station.equals(that.station)
                && songs.equals(that.songs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(station, listenedAt, songs);
    }

    @NonNull
    @Override
    public String toString() {
        return "RecentlyListenedStation{"
                + "station=" + station
                + ", listenedAt=" + listenedAt
                + ", songs=" + songs
                + '}';
    }
}
