package com.matijakljajic.freeairradio.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.stations.StationListFragment;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

public final class ShellChromeController {

    @NonNull
    private final View rootView;
    @Nullable
    private final View statusBarFilterView;
    @Nullable
    private final View bottomContentFilterView;
    @Nullable
    private final View playerShellContainerView;
    @Nullable
    private final ViewGroup searchShellOverlayContainer;
    @Nullable
    private final View.OnLayoutChangeListener playerShellLayoutChangeListener;
    @Nullable
    private final View.OnLayoutChangeListener searchShellLayoutChangeListener;
    @Nullable
    private MaterialCardView searchShellView;
    @Nullable
    private EditText searchInput;
    @Nullable
    private View searchButton;
    @Nullable
    private StationListFragment searchStationListFragment;
    private int statusBarInsetPx;
    private int topContentFilterHeightPx;
    private boolean searchShellVisible;

    public ShellChromeController(@NonNull View rootView,
                                 @Nullable View statusBarFilterView,
                                 @Nullable View bottomContentFilterView,
                                 @Nullable View playerShellContainerView,
                                 @Nullable ViewGroup searchShellOverlayContainer) {
        this.rootView = rootView;
        this.statusBarFilterView = statusBarFilterView;
        this.bottomContentFilterView = bottomContentFilterView;
        this.playerShellContainerView = playerShellContainerView;
        this.searchShellOverlayContainer = searchShellOverlayContainer;
        this.playerShellLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateBottomContentFilter();
        this.searchShellLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateSearchShellLayout();
    }

    public void attach() {
        if (playerShellContainerView != null && playerShellLayoutChangeListener != null) {
            playerShellContainerView.addOnLayoutChangeListener(playerShellLayoutChangeListener);
        }
        ensureSearchShell();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            statusBarInsetPx = systemBars.top;
            updateTopContentFilter();
            applySearchShellTopMargin();
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
        updateTopContentFilter();
        updateBottomContentFilter();
        updateSearchShellVisibility();
        updateSearchShellLayout();
    }

    public void detach() {
        if (playerShellContainerView != null && playerShellLayoutChangeListener != null) {
            playerShellContainerView.removeOnLayoutChangeListener(playerShellLayoutChangeListener);
        }
        if (searchShellView != null && searchShellLayoutChangeListener != null) {
            searchShellView.removeOnLayoutChangeListener(searchShellLayoutChangeListener);
        }
        if (searchShellOverlayContainer != null && searchShellView != null) {
            searchShellOverlayContainer.removeView(searchShellView);
        }
        ViewCompat.setOnApplyWindowInsetsListener(rootView, null);
        searchShellView = null;
        searchInput = null;
        searchButton = null;
        searchStationListFragment = null;
    }

    public void setActiveSearchTab(boolean visible) {
        if (searchShellVisible == visible) {
            return;
        }
        searchShellVisible = visible;
        updateSearchShellVisibility();
        updateSearchShellLayout();
        if (!visible) {
            setTopContentFilterHeightPx(0);
        }
    }

    public void setSearchStationListFragment(@Nullable StationListFragment stationListFragment) {
        searchStationListFragment = stationListFragment;
        updateSearchShellLayout();
    }

    @Nullable
    public EditText getSearchInput() {
        return searchInput;
    }

    @Nullable
    public View getSearchButton() {
        return searchButton;
    }

    private void ensureSearchShell() {
        if (searchShellView != null || searchShellOverlayContainer == null) {
            return;
        }

        View shellView = LayoutInflater.from(rootView.getContext())
                .inflate(R.layout.view_station_search_shell, searchShellOverlayContainer, false);
        searchShellOverlayContainer.addView(shellView);
        searchShellView = (MaterialCardView) shellView;
        searchInput = searchShellView.findViewById(R.id.station_search_input);
        searchButton = searchShellView.findViewById(R.id.station_search_button);
        searchShellView.addOnLayoutChangeListener(searchShellLayoutChangeListener);
    }

    private void updateSearchShellVisibility() {
        if (searchShellView == null) {
            return;
        }
        searchShellView.setVisibility(searchShellVisible ? View.VISIBLE : View.GONE);
    }

    private void updateSearchShellLayout() {
        if (searchShellView == null) {
            return;
        }

        if (!searchShellVisible || searchStationListFragment == null) {
            setTopContentFilterHeightPx(0);
            return;
        }

        int desiredTopPaddingPx = searchShellView.getBottom() + UiDimensions.px(rootView.getContext(), R.dimen.search_list_gap);
        searchStationListFragment.setSearchTopPaddingPx(desiredTopPaddingPx);
        searchStationListFragment.setBottomRecyclerGapPx(UiDimensions.px(rootView.getContext(), R.dimen.search_list_bottom_gap));
        setTopContentFilterHeightPx(desiredTopPaddingPx);
    }

    private void applySearchShellTopMargin() {
        if (searchShellView == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = searchShellView.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int desiredTopMarginPx = statusBarInsetPx + UiDimensions.px(rootView.getContext(), R.dimen.search_top_gap);
        if (marginLayoutParams.topMargin != desiredTopMarginPx) {
            marginLayoutParams.topMargin = desiredTopMarginPx;
            searchShellView.setLayoutParams(marginLayoutParams);
        }
    }

    public void setTopContentFilterHeightPx(int heightPx) {
        int sanitizedHeightPx = Math.max(0, heightPx);
        if (topContentFilterHeightPx == sanitizedHeightPx) {
            return;
        }
        topContentFilterHeightPx = sanitizedHeightPx;
        updateTopContentFilter();
    }

    public void resetTopContentFilterHeight() {
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

        int desiredHeight = playerShellContainerView.getHeight()
                + getPlayerShellBottomMarginPx()
                + UiDimensions.px(rootView.getContext(), R.dimen.bottom_content_gap);
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
}
