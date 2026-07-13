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

        String stationName = station != null
                ? station.getName()
                : charSequenceToString(rawMetadata.station);
        if (stationName != null) {
            builder.setStation(stationName);
        }
        byte[] cachedArtworkData = station != null
                ? StationArtworkBitmapLoader.getCachedArtworkData(StationArtworkResolver.getBestAvailableUrl(station))
                : rawMetadata.artworkData;
        if (cachedArtworkData != null) {
            builder.setArtworkData(cachedArtworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER);
        }
        Uri artworkUri = station != null
                ? resolveStationArtworkUri(station)
                : rawMetadata.artworkUri;
        if (artworkUri == null) {
            artworkUri = rawMetadata.artworkUri;
        }
        if (artworkUri != null) {
            builder.setArtworkUri(artworkUri);
        }
        if (station != null) {
            builder.setUserRating(new HeartRating(favorite));
        } else if (rawMetadata.userRating != null) {
            builder.setUserRating(rawMetadata.userRating);
        }

        NowPlaying nowPlaying = currentNowPlaying != null
                ? currentNowPlaying
                : extractNowPlaying(rawMetadata, stationName);
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
    public static Uri resolveStationArtworkUri(@NonNull Station station) {
        String url = StationArtworkResolver.getBestAvailableUrl(station);
        if (StationArtworkResolver.isSvgUrl(url)) {
            url = StationArtworkResolver.getBestRasterUrl(station);
        }
        return url == null ? null : Uri.parse(url);
    }

    @Nullable
    private static NowPlaying extractNowPlaying(@NonNull MediaMetadata rawMetadata,
                                                @Nullable String stationName) {
        return NowPlaying.fromMetadata(
                stationName,
                charSequenceToString(rawMetadata.artist),
                charSequenceToString(rawMetadata.title)
        );
    }

    @Nullable
    private static String charSequenceToString(@Nullable CharSequence value) {
        return value == null ? null : value.toString();
    }
}
