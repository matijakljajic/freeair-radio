package com.matijakljajic.freeairradio.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.player.PlayerFragment;
import com.matijakljajic.freeairradio.ui.stations.StationListFragment;

public class MainActivity extends AppCompatActivity implements StationListFragment.OnStationSelectedListener {

    private static final String STATE_SELECTED_STATION = "state_selected_station";

    @Nullable
    private Station selectedStation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            selectedStation = (Station) savedInstanceState.getSerializable(STATE_SELECTED_STATION);
        }

        syncPlayerFragment();
    }

    @Override
    public void onStationSelected(Station station) {
        selectedStation = station;
        syncPlayerFragment();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_SELECTED_STATION, selectedStation);
    }

    private void syncPlayerFragment() {
        PlayerFragment fragment = (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.player_fragment_container);
        if (fragment != null) {
            fragment.showStation(selectedStation);
        }
    }
}
