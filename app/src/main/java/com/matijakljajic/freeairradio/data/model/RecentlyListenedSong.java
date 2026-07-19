package com.matijakljajic.freeairradio.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public final class RecentlyListenedSong {

    @Nullable
    private final String artist;
    @Nullable
    private final String title;
    private final long heardAt;

    public RecentlyListenedSong(@Nullable String artist,
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

    public boolean hasSameTrackInfo(@Nullable RecentlyListenedSong other) {
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
        if (!(object instanceof RecentlyListenedSong)) {
            return false;
        }
        RecentlyListenedSong song = (RecentlyListenedSong) object;
        return heardAt == song.heardAt
                && Objects.equals(artist, song.artist)
                && Objects.equals(title, song.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artist, title, heardAt);
    }

    @NonNull
    @Override
    public String toString() {
        return "RecentlyListenedSong{"
                + "artist='" + artist + '\''
                + ", title='" + title + '\''
                + ", heardAt=" + heardAt
                + '}';
    }
}
