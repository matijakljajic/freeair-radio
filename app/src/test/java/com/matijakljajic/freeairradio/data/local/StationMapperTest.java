package com.matijakljajic.freeairradio.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.matijakljajic.freeairradio.data.local.entity.FavoriteStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.StationSnapshotFields;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import org.junit.Test;

public class StationMapperTest {

    @Test
    public void toFavoriteStationEntity_preservesAddedAtAndCopiesStationFields() {
        Station station = Station.builder(
                        "RADIO_BROWSER:test-station",
                        "Radio Test",
                        "https://example.com/live",
                        StationOrigin.RADIO_BROWSER
                )
                .setResolvedStreamUrl("https://cdn.example.com/live")
                .setHomepage("https://example.com")
                .setFavicon("https://example.com/icon.png")
                .setCountry("Croatia")
                .setCountryCode("HR")
                .setLanguage("Croatian")
                .setTags("news, talk")
                .setCodec("MP3")
                .setBitrate(192)
                .setHls(Boolean.FALSE)
                .build();
        FavoriteStationEntity existingEntity = new FavoriteStationEntity(
                station.getId(),
                new StationSnapshotFields(
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
                ),
                123L,
                456L,
                789L
        );

        FavoriteStationEntity mappedEntity =
                StationMapper.toFavoriteStationEntity(station, existingEntity, 123L, 999L);

        assertEquals("RADIO_BROWSER:test-station", mappedEntity.id);
        assertEquals("Radio Test", mappedEntity.station.name);
        assertEquals("https://example.com/live", mappedEntity.station.streamUrl);
        assertEquals("https://cdn.example.com/live", mappedEntity.station.resolvedStreamUrl);
        assertEquals("https://example.com", mappedEntity.station.homepage);
        assertEquals("https://example.com/icon.png", mappedEntity.station.favicon);
        assertEquals("Croatia", mappedEntity.station.country);
        assertEquals("HR", mappedEntity.station.countryCode);
        assertEquals("Croatian", mappedEntity.station.language);
        assertEquals("news, talk", mappedEntity.station.tags);
        assertEquals("MP3", mappedEntity.station.codec);
        assertEquals(192, mappedEntity.station.bitrate);
        assertEquals(Boolean.FALSE, mappedEntity.station.hls);
        assertEquals("RADIO_BROWSER", mappedEntity.station.origin);
        assertEquals(123L, mappedEntity.displayOrder);
        assertEquals(123L, mappedEntity.addedAt);
        assertEquals(999L, mappedEntity.updatedAt);
    }

    @Test
    public void toStation_fallsBackToLocalOriginFromIdPrefixWhenOriginIsMalformed() {
        FavoriteStationEntity entity = new FavoriteStationEntity(
                "LOCAL:test-station",
                new StationSnapshotFields(
                        "Local Test",
                        "https://example.com/live",
                        null,
                        "",
                        "",
                        "Serbia",
                        "RS",
                        "Serbian",
                        "",
                        "",
                        128,
                        null,
                        "not-a-valid-origin"
                ),
                5L,
                111L,
                222L
        );

        Station station = StationMapper.toStation(entity);

        assertEquals("LOCAL:test-station", station.getId());
        assertEquals(StationOrigin.LOCAL_USER, station.getOrigin());
        assertEquals("Serbia", station.getCountry());
        assertEquals("RS", station.getCountryCode());
        assertEquals("Serbian", station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
        assertNull(station.getHomepage());
        assertNull(station.getFavicon());
    }
}
