package com.matijakljajic.freeairradio.playback.metadata;

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

@SuppressWarnings("unused")
@UnstableApi
public final class NowPlayingObserver implements Player.Listener {

    public interface Listener {
        void onNowPlayingChanged(@NonNull Station station,
                                 @Nullable NowPlaying previousNowPlaying,
                                 @Nullable NowPlaying currentNowPlaying);
    }

    @NonNull
    private final Listener listener;
    @Nullable
    private Player observedPlayer;
    @Nullable
    private Station station;
    @Nullable
    private NowPlaying lastEmitted;
    private long playbackGeneration;

    public NowPlayingObserver(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void clearNowPlaying(long generationSnapshot) {
        if (generationSnapshot != playbackGeneration) {
            return;
        }

        NowPlaying previousNowPlaying = lastEmitted;
        lastEmitted = null;
        if (station != null && previousNowPlaying != null) {
            listener.onNowPlayingChanged(station, previousNowPlaying, null);
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
        emitParsedNowPlaying(parseNowPlaying(metadata), playbackGeneration);
    }

    @Override
    public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
        emitParsedNowPlaying(parseNowPlaying(mediaMetadata), playbackGeneration);
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

        NowPlaying previousNowPlaying = lastEmitted;
        lastEmitted = nowPlaying;
        listener.onNowPlayingChanged(station, previousNowPlaying, nowPlaying);
    }

    @Nullable
    private NowPlaying parseNowPlaying(@NonNull Metadata metadata) {
        if (station == null) {
            return null;
        }

        ParsedMetadata parsedMetadata = new ParsedMetadata();

        for (int i = 0; i < metadata.length(); i++) {
            consumeMetadataEntry(metadata.get(i), parsedMetadata);
        }

        return NowPlaying.fromMetadata(
                station.getName(),
                parsedMetadata.artist,
                firstNonNull(parsedMetadata.icyTitle, parsedMetadata.title)
        );
    }

    @Nullable
    private NowPlaying parseNowPlaying(@NonNull MediaMetadata mediaMetadata) {
        if (station == null) {
            return null;
        }

        return NowPlaying.fromMetadata(
                station.getName(),
                charSequenceToString(mediaMetadata.artist),
                charSequenceToString(mediaMetadata.title)
        );
    }

    private static void consumeMetadataEntry(@NonNull Metadata.Entry entry,
                                             @NonNull ParsedMetadata parsedMetadata) {
        if (entry instanceof IcyInfo) {
            parsedMetadata.icyTitle = normalizeNullable(((IcyInfo) entry).title);
            return;
        }

        if (!(entry instanceof TextInformationFrame)) {
            return;
        }

        TextInformationFrame frame = (TextInformationFrame) entry;
        String frameValue = getFirstFrameValue(frame);
        if (frameValue == null) {
            return;
        }

        if (isArtistFrame(frame.id)) {
            parsedMetadata.artist = firstNonNull(parsedMetadata.artist, frameValue);
        } else if (isTitleFrame(frame.id)) {
            parsedMetadata.title = firstNonNull(parsedMetadata.title, frameValue);
        }
    }

    @Nullable
    private static String getFirstFrameValue(@NonNull TextInformationFrame frame) {
        if (frame.values.isEmpty()) {
            return null;
        }
        return normalizeNullable(frame.values.get(0));
    }

    @Nullable
    private static String firstNonNull(@Nullable String first, @Nullable String second) {
        return first != null ? first : second;
    }

    @Nullable
    private static String charSequenceToString(@Nullable CharSequence value) {
        return value == null ? null : value.toString();
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

    private static final class ParsedMetadata {
        @Nullable
        private String icyTitle;
        @Nullable
        private String artist;
        @Nullable
        private String title;
    }
}
