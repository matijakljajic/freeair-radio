package com.matijakljajic.freeairradio.ui.stations;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
import com.matijakljajic.freeairradio.data.repository.StationRepository;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class StationListFragmentTest {

    @After
    public void tearDown() {
        TestStationListFragment.repository = null;
    }

    @Test
    public void staleSearchResultIsIgnoredAfterQueryIsCleared() {
        ControllableStationRepository repository = new ControllableStationRepository();
        TestStationListFragment.repository = repository;

        try (FragmentScenario<TestStationListFragment> scenario = FragmentScenario.launchInContainer(
                TestStationListFragment.class,
                Bundle.EMPTY,
                R.style.Theme_FreeAirRadio)) {
            scenario.onFragment(fragment -> fragment.submitQuery("jazz"));
            assertEquals("jazz", repository.getLastSearchQuery());

            scenario.onFragment(fragment -> fragment.submitQuery(""));
            scenario.onFragment(fragment -> repository.deliverStations(Collections.singletonList(createStation())));

            scenario.onFragment(this::assertIdleState);
        }
    }

    private void assertIdleState(@NonNull TestStationListFragment fragment) {
        TextView emptyView = fragment.requireView().findViewById(R.id.station_feed_empty_view);
        ProgressBar loadingView = fragment.requireView().findViewById(R.id.station_feed_loading_view);
        View recyclerView = fragment.requireView().findViewById(R.id.station_feed_recycler_view);

        assertEquals(View.VISIBLE, emptyView.getVisibility());
        assertEquals(fragment.getString(R.string.station_search_idle), emptyView.getText().toString());
        assertEquals(View.GONE, loadingView.getVisibility());
        assertEquals(View.GONE, recyclerView.getVisibility());
    }

    @NonNull
    private Station createStation() {
        return Station.builder(
                        "id-1",
                        "Ignored Station",
                        "https://example.com/stream",
                        StationOrigin.RADIO_BROWSER)
                .build();
    }

    private static final class TestStationListFragment extends StationListFragment {
        @Nullable
        static StationRepository repository;

        @NonNull
        @Override
        protected StationRepository createStationRepository() {
            if (repository == null) {
                throw new IllegalStateException("Repository not initialized");
            }
            return repository;
        }
    }

    private static final class ControllableStationRepository implements StationRepository {
        @Nullable
        private LoadCallback callback;
        @Nullable
        private String lastSearchQuery;

        @Override
        public void reportStationUsage(@NonNull Station station) {
            // Search test does not exercise click reporting.
        }

        @Override
        public void loadTopStations(@NonNull LoadCallback callback) {
            this.callback = callback;
        }

        @Override
        public void searchStationsByName(@NonNull String query, @NonNull LoadCallback callback) {
            lastSearchQuery = query;
            this.callback = callback;
        }

        void deliverStations(@NonNull List<Station> stations) {
            if (callback == null) {
                throw new IllegalStateException("No pending callback");
            }
            callback.onStationsLoaded(stations);
        }

        @Nullable
        String getLastSearchQuery() {
            return lastSearchQuery;
        }
    }
}
