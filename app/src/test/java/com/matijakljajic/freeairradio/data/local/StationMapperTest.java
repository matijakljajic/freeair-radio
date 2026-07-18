package com.matijakljajic.freeairradio.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.matijakljajic.freeairradio.data.local.entity.FavoriteStationEntity;
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
                123L,
                456L
        );

        FavoriteStationEntity mappedEntity =
                StationMapper.toFavoriteStationEntity(station, existingEntity, 999L);

        assertEquals("RADIO_BROWSER:test-station", mappedEntity.id);
        assertEquals("Radio Test", mappedEntity.name);
        assertEquals("https://example.com/live", mappedEntity.streamUrl);
        assertEquals("https://cdn.example.com/live", mappedEntity.resolvedStreamUrl);
        assertEquals("https://example.com", mappedEntity.homepage);
        assertEquals("https://example.com/icon.png", mappedEntity.favicon);
        assertEquals("Croatia", mappedEntity.country);
        assertEquals("HR", mappedEntity.countryCode);
        assertEquals("Croatian", mappedEntity.language);
        assertEquals("news, talk", mappedEntity.tags);
        assertEquals("MP3", mappedEntity.codec);
        assertEquals(192, mappedEntity.bitrate);
        assertEquals(Boolean.FALSE, mappedEntity.hls);
        assertEquals("RADIO_BROWSER", mappedEntity.origin);
        assertEquals(123L, mappedEntity.addedAt);
        assertEquals(999L, mappedEntity.updatedAt);
    }

    @Test
    public void toStation_fallsBackToLocalOriginFromIdPrefixWhenOriginIsMalformed() {
        FavoriteStationEntity entity = new FavoriteStationEntity(
                "LOCAL:test-station",
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
                "not-a-valid-origin",
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
