package com.matijakljajic.freeairradio.data.remote.radiobrowser;

import com.google.gson.Gson;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.dto.RadioBrowserStationDto;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("GrazieInspectionRunner")
public class RadioBrowserRepositoryTest {

    private final Gson gson = new Gson();

    @Test
    public void mapStationMapsFieldsAndNormalizesUnknownValues() {
        RadioBrowserStationDto dto = gson.fromJson("{"
                + "\"stationuuid\":\"uuid-1\","
                + "\"name\":\"Test Station\","
                + "\"url\":\"https://example.com/stream\","
                + "\"url_resolved\":\"https://example.com/resolved\","
                + "\"homepage\":\"https://example.com\","
                + "\"favicon\":\"https://example.com/favicon.png\","
                + "\"country\":\"unknown country\","
                + "\"countrycode\":\"\","
                + "\"language\":\"Serbian\","
                + "\"tags\":\"jazz,rock\","
                + "\"codec\":\"MP3\","
                + "\"bitrate\":128,"
                + "\"hls\":true"
                + "}", RadioBrowserStationDto.class);

        Station station = RadioBrowserRepository.mapStation(dto);

        Assert.assertNotNull(station);
        assertEquals("RADIO_BROWSER:uuid-1", station.getId());
        assertEquals("Test Station", station.getName());
        assertEquals("https://example.com/stream", station.getStreamUrl());
        assertEquals("https://example.com/resolved", station.getResolvedStreamUrl());
        assertEquals("https://example.com", station.getHomepage());
        assertEquals("https://example.com/favicon.png", station.getFavicon());
        assertEquals(Station.UNKNOWN, station.getCountry());
        assertEquals(Station.UNKNOWN, station.getCountryCode());
        assertEquals("Serbian", station.getLanguage());
        assertEquals("jazz,rock", station.getTags());
        assertEquals("MP3", station.getCodec());
        assertEquals(128, station.getBitrate());
        assertEquals(Boolean.TRUE, station.getHls());
        assertEquals("https://example.com/resolved", station.getPlayableStreamUrl());
        assertEquals(StationOrigin.RADIO_BROWSER, station.getOrigin());
    }

    @Test
    public void mapStationReturnsNullWhenRequiredDataIsMissing() {
        RadioBrowserStationDto dto = gson.fromJson("{"
                + "\"stationuuid\":\"uuid-2\","
                + "\"name\":\"Broken Station\""
                + "}", RadioBrowserStationDto.class);

        assertNull(RadioBrowserRepository.mapStation(dto));
    }

    @Test
    public void mapStationsFiltersNullEntries() {
        RadioBrowserStationDto first = gson.fromJson("{"
                + "\"stationuuid\":\"uuid-3\","
                + "\"name\":\"First\","
                + "\"url\":\"https://example.com/1\""
                + "}", RadioBrowserStationDto.class);
        List<Station> stations = RadioBrowserRepository.mapStations(Arrays.asList(first, null));

        assertEquals(1, stations.size());
        assertEquals("RADIO_BROWSER:uuid-3", stations.get(0).getId());
    }
}
