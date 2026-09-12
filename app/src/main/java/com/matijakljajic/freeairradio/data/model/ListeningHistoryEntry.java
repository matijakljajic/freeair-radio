package com.matijakljajic.freeairradio.data.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ListeningHistoryEntry {

    @NonNull
    private final Station station;
    private final long listenedAt;
    @NonNull
    private final List<ListeningHistoryTrack> tracks;

    public ListeningHistoryEntry(@NonNull Station station,
                                   long listenedAt,
                                   @NonNull List<ListeningHistoryTrack> tracks) {
        this.station = Objects.requireNonNull(station, "station");
        this.listenedAt = listenedAt;
        this.tracks = List.copyOf(new ArrayList<>(tracks));
    }

    @NonNull
    public Station getStation() {
        return station;
    }

    public long getListenedAt() {
        return listenedAt;
    }

    @NonNull
    public List<ListeningHistoryTrack> getTracks() {
        return tracks;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ListeningHistoryEntry)) {
            return false;
        }
        ListeningHistoryEntry that = (ListeningHistoryEntry) object;
        return listenedAt == that.listenedAt
                && station.equals(that.station)
                && tracks.equals(that.tracks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(station, listenedAt, tracks);
    }

    @NonNull
    @Override
    public String toString() {
        return "ListeningHistoryEntry{"
                + "station=" + station
                + ", listenedAt=" + listenedAt
                + ", tracks=" + tracks
                + '}';
    }
}
