package com.matijakljajic.freeairradio.playback;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.HeartRating;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaConstants;
import androidx.media3.session.MediaLibraryService.LibraryParams;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.artwork.StationArtworkBitmapLoader;
import com.matijakljajic.freeairradio.artwork.StationArtworkResolver;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.playback.metadata.PlaybackMetadataMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class RadioPlaybackLibraryCatalog {

    static final String BROWSE_ROOT_ID = "browse_root";
    static final String BROWSE_TOP_ID = "browse_top";
    static final String BROWSE_FAVORITES_ID = "browse_favorites";
    static final String BROWSE_RECENT_ID = "browse_recent";
    static final String BROWSE_LOCAL_ID = "browse_local";
    private static final String BROWSE_STATION_ID_PREFIX = "browse_station:";

    @NonNull
    private final Map<String, Station> browseStationsByMediaId = new ConcurrentHashMap<>();

    @Nullable
    MediaItem buildBrowseNodeItem(@NonNull Context context, @NonNull String mediaId) {
        if (BROWSE_ROOT_ID.equals(mediaId)) {
            return buildBrowsableMediaItem(
                    BROWSE_ROOT_ID,
                    context.getString(R.string.app_name),
                    MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
            );
        }
        if (BROWSE_TOP_ID.equals(mediaId)) {
            return buildBrowsableMediaItem(
                    BROWSE_TOP_ID,
                    context.getString(R.string.station_list_title_now_popular),
                    MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS
            );
        }
        if (BROWSE_FAVORITES_ID.equals(mediaId)) {
            return buildBrowsableMediaItem(
                    BROWSE_FAVORITES_ID,
                    context.getString(R.string.station_list_title_favorites),
                    MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS
            );
        }
        if (BROWSE_RECENT_ID.equals(mediaId)) {
            return buildBrowsableMediaItem(
                    BROWSE_RECENT_ID,
                    context.getString(R.string.station_list_title_recently_played),
                    MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS
            );
        }
        if (BROWSE_LOCAL_ID.equals(mediaId)) {
            return buildBrowsableMediaItem(
                    BROWSE_LOCAL_ID,
                    context.getString(R.string.station_list_title_local_stations),
                    MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS
            );
        }
        return null;
    }

    @NonNull
    List<MediaItem> buildRootChildren(@NonNull Context context) {
        List<MediaItem> items = new ArrayList<>(3);
        items.add(requireBrowseNodeItem(context, BROWSE_TOP_ID));
        items.add(requireBrowseNodeItem(context, BROWSE_FAVORITES_ID));
        items.add(requireBrowseNodeItem(context, BROWSE_RECENT_ID));
        return items;
    }

    @NonNull
    MediaItem buildPlayableStationMediaItem(@NonNull Station station, boolean favorite) {
        return buildStationLibraryMediaItem(
                station,
                buildPlayableStationLibraryMetadata(station, favorite)
        );
    }

    @NonNull
    MediaMetadata buildPlaybackStationMetadata(@NonNull Station station, boolean favorite) {
        return buildBaseMediaMetadata(station, favorite);
    }

    @NonNull
    MediaItem buildResumptionPreviewMediaItem(@NonNull Station station, boolean favorite) {
        return buildStationLibraryMediaItem(
                station,
                buildResumptionPreviewMetadata(station, favorite)
        );
    }

    @NonNull
    LibraryParams buildRootLibraryParams(@Nullable LibraryParams requestedParams) {
        Bundle extras = copyLibraryParamsExtras(requestedParams);
        extras.putBoolean(
                androidx.media3.session.legacy.MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED,
                true
        );
        extras.putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
        );
        extras.putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
        );
        return buildLibraryParams(extras, requestedParams, false);
    }

    @NonNull
    LibraryParams buildRecentLibraryParams(@Nullable LibraryParams requestedParams) {
        return buildLibraryParams(copyLibraryParamsExtras(requestedParams), requestedParams, true);
    }

    @NonNull
    String getLibraryRootMediaId(@Nullable LibraryParams params) {
        if (params == null) {
            return BROWSE_ROOT_ID;
        }
        if (params.isRecent) {
            return BROWSE_RECENT_ID;
        }
        if (params.isSuggested) {
            return BROWSE_TOP_ID;
        }
        return BROWSE_ROOT_ID;
    }

    @Nullable
    Station resolveRequestedStation(@NonNull MediaItem mediaItem) {
        if (mediaItem.mediaId == null || mediaItem.mediaId.isEmpty()) {
            return null;
        }
        return resolveRequestedStation(mediaItem.mediaId);
    }

    @Nullable
    Station resolveRequestedStation(@NonNull String mediaId) {
        return browseStationsByMediaId.get(mediaId);
    }

    @NonNull
    private MediaItem requireBrowseNodeItem(@NonNull Context context, @NonNull String mediaId) {
        MediaItem item = buildBrowseNodeItem(context, mediaId);
        if (item == null) {
            throw new IllegalArgumentException("Unknown browse node: " + mediaId);
        }
        return item;
    }

    @NonNull
    private MediaItem buildBrowsableMediaItem(@NonNull String mediaId,
                                              @NonNull String title,
                                              @MediaMetadata.MediaType int mediaType) {
        Bundle extras = new Bundle();
        extras.putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
        );
        extras.putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
        );
        return new MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(mediaType)
                        .setExtras(extras)
                        .build())
                .build();
    }

    @NonNull
    private MediaMetadata buildPlayableStationLibraryMetadata(@NonNull Station station, boolean favorite) {
        MediaMetadata.Builder builder = newStationMetadataBuilder(station.getName())
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION);
        if (isKnownValue(station.getCountryName())) {
            builder.setArtist(station.getCountryName());
        }
        applyStationArtwork(builder, station);
        if (favorite) {
            builder.setUserRating(new HeartRating(true));
        }
        return builder.build();
    }

    @NonNull
    private MediaMetadata buildResumptionPreviewMetadata(@NonNull Station station, boolean favorite) {
        MediaMetadata.Builder builder = buildBaseMediaMetadata(station, favorite).buildUpon();
        String artworkUrl = StationArtworkResolver.getBestAvailableUrl(station);
        byte[] artworkData = StationArtworkBitmapLoader.getCachedArtworkData(artworkUrl);
        if (artworkData != null) {
            builder.setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER);
        }
        return builder.build();
    }

    @NonNull
    private MediaMetadata buildBaseMediaMetadata(@NonNull Station station, boolean favorite) {
        MediaMetadata.Builder builder = newStationMetadataBuilder(station.getName())
                .setStation(station.getName())
                .setUserRating(new HeartRating(favorite));
        applyStationArtwork(builder, station);
        return builder.build();
    }

    @NonNull
    private MediaItem buildStationLibraryMediaItem(@NonNull Station station,
                                                   @NonNull MediaMetadata metadata) {
        String browseMediaId = registerBrowseMediaId(station);
        return new MediaItem.Builder()
                .setMediaId(browseMediaId)
                .setUri(Uri.parse(station.getPlayableStreamUrl()))
                .setMediaMetadata(metadata)
                .build();
    }

    @NonNull
    private String registerBrowseMediaId(@NonNull Station station) {
        String browseMediaId = BROWSE_STATION_ID_PREFIX + station.getId();
        browseStationsByMediaId.put(browseMediaId, station);
        return browseMediaId;
    }

    @NonNull
    private static MediaMetadata.Builder newStationMetadataBuilder(@NonNull String stationName) {
        return new MediaMetadata.Builder()
                .setTitle(stationName)
                .setDisplayTitle(stationName)
                .setDescription(stationName);
    }

    private static void applyStationArtwork(@NonNull MediaMetadata.Builder builder,
                                            @NonNull Station station) {
        Uri artworkUri = PlaybackMetadataMapper.resolveStationArtworkUri(station);
        if (artworkUri != null) {
            builder.setArtworkUri(artworkUri);
        }
    }

    @NonNull
    private static LibraryParams buildLibraryParams(@NonNull Bundle extras,
                                                    @Nullable LibraryParams requestedParams,
                                                    boolean forceRecent) {
        return new LibraryParams.Builder()
                .setExtras(extras)
                .setRecent(forceRecent || (requestedParams != null && requestedParams.isRecent))
                .setOffline(requestedParams != null && requestedParams.isOffline)
                .setSuggested(requestedParams != null && requestedParams.isSuggested)
                .build();
    }

    @NonNull
    private static Bundle copyLibraryParamsExtras(@Nullable LibraryParams requestedParams) {
        return requestedParams == null
                ? new Bundle()
                : new Bundle(requestedParams.extras);
    }

    private static boolean isKnownValue(@Nullable String value) {
        return value != null && !Station.UNKNOWN.equals(value);
    }
}
