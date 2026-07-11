package com.matijakljajic.freeairradio.playback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.extractor.metadata.icy.IcyInfo;
import androidx.media3.extractor.metadata.id3.TextInformationFrame;

import com.matijakljajic.freeairradio.data.model.Station;

import java.util.Locale;
import java.util.Objects;

@UnstableApi
public final class NowPlayingObserver implements Player.Listener {

    private static final NowPlayingObserver INSTANCE = new NowPlayingObserver();

    @NonNull
    private final CurrentPlaybackState currentPlaybackState = CurrentPlaybackState.getInstance();
    @Nullable
    private Player observedPlayer;
    @Nullable
    private Station station;
    @Nullable
    private NowPlaying lastEmitted;
    private long playbackGeneration;

    private NowPlayingObserver() {
    }

    @NonNull
    public static NowPlayingObserver getInstance() {
        return INSTANCE;
    }

    public void clearNowPlaying(long generationSnapshot) {
        if (generationSnapshot != playbackGeneration) {
            return;
        }

        lastEmitted = null;
        if (station != null) {
            currentPlaybackState.setCurrentNowPlaying(station, null);
        }
    }

    public void startObserving(@NonNull Player player,
                               @NonNull Station station,
                               long playbackGeneration) {
        stopObserving();
        observedPlayer = player;
        this.station = station;
        this.playbackGeneration = playbackGeneration;
        this.lastEmitted = null;
        player.addListener(this);
        currentPlaybackState.setCurrentStation(station);
    }

    public void stopObserving() {
        if (observedPlayer != null) {
            observedPlayer.removeListener(this);
        }
        observedPlayer = null;
        station = null;
        playbackGeneration = 0L;
        lastEmitted = null;
    }

    @Override
    public void onMetadata(@NonNull Metadata metadata) {
        long generationSnapshot = playbackGeneration;
        emitParsedNowPlaying(parseMetadata(metadata), generationSnapshot);
    }

    @Override
    public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
        long generationSnapshot = playbackGeneration;
        emitParsedNowPlaying(parseMediaMetadata(mediaMetadata), generationSnapshot);
    }

    private void emitParsedNowPlaying(@Nullable NowPlaying nowPlaying, long generationSnapshot) {
        if (nowPlaying != null) {
            emitIfChanged(nowPlaying, generationSnapshot);
        }
    }

    private void emitIfChanged(@NonNull NowPlaying nowPlaying, long generationSnapshot) {
        if (generationSnapshot != playbackGeneration || station == null) {
            return;
        }
        if (Objects.equals(lastEmitted, nowPlaying)) {
            return;
        }
        lastEmitted = nowPlaying;
        currentPlaybackState.setCurrentNowPlaying(station, nowPlaying);
    }

    @Nullable
    private NowPlaying parseMetadata(@NonNull Metadata metadata) {
        if (station == null) {
            return null;
        }

        String icyTitle = null;
        String artist = null;
        String title = null;

        for (int i = 0; i < metadata.length(); i++) {
            Metadata.Entry entry = metadata.get(i);
            if (entry instanceof IcyInfo) {
                IcyInfo icyInfo = (IcyInfo) entry;
                icyTitle = normalizeNullable(icyInfo.title);
            } else if (entry instanceof TextInformationFrame) {
                TextInformationFrame frame = (TextInformationFrame) entry;
                if (isArtistFrame(frame.id)) {
                    artist = pickFirst(artist, normalizeNullable(frame.values.get(0)));
                } else if (isTitleFrame(frame.id)) {
                    title = pickFirst(title, normalizeNullable(frame.values.get(0)));
                }
            }
        }

        ParsedTrack parsedTrack = parseTrackTitle(pickFirst(icyTitle, title));
        if (parsedTrack != null) {
            artist = pickFirst(artist, parsedTrack.artist);
            title = parsedTrack.title != null ? parsedTrack.title : title;
        }

        if (artist == null && title == null) {
            return null;
        }

        return new NowPlaying(artist, title);
    }

    @Nullable
    private NowPlaying parseMediaMetadata(@NonNull MediaMetadata mediaMetadata) {
        if (station == null) {
            return null;
        }

        String artist = normalizeNullable((String) mediaMetadata.artist);
        String title = normalizeNullable((String) mediaMetadata.title);

        ParsedTrack parsedTrack = parseTrackTitle(title);
        if (parsedTrack != null) {
            artist = pickFirst(artist, parsedTrack.artist);
            title = parsedTrack.title != null ? parsedTrack.title : title;
        }

        if (artist == null && title == null) {
            return null;
        }

        if (station.getName().equals(title) && artist == null) {
            return null;
        }

        return new NowPlaying(artist, title);
    }

    @Nullable
    static ParsedTrack parseTrackTitle(@Nullable String rawTitle) {
        String normalizedTitle = normalizeNullable(rawTitle);
        if (normalizedTitle == null) {
            return null;
        }

        String[] separators = {" - ", " – ", " — "};
        for (String separator : separators) {
            int firstSeparator = normalizedTitle.indexOf(separator);
            if (firstSeparator <= 0) {
                continue;
            }
            int secondSeparator = normalizedTitle.indexOf(separator, firstSeparator + separator.length());
            if (secondSeparator >= 0) {
                continue;
            }

            String left = normalizeNullable(normalizedTitle.substring(0, firstSeparator));
            String right = normalizeNullable(normalizedTitle.substring(firstSeparator + separator.length()));
            if (left == null || right == null) {
                continue;
            }

            return new ParsedTrack(left, right, normalizedTitle);
        }

        return new ParsedTrack(null, normalizedTitle, normalizedTitle);
    }

    @Nullable
    private static String pickFirst(@Nullable String first, @Nullable String second) {
        return first != null ? first : second;
    }

    @Nullable
    private static String normalizeNullable(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private static boolean isArtistFrame(@NonNull String id) {
        String normalizedId = id.toUpperCase(Locale.ROOT);
        return "TPE1".equals(normalizedId)
                || "TPE2".equals(normalizedId)
                || "TPE3".equals(normalizedId);
    }

    private static boolean isTitleFrame(@NonNull String id) {
        return "TIT2".equals(id.toUpperCase(Locale.ROOT));
    }

    static final class ParsedTrack {
        @Nullable
        private final String artist;
        @Nullable
        private final String title;
        @NonNull
        private final String rawTitle;

        private ParsedTrack(@Nullable String artist, @Nullable String title, @NonNull String rawTitle) {
            this.artist = artist;
            this.title = title;
            this.rawTitle = rawTitle;
        }

        @Nullable
        String getArtist() {
            return artist;
        }

        @Nullable
        String getTitle() {
            return title;
        }

        @NonNull
        String getRawTitle() {
            return rawTitle;
        }
    }
}
