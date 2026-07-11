package com.matijakljajic.freeairradio.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.BundleCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.playback.RadioPlayer;
import com.matijakljajic.freeairradio.ui.homepage.HomePageFragment;
import com.matijakljajic.freeairradio.ui.player.PlayerFragment;
import com.matijakljajic.freeairradio.ui.settings.SettingsFragment;
import com.matijakljajic.freeairradio.ui.stations.StationFeedFragment;
import com.matijakljajic.freeairradio.ui.stations.StationSearchFragment;

public class MainActivity extends AppCompatActivity implements StationFeedFragment.OnStationSelectedListener, ShellChromeHost {

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;
    private static final String STATE_SELECTED_STATION = "state_selected_station";
    private static final String STATE_CURRENT_TAB = "state_current_tab";

    @Nullable
    private Station selectedStation;
    @NonNull
    private Tab currentTab = Tab.HOME;
    @Nullable
    private MaterialButtonToggleGroup navToggleGroup;
    @Nullable
    private ShellChromeController shellChromeController;
    @Nullable
    private RadioPlayer radioPlayer;
    private boolean suppressNavCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        if (savedInstanceState != null) {
            selectedStation = BundleCompat.getSerializable(savedInstanceState, STATE_SELECTED_STATION, Station.class);
            String savedTabName = savedInstanceState.getString(STATE_CURRENT_TAB, Tab.HOME.name());
            currentTab = Tab.valueOf(savedTabName);
        }

        navToggleGroup = findViewById(R.id.main_nav_toggle_group);
        radioPlayer = new RadioPlayer(this);
        shellChromeController = new ShellChromeController(
                findViewById(R.id.main),
                findViewById(R.id.status_bar_filter),
                findViewById(R.id.bottom_content_filter),
                findViewById(R.id.player_shell_container),
                findViewById(R.id.search_shell_overlay_container)
        );
        if (navToggleGroup != null) {
            navToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked || suppressNavCallbacks) {
                    return;
                }
                Tab tab = Tab.fromButtonId(checkedId);
                if (tab != null) {
                    selectTab(tab);
                }
            });
        }
        if (shellChromeController != null) {
            shellChromeController.attach();
        }
        requestNotificationPermissionIfNeeded();
        selectTab(currentTab, true);
        syncPlayerFragment();
    }

    @Override
    public void onStationSelected(@NonNull Station station) {
        selectedStation = station;
        if (radioPlayer != null) {
            radioPlayer.play(station);
        }
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

    @Override
    protected void onDestroy() {
        if (shellChromeController != null) {
            shellChromeController.detach();
        }
        radioPlayer = null;
        super.onDestroy();
    }

    @Nullable
    public ShellChromeController getShellChromeController() {
        return shellChromeController;
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
                .replace(R.id.main_content_fragment_container, tab.createFragment())
                .setTransition(ShellChromeController.DEFAULT_SHELL_TRANSITION_TYPE)
                .runOnCommit(() -> {
                    if (shellChromeController != null) {
                        shellChromeController.setFloaterShellVisible(tab == Tab.SEARCH, ShellChromeController.DEFAULT_SHELL_TRANSITION_TYPE, 0L);
                    }
                })
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

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_CODE_POST_NOTIFICATIONS
        );
    }

    private enum Tab {
        HOME(R.id.nav_home_button) {
            @NonNull
            @Override
            Fragment createFragment() {
                return new HomePageFragment();
            }
        },
        SEARCH(R.id.nav_search_button) {
            @NonNull
            @Override
            Fragment createFragment() {
                return new StationSearchFragment();
            }
        },
        SETTINGS(R.id.nav_settings_button) {
            @NonNull
            @Override
            Fragment createFragment() {
                return new SettingsFragment();
            }
        };

        final int buttonId;

        Tab(int buttonId) {
            this.buttonId = buttonId;
        }

        @Nullable
        static Tab fromButtonId(int buttonId) {
            for (Tab tab : values()) {
                if (tab.buttonId == buttonId) {
                    return tab;
                }
            }
            return null;
        }

        @NonNull
        abstract Fragment createFragment();
    }
}
