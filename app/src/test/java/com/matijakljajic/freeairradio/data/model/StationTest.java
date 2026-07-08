package com.matijakljajic.freeairradio.data.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StationTest {

    @Test
    public void constructorWithOriginOnlyDefaultsOptionalFields() {
        Station station = new Station(
                "id-1",
                "Test Station",
                "https://example.com/stream",
                StationOrigin.RADIO_BROWSER
        );

        assertEquals(Station.UNKNOWN, station.getCountry());
        assertEquals(Station.UNKNOWN, station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
        assertEquals(0, station.getBitrate());
    }

    @Test
    public void constructorWithCountryOnlyStoresCountry() {
        Station station = new Station(
                "id-2",
                "Test Station",
                "https://example.com/stream",
                "Norway",
                StationOrigin.RADIO_BROWSER
        );

        assertEquals("Norway", station.getCountry());
        assertEquals(Station.UNKNOWN, station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
    }

    @Test
    public void constructorWithCountryAndLanguageStoresLanguage() {
        Station station = new Station(
                "id-3",
                "Test Station",
                "https://example.com/stream",
                "Serbia",
                "Serbian",
                StationOrigin.RADIO_BROWSER
        );

        assertEquals("Serbia", station.getCountry());
        assertEquals("Serbian", station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
    }

    @Test
    public void constructorWithTagsStoresTags() {
        Station station = new Station(
                "id-4",
                "Test Station",
                "https://example.com/stream",
                "France",
                "French",
                "jazz, smooth jazz",
                StationOrigin.RADIO_BROWSER
        );

        assertEquals("France", station.getCountry());
        assertEquals("French", station.getLanguage());
        assertEquals("jazz, smooth jazz", station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
    }

    @Test
    public void constructorWithCodecStoresCodec() {
        Station station = new Station(
                "id-5",
                "Test Station",
                "https://example.com/stream",
                "Croatia",
                "Croatian",
                "classic",
                "AAC+",
                StationOrigin.RADIO_BROWSER
        );

        assertEquals("Croatia", station.getCountry());
        assertEquals("Croatian", station.getLanguage());
        assertEquals("classic", station.getTags());
        assertEquals("AAC+", station.getCodec());
        assertEquals(0, station.getBitrate());
    }

    @Test
    public void constructorNormalizesUnknownValues() {
        Station station = new Station(
                "id-6",
                "Test Station",
                "https://example.com/stream",
                "unknown country",
                "  ",
                "unknown tags",
                "unknown codec",
                128,
                StationOrigin.RADIO_BROWSER
        );

        assertEquals(Station.UNKNOWN, station.getCountry());
        assertEquals(Station.UNKNOWN, station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
        assertEquals(128, station.getBitrate());
    }
}
