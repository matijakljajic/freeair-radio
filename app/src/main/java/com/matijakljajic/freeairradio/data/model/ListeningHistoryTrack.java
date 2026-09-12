package com.matijakljajic.freeairradio.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public final class ListeningHistoryTrack {

    @Nullable
    private final String artist;
    @Nullable
    private final String title;
    private final long heardAt;

    public ListeningHistoryTrack(@Nullable String artist,
                                @Nullable String title,
                                long heardAt) {
        this.artist = normalize(artist);
        this.title = normalize(title);
        this.heardAt = heardAt;
    }

    @Nullable
    public String getArtist() {
        return artist;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public long getHeardAt() {
        return heardAt;
    }

    @Nullable
    public String buildDisplayText() {
        if (title == null) {
            return artist;
        }
        return artist == null ? title : title + " – " + artist;
    }

    public boolean hasSameTrackInfo(@Nullable ListeningHistoryTrack other) {
        return other != null
                && Objects.equals(artist, other.artist)
                && Objects.equals(title, other.title);
    }

    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ListeningHistoryTrack)) {
            return false;
        }
        ListeningHistoryTrack track = (ListeningHistoryTrack) object;
        return heardAt == track.heardAt
                && Objects.equals(artist, track.artist)
                && Objects.equals(title, track.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artist, title, heardAt);
    }

    @NonNull
    @Override
    public String toString() {
        return "ListeningHistoryTrack{"
                + "artist='" + artist + '\''
                + ", title='" + title + '\''
                + ", heardAt=" + heardAt
                + '}';
    }
}
