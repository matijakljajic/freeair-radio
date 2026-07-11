package com.matijakljajic.freeairradio.ui.player;

import android.widget.TextView;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.button.MaterialButton;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
import com.matijakljajic.freeairradio.playback.CurrentPlaybackState;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerFragmentTest {

    @After
    public void tearDown() {
        CurrentPlaybackState.getInstance().clear();
    }

    @Test
    public void initialStateShowsDefaultMiniPlayerState() {
        try (FragmentScenario<PlayerFragment> scenario = FragmentScenario.launchInContainer(
                PlayerFragment.class,
                null,
                R.style.Theme_FreeAirRadio
        )) {
            scenario.onFragment(fragment -> {
                TextView stationName = fragment.requireView().findViewById(R.id.player_station_name);
                TextView nowPlaying = fragment.requireView().findViewById(R.id.player_now_playing);
                MaterialButton playStop = fragment.requireView().findViewById(R.id.player_play_stop_button);

                assertEquals(fragment.getString(R.string.player_no_station_selected), stationName.getText().toString());
                assertEquals("", nowPlaying.getText().toString());
                assertEquals(android.view.View.INVISIBLE, nowPlaying.getVisibility());
                assertFalse(playStop.isEnabled());
            });
        }
    }

    @Test
    public void showStationUpdatesMiniPlayerAndActivatesTitleMarqueeAfterDelay() throws InterruptedException {
        Station station = Station.builder(
                        "id-1",
                        "Test Station 1",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .build();

        try (FragmentScenario<PlayerFragment> scenario = FragmentScenario.launchInContainer(
                PlayerFragment.class,
                null,
                R.style.Theme_FreeAirRadio
        )) {
            scenario.onFragment(fragment -> fragment.showStation(station));

            Thread.sleep(1200L);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scenario.onFragment(fragment -> {
                TextView stationName = fragment.requireView().findViewById(R.id.player_station_name);
                TextView nowPlaying = fragment.requireView().findViewById(R.id.player_now_playing);
                MaterialButton playStop = fragment.requireView().findViewById(R.id.player_play_stop_button);

                assertEquals("Test Station 1", stationName.getText().toString());
                assertEquals("", nowPlaying.getText().toString());
                assertEquals(android.view.View.INVISIBLE, nowPlaying.getVisibility());
                assertTrue(stationName.isSelected());
                assertFalse(nowPlaying.isSelected());
                assertTrue(playStop.isEnabled());
            });
        }
    }
}
