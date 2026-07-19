package com.matijakljajic.freeairradio.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.local.entity.FavoriteStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.LocalStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.RecentlyListenedSongEntity;
import com.matijakljajic.freeairradio.data.local.entity.RecentlyPlayedStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.StationSnapshotFields;
import com.matijakljajic.freeairradio.data.model.RecentlyListenedSong;
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
                                                                long displayOrder,
                                                                long now) {
        long addedAt = existingEntity != null ? existingEntity.addedAt : now;
        return new FavoriteStationEntity(
                station.getId(),
                toStationSnapshotFields(station),
                displayOrder,
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
                toStationSnapshotFields(station),
                createdAt,
                now
        );
    }

    @NonNull
    public static RecentlyPlayedStationEntity toRecentlyPlayedStationEntity(@NonNull Station station,
                                                                            long now) {
        return new RecentlyPlayedStationEntity(
                station.getId(),
                toStationSnapshotFields(station),
                now
        );
    }

    @NonNull
    public static RecentlyListenedSongEntity toRecentlyListenedSongEntity(@NonNull String stationId,
                                                                          @NonNull RecentlyListenedSong song) {
        return new RecentlyListenedSongEntity(
                0L,
                stationId,
                song.getArtist(),
                song.getTitle(),
                song.getHeardAt()
        );
    }

    @NonNull
    public static Station toStation(@NonNull FavoriteStationEntity entity) {
        return buildStation(
                entity.id,
                entity.station
        );
    }

    @NonNull
    public static Station toStation(@NonNull LocalStationEntity entity) {
        return buildStation(
                entity.id,
                entity.station
        );
    }

    @NonNull
    public static Station toStation(@NonNull RecentlyPlayedStationEntity entity) {
        return buildStation(
                entity.id,
                entity.station
        );
    }

    @NonNull
    public static RecentlyListenedSong toRecentlyListenedSong(@NonNull RecentlyListenedSongEntity entity) {
        return new RecentlyListenedSong(entity.artist, entity.title, entity.heardAt);
    }

    @NonNull
    public static List<Station> toFavoriteStations(@NonNull List<FavoriteStationEntity> entities) {
        return toStations(entities, StationMapper::toStation);
    }

    @NonNull
    public static List<Station> toLocalStations(@NonNull List<LocalStationEntity> entities) {
        return toStations(entities, StationMapper::toStation);
    }

    @NonNull
    public static List<Station> toRecentlyPlayedStations(@NonNull List<RecentlyPlayedStationEntity> entities) {
        return toStations(entities, StationMapper::toStation);
    }

    @NonNull
    private static <T> List<Station> toStations(@NonNull List<T> entities,
                                                @NonNull EntityStationMapper<T> mapper) {
        List<Station> stations = new ArrayList<>(entities.size());
        for (T entity : entities) {
            stations.add(mapper.map(entity));
        }
        return stations;
    }

    @NonNull
    private static Station buildStation(@NonNull String id,
                                        @NonNull StationSnapshotFields station) {
        return Station.builder(id, station.name, station.streamUrl, parseOrigin(id, station.origin))
                .setResolvedStreamUrl(station.resolvedStreamUrl)
                .setHomepage(station.homepage)
                .setFavicon(station.favicon)
                .setCountry(station.country)
                .setCountryCode(station.countryCode)
                .setLanguage(station.language)
                .setTags(station.tags)
                .setCodec(station.codec)
                .setBitrate(station.bitrate)
                .setHls(station.hls)
                .build();
    }

    @NonNull
    private static StationSnapshotFields toStationSnapshotFields(@NonNull Station station) {
        return new StationSnapshotFields(
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
                station.getOrigin().name()
        );
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

    private interface EntityStationMapper<T> {
        @NonNull
        Station map(@NonNull T entity);
    }
}
