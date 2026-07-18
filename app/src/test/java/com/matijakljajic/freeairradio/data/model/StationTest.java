package com.matijakljajic.freeairradio.data.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@SuppressWarnings("GrazieInspectionRunner")
public class StationTest {

    @Test
    public void builderWithOriginOnlyDefaultsOptionalFields() {
        Station station = stationBuilder("id-1")
                .build();

        assertEquals(Station.UNKNOWN, station.getCountry());
        assertEquals(Station.UNKNOWN, station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
        assertEquals(0, station.getBitrate());
    }

    @Test
    public void builderWithCountryOnlyStoresCountry() {
        Station station = stationBuilder("id-2")
                .setCountry("Norway")
                .build();

        assertEquals("Norway", station.getCountry());
        assertEquals(Station.UNKNOWN, station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
    }

    @Test
    public void builderWithCountryAndLanguageStoresLanguage() {
        Station station = stationBuilder("id-3")
                .setCountry("Serbia")
                .setLanguage("Serbian")
                .build();

        assertEquals("Serbia", station.getCountry());
        assertEquals("Serbian", station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
    }

    @Test
    public void builderWithTagsStoresTags() {
        Station station = stationBuilder("id-4")
                .setCountry("France")
                .setLanguage("French")
                .setTags("jazz, smooth jazz")
                .build();

        assertEquals("France", station.getCountry());
        assertEquals("French", station.getLanguage());
        assertEquals("jazz, smooth jazz", station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
    }

    @Test
    public void builderWithCodecStoresCodec() {
        Station station = stationBuilder("id-5")
                .setCountry("Croatia")
                .setLanguage("Croatian")
                .setTags("classic")
                .setCodec("AAC+")
                .build();

        assertEquals("Croatia", station.getCountry());
        assertEquals("Croatian", station.getLanguage());
        assertEquals("classic", station.getTags());
        assertEquals("AAC+", station.getCodec());
        assertEquals(0, station.getBitrate());
    }

    @Test
    public void builderNormalizesUnknownValues() {
        Station station = stationBuilder("id-6")
                .setCountry("unknown country")
                .setLanguage("  ")
                .setTags("unknown tags")
                .setCodec("unknown codec")
                .setBitrate(128)
                .build();

        assertEquals(Station.UNKNOWN, station.getCountry());
        assertEquals(Station.UNKNOWN, station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
        assertEquals(128, station.getBitrate());
    }

    @Test
    public void equalsAndHashCodeDependOnNormalizedValues() {
        Station first = stationBuilder("id-7")
                .setCountry(" Norway ")
                .setLanguage("norwegian")
                .setTags("jazz")
                .setCodec("MP3")
                .setBitrate(128)
                .build();
        Station second = stationBuilder("id-7")
                .setCountry("Norway")
                .setLanguage("norwegian")
                .setTags("jazz")
                .setCodec("MP3")
                .setBitrate(128)
                .build();
        Station different = Station.builder(
                        "id-8",
                        "Other Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
    }

    @Test
    public void getPlayableStreamUrlFallsBackToOriginalStreamUrl() {
        Station station = Station.builder(
                        "RADIO_BROWSER:1",
                        "Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .build();

        assertEquals("https://example.com/stream", station.getPlayableStreamUrl());
    }

    private Station.Builder stationBuilder(String id) {
        return Station.builder(
                id,
                "Test Station",
                "https://example.com/stream",
                StationOrigin.RADIO_BROWSER
        );
    }
}
