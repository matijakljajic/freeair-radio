package com.matijakljajic.freeairradio.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.local.entity.FavoriteStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.LocalStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.ListeningHistoryTrackEntity;
import com.matijakljajic.freeairradio.data.local.entity.ListeningHistoryStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.StationSnapshotFields;
import com.matijakljajic.freeairradio.data.model.ListeningHistoryTrack;
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
    public static ListeningHistoryStationEntity toListeningHistoryStationEntity(@NonNull Station station,
                                                                            long lastPlayedAt) {
        return new ListeningHistoryStationEntity(
                station.getId(),
                toStationSnapshotFields(station),
                lastPlayedAt
        );
    }

    @NonNull
    public static ListeningHistoryTrackEntity toListeningHistoryTrackEntity(@NonNull String stationId,
                                                                          @NonNull ListeningHistoryTrack track) {
        return new ListeningHistoryTrackEntity(
                0L,
                stationId,
                track.getArtist(),
                track.getTitle(),
                track.getHeardAt()
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
    public static Station toStation(@NonNull ListeningHistoryStationEntity entity) {
        return buildStation(
                entity.id,
                entity.station
        );
    }

    @NonNull
    public static ListeningHistoryTrack toListeningHistoryTrack(@NonNull ListeningHistoryTrackEntity entity) {
        return new ListeningHistoryTrack(entity.artist, entity.title, entity.heardAt);
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
    public static List<Station> toListeningHistoryStations(@NonNull List<ListeningHistoryStationEntity> entities) {
        List<Station> stations = new ArrayList<>(entities.size());
        for (ListeningHistoryStationEntity entity : entities) {
            stations.add(toStation(entity));
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
                .setCountryName(station.country)
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
                station.getCountryName(),
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
}
