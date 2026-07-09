package com.matijakljajic.freeairradio.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.BundleCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.player.PlayerFragment;
import com.matijakljajic.freeairradio.ui.settings.SettingsFragment;
import com.matijakljajic.freeairradio.ui.stations.StationListFragment;
import com.matijakljajic.freeairradio.ui.stations.StationSearchFragment;

public class MainActivity extends AppCompatActivity implements StationListFragment.OnStationSelectedListener {

    private static final String STATE_SELECTED_STATION = "state_selected_station";
    private static final String STATE_CURRENT_TAB = "state_current_tab";

    @Nullable
    private Station selectedStation;
    @NonNull
    private Tab currentTab = Tab.HOME;
    @Nullable
    private MaterialButtonToggleGroup navToggleGroup;
    @Nullable
    private ShellFilterController shellFilterController;
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
        shellFilterController = new ShellFilterController(
                findViewById(R.id.main),
                findViewById(R.id.status_bar_filter),
                findViewById(R.id.bottom_content_filter),
                findViewById(R.id.player_shell_container)
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
        if (shellFilterController != null) {
            shellFilterController.attach();
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

    @Override
    protected void onDestroy() {
        if (shellFilterController != null) {
            shellFilterController.detach();
        }
        super.onDestroy();
    }

    public void setTopContentFilterHeightPx(int heightPx) {
        if (shellFilterController != null) {
            shellFilterController.setTopContentFilterHeightPx(heightPx);
        }
    }

    public void resetTopContentFilterHeight() {
        if (shellFilterController != null) {
            shellFilterController.resetTopContentFilterHeight();
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
                .replace(R.id.station_list_fragment_container, tab.createFragment())
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

    private enum Tab {
        HOME(R.id.nav_home_button) {
            @NonNull
            @Override
            Fragment createFragment() {
                return StationListFragment.newHomeInstance();
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

    private static final class ShellFilterController {

        private static final int BOTTOM_CONTENT_GAP_DP = 10;

        @NonNull
        private final View rootView;
        @Nullable
        private final View statusBarFilterView;
        @Nullable
        private final View bottomContentFilterView;
        @Nullable
        private final View playerShellContainerView;
        @Nullable
        private final View.OnLayoutChangeListener playerShellLayoutChangeListener;
        private int statusBarInsetPx;
        private int topContentFilterHeightPx;

        private ShellFilterController(@NonNull View rootView,
                                       @Nullable View statusBarFilterView,
                                       @Nullable View bottomContentFilterView,
                                       @Nullable View playerShellContainerView) {
            this.rootView = rootView;
            this.statusBarFilterView = statusBarFilterView;
            this.bottomContentFilterView = bottomContentFilterView;
            this.playerShellContainerView = playerShellContainerView;
            this.playerShellLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateBottomContentFilter();
        }

        private void attach() {
            if (playerShellContainerView != null && playerShellLayoutChangeListener != null) {
                playerShellContainerView.addOnLayoutChangeListener(playerShellLayoutChangeListener);
            }
            if (statusBarFilterView != null) {
                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
                    androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                    statusBarInsetPx = statusBarInsets.top;
                    updateTopContentFilter();
                    return insets;
                });
            }
            androidx.core.view.ViewCompat.requestApplyInsets(rootView);
            updateTopContentFilter();
            updateBottomContentFilter();
        }

        private void detach() {
            if (playerShellContainerView != null && playerShellLayoutChangeListener != null) {
                playerShellContainerView.removeOnLayoutChangeListener(playerShellLayoutChangeListener);
            }
        }

        private void setTopContentFilterHeightPx(int heightPx) {
            int sanitizedHeightPx = Math.max(0, heightPx);
            if (topContentFilterHeightPx == sanitizedHeightPx) {
                return;
            }
            topContentFilterHeightPx = sanitizedHeightPx;
            updateTopContentFilter();
        }

        private void resetTopContentFilterHeight() {
            if (topContentFilterHeightPx == 0) {
                return;
            }
            topContentFilterHeightPx = 0;
            updateTopContentFilter();
        }

        private void updateTopContentFilter() {
            if (statusBarFilterView == null) {
                return;
            }

            int desiredHeight = Math.max(statusBarInsetPx, topContentFilterHeightPx);
            ViewGroup.LayoutParams layoutParams = statusBarFilterView.getLayoutParams();
            if (layoutParams != null && layoutParams.height != desiredHeight) {
                layoutParams.height = desiredHeight;
                statusBarFilterView.setLayoutParams(layoutParams);
            }
            statusBarFilterView.setVisibility(desiredHeight > 0 ? View.VISIBLE : View.GONE);
        }

        private void updateBottomContentFilter() {
            if (bottomContentFilterView == null || playerShellContainerView == null) {
                return;
            }

            int desiredHeight = playerShellContainerView.getHeight() + getPlayerShellBottomMarginPx() + dpToPx(BOTTOM_CONTENT_GAP_DP);
            ViewGroup.LayoutParams layoutParams = bottomContentFilterView.getLayoutParams();
            if (layoutParams != null && layoutParams.height != desiredHeight) {
                layoutParams.height = desiredHeight;
                bottomContentFilterView.setLayoutParams(layoutParams);
            }
            bottomContentFilterView.setVisibility(desiredHeight > 0 ? View.VISIBLE : View.GONE);
        }

        private int getPlayerShellBottomMarginPx() {
            if (playerShellContainerView == null) {
                return 0;
            }

            ViewGroup.LayoutParams layoutParams = playerShellContainerView.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                return ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin;
            }
            return 0;
        }

        private int dpToPx(int dp) {
            return Math.round(dp * rootView.getResources().getDisplayMetrics().density);
        }
    }
}
