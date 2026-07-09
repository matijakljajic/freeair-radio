package com.matijakljajic.freeairradio.ui.util;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StationDisplayFormatterTest {

    @Test
    public void formatTags_trimsAndJoinsValues() {
        Station station = new Station(
                "id-0",
                "Test Station",
                "https://example.com/stream",
                "Norway",
                "norwegian",
                " jazz, smooth jazz , pop ",
                StationOrigin.RADIO_BROWSER
        );

        assertEquals("jazz, smooth jazz, pop", StationDisplayFormatter.formatTags(station));
    }

    @Test
    public void formatTags_returnsUnknownForUnknownStationTags() {
        Station station = new Station(
                "id-1",
                "Test Station",
                "https://example.com/stream",
                "Norway",
                "norwegian",
                "unknown",
                StationOrigin.RADIO_BROWSER
        );

        assertEquals(Station.UNKNOWN, StationDisplayFormatter.formatTags(station));
    }

    @Test
    public void formatStationDetails_omitsUnknownTags() {
        Station station = new Station(
                "id-1",
                "Test Station",
                "https://example.com/stream",
                "Norway",
                "norwegian",
                Station.UNKNOWN,
                StationOrigin.RADIO_BROWSER
        );

        assertEquals("Norway", StationDisplayFormatter.formatStationDetails(station));
    }

    @Test
    public void formatStationDetails_combinesCountryAndTags() {
        Station station = new Station(
                "id-2",
                "Test Station",
                "https://example.com/stream",
                "Norway",
                "norwegian",
                "jazz,smooth jazz",
                StationOrigin.RADIO_BROWSER
        );

        assertEquals("Norway • jazz, smooth jazz", StationDisplayFormatter.formatStationDetails(station));
    }
}
