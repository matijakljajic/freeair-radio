package com.matijakljajic.freeairradio.playback.metadata;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.HeartRating;
import androidx.media3.common.MediaMetadata;

import com.matijakljajic.freeairradio.artwork.StationArtworkBitmapLoader;
import com.matijakljajic.freeairradio.artwork.StationArtworkResolver;
import com.matijakljajic.freeairradio.data.model.Station;

public final class PlaybackMetadataMapper {

    private PlaybackMetadataMapper() {
    }

    @NonNull
    public static MediaMetadata buildPresentedMediaMetadata(@NonNull MediaMetadata rawMetadata,
                                                            @Nullable Station station,
                                                            @Nullable NowPlaying currentNowPlaying,
                                                            boolean favorite) {
        MediaMetadata.Builder builder = new MediaMetadata.Builder();

        String stationName = resolveStationName(rawMetadata, station);
        if (stationName != null) {
            builder.setStation(stationName);
        }
        byte[] cachedArtworkData = resolveArtworkData(rawMetadata, station);
        if (cachedArtworkData != null) {
            builder.setArtworkData(cachedArtworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER);
        }
        Uri artworkUri = resolveArtworkUri(rawMetadata, station);
        if (artworkUri != null) {
            builder.setArtworkUri(artworkUri);
        }
        HeartRating userRating = resolveUserRating(rawMetadata, station, favorite);
        if (userRating != null) {
            builder.setUserRating(userRating);
        }

        NowPlaying nowPlaying = resolvePresentedNowPlaying(rawMetadata, stationName, currentNowPlaying);
        if (nowPlaying != null) {
            if (nowPlaying.getTitle() != null) {
                builder.setTitle(nowPlaying.getTitle());
            }
            if (nowPlaying.getArtist() != null) {
                builder.setArtist(nowPlaying.getArtist());
            }
        }

        return builder.build();
    }

    @Nullable
    public static CharSequence buildNotificationTitle(@NonNull MediaMetadata metadata) {
        NowPlaying nowPlaying = resolvePresentedNowPlaying(metadata, charSequenceToString(metadata.station), null);
        if (hasCompleteTrackInfo(nowPlaying)) {
            return nowPlaying.getTitle();
        }
        return firstNonEmpty(metadata.station, metadata.displayTitle, metadata.artist);
    }

    @Nullable
    public static CharSequence buildNotificationText(@NonNull MediaMetadata metadata) {
        NowPlaying nowPlaying = resolvePresentedNowPlaying(metadata, charSequenceToString(metadata.station), null);
        return hasCompleteTrackInfo(nowPlaying) ? nowPlaying.getArtist() : null;
    }

    @Nullable
    public static Uri resolveStationArtworkUri(@NonNull Station station) {
        String url = StationArtworkResolver.getBestAvailableUrl(station);
        if (StationArtworkResolver.isSvgUrl(url)) {
            url = StationArtworkResolver.getBestRasterUrl(station);
        }
        return url == null ? null : Uri.parse(url);
    }

    @Nullable
    private static NowPlaying resolvePresentedNowPlaying(@NonNull MediaMetadata rawMetadata,
                                                         @Nullable String stationName,
                                                         @Nullable NowPlaying currentNowPlaying) {
        if (currentNowPlaying != null) {
            return currentNowPlaying;
        }
        return NowPlaying.fromMetadata(
                stationName,
                charSequenceToString(rawMetadata.artist),
                charSequenceToString(rawMetadata.title)
        );
    }

    @Nullable
    private static String resolveStationName(@NonNull MediaMetadata rawMetadata,
                                             @Nullable Station station) {
        return station != null ? station.getName() : charSequenceToString(rawMetadata.station);
    }

    @Nullable
    private static byte[] resolveArtworkData(@NonNull MediaMetadata rawMetadata,
                                             @Nullable Station station) {
        if (station == null) {
            return rawMetadata.artworkData;
        }
        return StationArtworkBitmapLoader.getCachedArtworkData(
                StationArtworkResolver.getBestAvailableUrl(station)
        );
    }

    @Nullable
    private static Uri resolveArtworkUri(@NonNull MediaMetadata rawMetadata,
                                         @Nullable Station station) {
        if (station == null) {
            return rawMetadata.artworkUri;
        }

        Uri artworkUri = resolveStationArtworkUri(station);
        return artworkUri != null ? artworkUri : rawMetadata.artworkUri;
    }

    @Nullable
    private static HeartRating resolveUserRating(@NonNull MediaMetadata rawMetadata,
                                                 @Nullable Station station,
                                                 boolean favorite) {
        if (station != null) {
            return new HeartRating(favorite);
        }
        return rawMetadata.userRating instanceof HeartRating
                ? (HeartRating) rawMetadata.userRating
                : null;
    }

    private static boolean hasCompleteTrackInfo(@Nullable NowPlaying nowPlaying) {
        return nowPlaying != null
                && nowPlaying.getTitle() != null
                && nowPlaying.getArtist() != null;
    }

    @Nullable
    private static CharSequence firstNonEmpty(@Nullable CharSequence... values) {
        for (CharSequence value : values) {
            if (value != null && value.length() > 0) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    private static String charSequenceToString(@Nullable CharSequence value) {
        return value == null ? null : value.toString();
    }
}
