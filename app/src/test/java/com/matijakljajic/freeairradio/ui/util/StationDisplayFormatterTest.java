package com.matijakljajic.freeairradio.ui.util;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StationDisplayFormatterTest {

    @Test
    public void formatTags_trimsAndJoinsValues() {
        Station station = Station.builder(
                        "id-0",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .setCountry("Norway")
                .setLanguage("norwegian")
                .setTags(" jazz, smooth jazz , pop ")
                .build();

        assertEquals("jazz, smooth jazz, pop", StationDisplayFormatter.formatTags(station));
    }

    @Test
    public void formatTags_returnsUnknownForUnknownStationTags() {
        Station station = Station.builder(
                        "id-1",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .setCountry("Norway")
                .setLanguage("norwegian")
                .setTags("unknown")
                .build();

        assertEquals(Station.UNKNOWN, StationDisplayFormatter.formatTags(station));
    }

    @Test
    public void formatStationDetails_omitsUnknownTags() {
        Station station = Station.builder(
                        "id-1",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .setCountry("Norway")
                .setLanguage("norwegian")
                .setTags(Station.UNKNOWN)
                .build();

        assertEquals("Norway", StationDisplayFormatter.formatStationDetails(station));
    }

    @Test
    public void formatStationDetails_combinesCountryAndTags() {
        Station station = Station.builder(
                        "id-2",
                        "Test Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .setCountry("Norway")
                .setLanguage("norwegian")
                .setTags("jazz,smooth jazz")
                .build();

        assertEquals("Norway • jazz, smooth jazz", StationDisplayFormatter.formatStationDetails(station));
    }
}
