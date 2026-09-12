package com.matijakljajic.freeairradio.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.BundleCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.RadioBrowserRepository;
import com.matijakljajic.freeairradio.playback.RadioPlayer;
import com.matijakljajic.freeairradio.ui.settings.AppThemeSettings;
import com.matijakljajic.freeairradio.ui.homepage.HomePageFragment;
import com.matijakljajic.freeairradio.ui.player.PlayerFragment;
import com.matijakljajic.freeairradio.ui.player.ListeningHistoryOverlayController;
import com.matijakljajic.freeairradio.ui.settings.SettingsFragment;
import com.matijakljajic.freeairradio.ui.shell.AppShellController;
import com.matijakljajic.freeairradio.ui.shell.AppShellHost;
import com.matijakljajic.freeairradio.ui.stations.StationFeedFragment;
import com.matijakljajic.freeairradio.ui.stations.StationSearchFragment;

@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity implements
        StationFeedFragment.OnStationSelectedListener,
        AppShellHost,
        PlayerFragment.PlayerSurfaceHost {

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;
    private static final int TAB_CHANGE_TRANSITION = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE;
    private static final String STATE_SELECTED_STATION = "state_selected_station";
    private static final String STATE_CURRENT_TAB = "state_current_tab";

    @Nullable
    private Station selectedStation;
    @NonNull
    private Tab currentTab = Tab.HOME;
    @Nullable
    private MaterialButtonToggleGroup navToggleGroup;
    @Nullable
    private AppShellController appShellController;
    @Nullable
    private RadioPlayer radioPlayer;
    @Nullable
    private ListeningHistoryOverlayController listeningHistoryOverlayController;
    private boolean suppressNavCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new AppThemeSettings(this).applyNightMode();

        super.onCreate(savedInstanceState);
        configureWindow();
        restoreState(savedInstanceState);
        bindViews();
        bindNavigation();
        attachAppShell();
        requestNotificationPermissionIfNeeded();
        refreshRadioBrowserServers();
        selectTab(currentTab, true);
        showSelectedStation();
    }

    @Override
    public void onStationSelected(@NonNull Station station) {
        selectedStation = station;
        if (radioPlayer != null) {
            radioPlayer.play(station);
        }
        showSelectedStation();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_SELECTED_STATION, selectedStation);
        outState.putString(STATE_CURRENT_TAB, currentTab.name());
    }

    private void configureWindow() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
    }

    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        selectedStation = BundleCompat.getSerializable(savedInstanceState, STATE_SELECTED_STATION, Station.class);
        currentTab = Tab.fromName(savedInstanceState.getString(STATE_CURRENT_TAB));
    }

    private void bindViews() {
        ViewGroup rootView = findRequiredView(R.id.main);
        navToggleGroup = findRequiredView(R.id.main_nav_toggle_group);
        radioPlayer = new RadioPlayer(this);
        appShellController = new AppShellController(
                rootView,
                findRequiredView(R.id.status_bar_filter),
                findRequiredView(R.id.bottom_content_filter),
                findRequiredView(R.id.bottom_controls_container),
                findRequiredView(R.id.search_overlay_container)
        );
        View playerHistoryOverlay = findViewById(R.id.player_history_overlay_container);
        if (playerHistoryOverlay != null) {
            listeningHistoryOverlayController = new ListeningHistoryOverlayController(
                    this,
                    rootView,
                    playerHistoryOverlay,
                    findRequiredView(R.id.player_history_overlay_scrim),
                    findRequiredView(R.id.player_fragment_container),
                    findRequiredView(R.id.expanded_player_container),
                    findRequiredView(R.id.listening_history_fragment_container)
            );
        }
    }

    @NonNull
    private <T extends View> T findRequiredView(@IdRes int viewId) {
        T view = findViewById(viewId);
        if (view == null) {
            throw new IllegalStateException(
                    "Missing required activity view: " + getResources().getResourceName(viewId)
            );
        }
        return view;
    }

    private void bindNavigation() {
        if (navToggleGroup == null) {
            return;
        }

        navToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || suppressNavCallbacks) {
                return;
            }
            onNavButtonChecked(checkedId);
        });
    }

    private void onNavButtonChecked(int checkedId) {
        Tab tab = Tab.fromButtonId(checkedId);
        if (tab != null) {
            selectTab(tab);
        }
    }

    private void attachAppShell() {
        if (appShellController != null) {
            appShellController.attach();
        }
        if (listeningHistoryOverlayController != null) {
            listeningHistoryOverlayController.attach();
        }
    }

    private void refreshRadioBrowserServers() {
        RadioBrowserRepository.refreshServerDirectoryAsync();
    }

    private void showSelectedStation() {
        showSelectedStation(R.id.player_fragment_container);
        showSelectedStation(R.id.expanded_player_fragment_container);
    }

    private void showSelectedStation(int containerId) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(containerId);
        if (fragment instanceof PlayerFragment) {
            ((PlayerFragment) fragment).showStation(selectedStation);
        }
    }

    @Override
    protected void onDestroy() {
        detachAppShell();
        radioPlayer = null;
        super.onDestroy();
    }

    private void detachAppShell() {
        if (listeningHistoryOverlayController != null) {
            listeningHistoryOverlayController.detach();
        }
        if (appShellController != null) {
            appShellController.detach();
        }
    }

    @Override
    public void onPlayerSurfaceTap(boolean expanded) {
        if (listeningHistoryOverlayController == null) {
            return;
        }

        if (expanded) {
            listeningHistoryOverlayController.close();
        } else {
            listeningHistoryOverlayController.open();
        }
    }

    @Override
    public void onPlayerSurfaceSwipe(boolean expanded, boolean upward) {
        if (listeningHistoryOverlayController == null) {
            return;
        }

        if (!expanded && upward) {
            listeningHistoryOverlayController.open();
        } else if (expanded && !upward) {
            listeningHistoryOverlayController.close();
        }
    }

    @Nullable
    public AppShellController getAppShellController() {
        return appShellController;
    }

    private void selectTab(@NonNull Tab tab) {
        selectTab(tab, false);
    }

    private void selectTab(@NonNull Tab tab, boolean forceReplace) {
        if (!shouldReplaceTab(tab, forceReplace)) {
            syncNavSelection();
            return;
        }
        currentTab = tab;
        replaceMainFragment(tab);
        syncNavSelection();
    }

    private boolean shouldReplaceTab(@NonNull Tab tab, boolean forceReplace) {
        return forceReplace || tab != currentTab;
    }

    private void replaceMainFragment(@NonNull Tab tab) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_content_fragment_container, tab.createFragment())
                .setTransition(TAB_CHANGE_TRANSITION)
                .runOnCommit(() -> applyTabShellState(tab))
                .commit();
    }

    private void applyTabShellState(@NonNull Tab tab) {
        if (appShellController != null) {
            appShellController.setSearchBarVisible(tab.showsSearchBar);
        }
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
        HOME(R.id.nav_home_button, false) {
            @NonNull
            @Override
            Fragment createFragment() {
                return new HomePageFragment();
            }
        },
        SEARCH(R.id.nav_search_button, true) {
            @NonNull
            @Override
            Fragment createFragment() {
                return new StationSearchFragment();
            }
        },
        SETTINGS(R.id.nav_settings_button, false) {
            @NonNull
            @Override
            Fragment createFragment() {
                return new SettingsFragment();
            }
        };

        final int buttonId;
        final boolean showsSearchBar;

        Tab(int buttonId, boolean showsSearchBar) {
            this.buttonId = buttonId;
            this.showsSearchBar = showsSearchBar;
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
        static Tab fromName(@Nullable String tabName) {
            if (tabName == null) {
                return HOME;
            }

            try {
                return Tab.valueOf(tabName);
            } catch (IllegalArgumentException ignored) {
                return HOME;
            }
        }

        @NonNull
        abstract Fragment createFragment();
    }
}
