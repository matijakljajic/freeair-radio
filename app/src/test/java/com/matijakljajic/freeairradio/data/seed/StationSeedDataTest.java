package com.matijakljajic.freeairradio.data.seed;

import com.matijakljajic.freeairradio.data.model.Station;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class StationSeedDataTest {

    @Test
    public void createDemoStations_returnsExpectedStations() {
        List<Station> stations = StationSeedData.createDemoStations();

        assertEquals(6, stations.size());
        assertEquals("1-NRK Jazz", stations.get(0).getName());
        assertEquals("Radio Caroline", stations.get(4).getName());
        assertTrue(stations.stream().anyMatch(station -> "Radio Nova Vintage".equals(station.getName())));
    }

    @Test
    public void createDemoStations_returnsUnmodifiableList() {
        List<Station> stations = StationSeedData.createDemoStations();

        assertThrows(UnsupportedOperationException.class, () ->
                stations.add(new Station(
                        "extra",
                        "Extra Station",
                        "https://example.com/stream",
                        com.matijakljajic.freeairradio.data.model.StationOrigin.RADIO_BROWSER
                )));
    }
}
