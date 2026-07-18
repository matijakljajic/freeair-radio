package com.matijakljajic.freeairradio.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FavoriteStationNavigatorTest {

    private FavoriteStationNavigator navigator;

    @Before
    public void setUp() {
        navigator = new FavoriteStationNavigator();
    }

    @Test
    public void canNavigate_returnsFalseWhenCurrentStationIsNotFavorite() {
        Station currentStation = station("RADIO_BROWSER:current", "Current");
        List<Station> favoriteStations = Arrays.asList(
                station("RADIO_BROWSER:first", "First"),
                station("RADIO_BROWSER:second", "Second")
        );

        assertFalse(navigator.canNavigate(currentStation, favoriteStations));
        assertNull(navigator.getPrevious(currentStation, favoriteStations));
        assertNull(navigator.getNext(currentStation, favoriteStations));
    }

    @Test
    public void canNavigate_returnsFalseWhenThereIsOnlyOneFavorite() {
        Station onlyFavorite = station("RADIO_BROWSER:only", "Only");

        assertFalse(navigator.canNavigate(onlyFavorite, Collections.singletonList(onlyFavorite)));
        assertNull(navigator.getPrevious(onlyFavorite, Collections.singletonList(onlyFavorite)));
        assertNull(navigator.getNext(onlyFavorite, Collections.singletonList(onlyFavorite)));
    }

    @Test
    public void previousAndNext_wrapAroundFavoriteOrder() {
        Station first = station("RADIO_BROWSER:first", "First");
        Station second = station("RADIO_BROWSER:second", "Second");
        Station third = station("RADIO_BROWSER:third", "Third");
        List<Station> favoriteStations = Arrays.asList(first, second, third);

        assertTrue(navigator.canNavigate(first, favoriteStations));
        assertEquals(third, navigator.getPrevious(first, favoriteStations));
        assertEquals(second, navigator.getNext(first, favoriteStations));
        assertEquals(first, navigator.getPrevious(second, favoriteStations));
        assertEquals(third, navigator.getNext(second, favoriteStations));
        assertEquals(second, navigator.getPrevious(third, favoriteStations));
        assertEquals(first, navigator.getNext(third, favoriteStations));
    }

    private static Station station(String id, String name) {
        return Station.builder(
                        id,
                        name,
                        "https://example.com/" + id,
                        StationOrigin.RADIO_BROWSER
                )
                .build();
    }
}
