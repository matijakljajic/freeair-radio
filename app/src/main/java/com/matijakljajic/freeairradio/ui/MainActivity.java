package com.matijakljajic.freeairradio.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.player.PlayerFragment;
import com.matijakljajic.freeairradio.ui.stations.StationListFragment;
import com.matijakljajic.freeairradio.ui.stations.StationSearchFragment;
import com.matijakljajic.freeairradio.ui.settings.SettingsFragment;

import com.google.android.material.button.MaterialButtonToggleGroup;

public class MainActivity extends AppCompatActivity implements StationListFragment.OnStationSelectedListener {

    private static final String STATE_SELECTED_STATION = "state_selected_station";
    private static final String STATE_CURRENT_TAB = "state_current_tab";

    @Nullable
    private Station selectedStation;
    @NonNull
    private Tab currentTab = Tab.HOME;
    @Nullable
    private MaterialButtonToggleGroup navToggleGroup;
    private boolean suppressNavCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            selectedStation = (Station) savedInstanceState.getSerializable(STATE_SELECTED_STATION);
            String savedTabName = savedInstanceState.getString(STATE_CURRENT_TAB, Tab.HOME.name());
            currentTab = Tab.valueOf(savedTabName);
        }

        navToggleGroup = findViewById(R.id.main_nav_toggle_group);
        if (navToggleGroup != null) {
            navToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked || suppressNavCallbacks) {
                    return;
                }
                if (checkedId == R.id.nav_home_button) {
                    selectTab(Tab.HOME);
                } else if (checkedId == R.id.nav_search_button) {
                    selectTab(Tab.SEARCH);
                } else if (checkedId == R.id.nav_settings_button) {
                    selectTab(Tab.SETTINGS);
                }
            });
        }
        selectTab(currentTab, true);
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
        outState.putString(STATE_CURRENT_TAB, currentTab.name());
    }

    private void syncPlayerFragment() {
        PlayerFragment fragment = (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.player_fragment_container);
        if (fragment != null) {
            fragment.showStation(selectedStation);
        }
    }

    private void selectTab(@NonNull Tab tab) {
        selectTab(tab, false);
    }

    private void selectTab(@NonNull Tab tab, boolean forceReplace) {
        if (!forceReplace && tab == currentTab) {
            syncNavSelection();
            return;
        }
        currentTab = tab;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.station_list_fragment_container, createFragmentForTab(tab))
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();
        syncNavSelection();
    }

    private void syncNavSelection() {
        if (navToggleGroup == null) {
            return;
        }

        int desiredButtonId = currentTab.buttonId;
        if (navToggleGroup.getCheckedButtonId() == desiredButtonId) {
            return;
        }

        suppressNavCallbacks = true;
        navToggleGroup.check(desiredButtonId);
        suppressNavCallbacks = false;
    }

    @NonNull
    private Fragment createFragmentForTab(@NonNull Tab tab) {
        switch (tab) {
            case SEARCH:
                return new StationSearchFragment();
            case SETTINGS:
                return new SettingsFragment();
            case HOME:
            default:
                return new StationListFragment();
        }
    }

    private enum Tab {
        HOME(R.id.nav_home_button),
        SEARCH(R.id.nav_search_button),
        SETTINGS(R.id.nav_settings_button);

        final int buttonId;

        Tab(int buttonId) {
            this.buttonId = buttonId;
        }
    }
}
