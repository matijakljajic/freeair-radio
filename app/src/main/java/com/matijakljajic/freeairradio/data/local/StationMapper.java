package com.matijakljajic.freeairradio.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.local.entity.FavoriteStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.LocalStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.RecentlyPlayedStationEntity;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StationMapper {

    private StationMapper() {
    }

    @NonNull
    public static FavoriteStationEntity toFavoriteStationEntity(@NonNull Station station,
                                                                @Nullable FavoriteStationEntity existingEntity,
                                                                long now) {
        long addedAt = existingEntity != null ? existingEntity.addedAt : now;
        return new FavoriteStationEntity(
                station.getId(),
                station.getName(),
                station.getStreamUrl(),
                station.getResolvedStreamUrl(),
                station.getHomepage(),
                station.getFavicon(),
                station.getCountry(),
                station.getCountryCode(),
                station.getLanguage(),
                station.getTags(),
                station.getCodec(),
                station.getBitrate(),
                station.getHls(),
                station.getOrigin().name(),
                addedAt,
                now
        );
    }

    @NonNull
    public static LocalStationEntity toLocalStationEntity(@NonNull Station station,
                                                          @Nullable LocalStationEntity existingEntity,
                                                          long now) {
        long createdAt = existingEntity != null ? existingEntity.createdAt : now;
        return new LocalStationEntity(
                station.getId(),
                station.getName(),
                station.getStreamUrl(),
                station.getResolvedStreamUrl(),
                station.getHomepage(),
                station.getFavicon(),
                station.getCountry(),
                station.getCountryCode(),
                station.getLanguage(),
                station.getTags(),
                station.getCodec(),
                station.getBitrate(),
                station.getHls(),
                station.getOrigin().name(),
                createdAt,
                now
        );
    }

    @NonNull
    public static RecentlyPlayedStationEntity toRecentlyPlayedStationEntity(@NonNull Station station,
                                                                            long now) {
        return new RecentlyPlayedStationEntity(
                station.getId(),
                station.getName(),
                station.getStreamUrl(),
                station.getResolvedStreamUrl(),
                station.getHomepage(),
                station.getFavicon(),
                station.getCountry(),
                station.getCountryCode(),
                station.getLanguage(),
                station.getTags(),
                station.getCodec(),
                station.getBitrate(),
                station.getHls(),
                station.getOrigin().name(),
                now
        );
    }

    @NonNull
    public static Station toStation(@NonNull FavoriteStationEntity entity) {
        return buildStation(
                entity.id,
                entity.name,
                entity.streamUrl,
                entity.resolvedStreamUrl,
                entity.homepage,
                entity.favicon,
                entity.country,
                entity.countryCode,
                entity.language,
                entity.tags,
                entity.codec,
                entity.bitrate,
                entity.hls,
                entity.origin
        );
    }

    @NonNull
    public static Station toStation(@NonNull LocalStationEntity entity) {
        return buildStation(
                entity.id,
                entity.name,
                entity.streamUrl,
                entity.resolvedStreamUrl,
                entity.homepage,
                entity.favicon,
                entity.country,
                entity.countryCode,
                entity.language,
                entity.tags,
                entity.codec,
                entity.bitrate,
                entity.hls,
                entity.origin
        );
    }

    @NonNull
    public static Station toStation(@NonNull RecentlyPlayedStationEntity entity) {
        return buildStation(
                entity.id,
                entity.name,
                entity.streamUrl,
                entity.resolvedStreamUrl,
                entity.homepage,
                entity.favicon,
                entity.country,
                entity.countryCode,
                entity.language,
                entity.tags,
                entity.codec,
                entity.bitrate,
                entity.hls,
                entity.origin
        );
    }

    @NonNull
    public static List<Station> toFavoriteStations(@NonNull List<FavoriteStationEntity> entities) {
        List<Station> stations = new ArrayList<>(entities.size());
        for (FavoriteStationEntity entity : entities) {
            stations.add(toStation(entity));
        }
        return stations;
    }

    @NonNull
    public static List<Station> toLocalStations(@NonNull List<LocalStationEntity> entities) {
        List<Station> stations = new ArrayList<>(entities.size());
        for (LocalStationEntity entity : entities) {
            stations.add(toStation(entity));
        }
        return stations;
    }

    @NonNull
    public static List<Station> toRecentlyPlayedStations(@NonNull List<RecentlyPlayedStationEntity> entities) {
        List<Station> stations = new ArrayList<>(entities.size());
        for (RecentlyPlayedStationEntity entity : entities) {
            stations.add(toStation(entity));
        }
        return stations;
    }

    @NonNull
    private static Station buildStation(@NonNull String id,
                                        @NonNull String name,
                                        @NonNull String streamUrl,
                                        @Nullable String resolvedStreamUrl,
                                        @Nullable String homepage,
                                        @Nullable String favicon,
                                        @Nullable String country,
                                        @Nullable String countryCode,
                                        @Nullable String language,
                                        @Nullable String tags,
                                        @Nullable String codec,
                                        int bitrate,
                                        @Nullable Boolean hls,
                                        @Nullable String originName) {
        return Station.builder(id, name, streamUrl, parseOrigin(id, originName))
                .setResolvedStreamUrl(resolvedStreamUrl)
                .setHomepage(homepage)
                .setFavicon(favicon)
                .setCountry(country)
                .setCountryCode(countryCode)
                .setLanguage(language)
                .setTags(tags)
                .setCodec(codec)
                .setBitrate(bitrate)
                .setHls(hls)
                .build();
    }

    @NonNull
    private static StationOrigin parseOrigin(@NonNull String stationId,
                                             @Nullable String originName) {
        if (originName != null) {
            try {
                return StationOrigin.valueOf(originName.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fall back to the station-id prefix when old or malformed data is read.
            }
        }

        return stationId.startsWith("LOCAL:")
                ? StationOrigin.LOCAL_USER
                : StationOrigin.RADIO_BROWSER;
    }
}
