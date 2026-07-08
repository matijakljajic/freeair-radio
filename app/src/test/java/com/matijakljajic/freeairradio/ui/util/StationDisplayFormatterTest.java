package com.matijakljajic.freeairradio.ui.util;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StationDisplayFormatterTest {

    @Test
    public void formatTags_trimsAndJoinsValues() {
        assertEquals("jazz, smooth jazz, pop", StationDisplayFormatter.formatTags(" jazz, smooth jazz , pop "));
    }

    @Test
    public void formatTags_returnsUnknownForEmptyOrUnknownInput() {
        assertEquals(Station.UNKNOWN, StationDisplayFormatter.formatTags(""));
        assertEquals(Station.UNKNOWN, StationDisplayFormatter.formatTags("unknown"));
        assertEquals(Station.UNKNOWN, StationDisplayFormatter.formatTags("   unknown   "));
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
