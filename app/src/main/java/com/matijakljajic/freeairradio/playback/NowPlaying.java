package com.matijakljajic.freeairradio.playback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public final class NowPlaying {

    @Nullable
    private final String artist;
    @Nullable
    private final String title;

    public NowPlaying(@Nullable String artist, @Nullable String title) {
        this.artist = artist;
        this.title = title;
    }

    @Nullable
    public String getArtist() {
        return artist;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String buildDisplayText() {
        if (title != null) {
            return artist == null ? title : artist + " - " + title;
        }
        return artist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NowPlaying)) {
            return false;
        }
        NowPlaying that = (NowPlaying) o;
        return Objects.equals(artist, that.artist)
                && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artist, title);
    }

    @NonNull
    @Override
    public String toString() {
        return "NowPlaying{"
                + "artist='" + artist + '\''
                + ", title='" + title + '\''
                + '}';
    }
}
