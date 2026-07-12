package com.matijakljajic.freeairradio.playback.metadata;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CurrentPlaybackStateTest {

    @Test
    public void setCurrentStationClearsPreviousNowPlaying() {
        CurrentPlaybackState playbackState = CurrentPlaybackState.getInstance();
        playbackState.clear();

        Station firstStation = createStation("RADIO_BROWSER:1", "First");
        Station secondStation = createStation("RADIO_BROWSER:2", "Second");
        playbackState.setCurrentStation(firstStation);
        playbackState.setCurrentNowPlaying(firstStation, new NowPlaying("Artist", "Track"));

        playbackState.setCurrentStation(secondStation);

        assertEquals(secondStation, playbackState.getCurrentStation());
        assertNull(playbackState.getCurrentNowPlaying());
        assertEquals(CurrentPlaybackState.PlaybackStatus.CONNECTING, playbackState.getPlaybackStatus());
        playbackState.clear();
    }

    @Test
    public void setCurrentNowPlayingIgnoresDifferentStation() {
        CurrentPlaybackState playbackState = CurrentPlaybackState.getInstance();
        playbackState.clear();

        Station currentStation = createStation("RADIO_BROWSER:1", "Current");
        Station otherStation = createStation("RADIO_BROWSER:2", "Other");
        playbackState.setCurrentStation(currentStation);

        playbackState.setCurrentNowPlaying(otherStation, new NowPlaying("Artist", "Track"));

        assertEquals(currentStation, playbackState.getCurrentStation());
        assertNull(playbackState.getCurrentNowPlaying());
        assertEquals(CurrentPlaybackState.PlaybackStatus.CONNECTING, playbackState.getPlaybackStatus());
        playbackState.clear();
    }

    @Test
    public void setPlaybackStatusIgnoresDifferentStation() {
        CurrentPlaybackState playbackState = CurrentPlaybackState.getInstance();
        playbackState.clear();

        Station currentStation = createStation("RADIO_BROWSER:1", "Current");
        Station otherStation = createStation("RADIO_BROWSER:2", "Other");
        playbackState.setCurrentStation(currentStation);

        playbackState.setPlaybackStatus(otherStation, CurrentPlaybackState.PlaybackStatus.PLAYING);

        assertEquals(CurrentPlaybackState.PlaybackStatus.CONNECTING, playbackState.getPlaybackStatus());
        playbackState.clear();
    }

    @Test
    public void clearResetsPlaybackStatusToIdle() {
        CurrentPlaybackState playbackState = CurrentPlaybackState.getInstance();
        playbackState.clear();

        Station station = createStation("RADIO_BROWSER:1", "Current");
        playbackState.setCurrentStation(station);
        playbackState.setPlaybackStatus(station, CurrentPlaybackState.PlaybackStatus.PLAYING);

        playbackState.clear();

        assertEquals(CurrentPlaybackState.PlaybackStatus.IDLE, playbackState.getPlaybackStatus());
        playbackState.clear();
    }

    private Station createStation(String id, String name) {
        return Station.builder(id, name, "https://example.com/stream", StationOrigin.RADIO_BROWSER)
                .build();
    }
}
