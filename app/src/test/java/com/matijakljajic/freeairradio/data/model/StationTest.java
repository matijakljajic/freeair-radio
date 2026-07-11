package com.matijakljajic.freeairradio.data.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@SuppressWarnings("GrazieInspectionRunner")
public class StationTest {

    @Test
    public void builderWithOriginOnlyDefaultsOptionalFields() {
        Station station = Station.builder(
                        "id-1",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .build();

        assertEquals(Station.UNKNOWN, station.getCountry());
        assertEquals(Station.UNKNOWN, station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
        assertEquals(0, station.getBitrate());
    }

    @Test
    public void builderWithCountryOnlyStoresCountry() {
        Station station = Station.builder(
                        "id-2",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .setCountry("Norway")
                .build();

        assertEquals("Norway", station.getCountry());
        assertEquals(Station.UNKNOWN, station.getLanguage());
        assertEquals(Station.UNKNOWN, station.getTags());
        assertEquals(Station.UNKNOWN, station.getCodec());
    }

    @Test
    public void builderWithCountryAndLanguageStoresLanguage() {
        Station station = Station.builder(
                        "id-3",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
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
        Station station = Station.builder(
                        "id-4",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
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
        Station station = Station.builder(
                        "id-5",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
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
        Station station = Station.builder(
                        "id-6",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
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
        Station first = Station.builder(
                        "id-7",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .setCountry(" Norway ")
                .setLanguage("norwegian")
                .setTags("jazz")
                .setCodec("MP3")
                .setBitrate(128)
                .build();
        Station second = Station.builder(
                        "id-7",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
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
}
