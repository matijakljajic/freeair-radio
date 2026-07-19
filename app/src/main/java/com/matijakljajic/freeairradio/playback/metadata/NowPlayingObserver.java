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
import com.matijakljajic.freeairradio.util.AppLog;

import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("unused")
@UnstableApi
public final class NowPlayingObserver implements Player.Listener {

    private static final String TAG = "NowPlayingObserver";

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

    private void emitParsedNowPlaying(@Nullable ParsedNowPlaying parsedNowPlaying, long generationSnapshot) {
        if (parsedNowPlaying != null && parsedNowPlaying.nowPlaying != null) {
            emitIfChanged(parsedNowPlaying.nowPlaying, generationSnapshot, parsedNowPlaying.source);
        }
    }

    private void emitIfChanged(@NonNull NowPlaying nowPlaying,
                               long generationSnapshot,
                               @NonNull String source) {
        if (generationSnapshot != playbackGeneration || station == null) {
            AppLog.d(TAG, "Ignoring stale now playing"
                    + " station=" + getObservedStationLogName()
                    + " source=" + source
                    + " artist=" + AppLog.value(nowPlaying.getArtist())
                    + " title=" + AppLog.value(nowPlaying.getTitle()));
            return;
        }
        if (Objects.equals(lastEmitted, nowPlaying)) {
            AppLog.d(TAG, "Ignoring duplicate now playing"
                    + " station=" + getObservedStationLogName()
                    + " source=" + source
                    + " artist=" + AppLog.value(nowPlaying.getArtist())
                    + " title=" + AppLog.value(nowPlaying.getTitle()));
            return;
        }

        NowPlaying previousNowPlaying = lastEmitted;
        lastEmitted = nowPlaying;
        AppLog.d(TAG, "Emitting now playing"
                + " station=" + getObservedStationLogName()
                + " source=" + source
                + " artist=" + AppLog.value(nowPlaying.getArtist())
                + " title=" + AppLog.value(nowPlaying.getTitle()));
        listener.onNowPlayingChanged(station, previousNowPlaying, nowPlaying);
    }

    @Nullable
    private ParsedNowPlaying parseNowPlaying(@NonNull Metadata metadata) {
        if (station == null) {
            return null;
        }

        ParsedMetadata parsedMetadata = new ParsedMetadata();

        for (int i = 0; i < metadata.length(); i++) {
            consumeMetadataEntry(metadata.get(i), parsedMetadata);
        }

        return new ParsedNowPlaying(
                NowPlaying.fromMetadata(
                        station.getName(),
                        parsedMetadata.artist,
                        firstNonNull(parsedMetadata.icyTitle, parsedMetadata.title)
                ),
                parsedMetadata.resolveSource()
        );
    }

    @Nullable
    private ParsedNowPlaying parseNowPlaying(@NonNull MediaMetadata mediaMetadata) {
        if (station == null) {
            return null;
        }

        return new ParsedNowPlaying(
                NowPlaying.fromMetadata(
                        station.getName(),
                        charSequenceToString(mediaMetadata.artist),
                        charSequenceToString(mediaMetadata.title)
                ),
                "MEDIA_METADATA"
        );
    }

    private static void consumeMetadataEntry(@NonNull Metadata.Entry entry,
                                             @NonNull ParsedMetadata parsedMetadata) {
        if (entry instanceof IcyInfo) {
            parsedMetadata.sawIcy = true;
            parsedMetadata.icyTitle = normalizeNullable(((IcyInfo) entry).title);
            return;
        }

        if (!(entry instanceof TextInformationFrame)) {
            return;
        }

        TextInformationFrame frame = (TextInformationFrame) entry;
        parsedMetadata.sawId3 = true;
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
        private boolean sawIcy;
        private boolean sawId3;

        @NonNull
        private String resolveSource() {
            if (icyTitle != null || sawIcy) {
                return "ICY";
            }
            if (artist != null || title != null || sawId3) {
                return "ID3";
            }
            return "UNKNOWN";
        }
    }

    private static final class ParsedNowPlaying {
        @Nullable
        private final NowPlaying nowPlaying;
        @NonNull
        private final String source;

        private ParsedNowPlaying(@Nullable NowPlaying nowPlaying, @NonNull String source) {
            this.nowPlaying = nowPlaying;
            this.source = source;
        }
    }

    @NonNull
    private String getObservedStationLogName() {
        return AppLog.stationName(station != null ? station.getName() : null);
    }
}
