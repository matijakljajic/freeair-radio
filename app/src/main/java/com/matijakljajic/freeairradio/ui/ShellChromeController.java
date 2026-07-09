package com.matijakljajic.freeairradio.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.card.MaterialCardView;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.stations.StationListFragment;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

public final class ShellChromeController {

    public static final int DEFAULT_SHELL_TRANSITION_TYPE = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE;
    // Matches the default duration of the parent fragment close transition.
    public static final long DEFAULT_SHELL_TRANSITION_DURATION_MS = 300L;

    @NonNull
    private final View rootView;
    @Nullable
    private final View statusBarFilterView;
    @Nullable
    private final View bottomContentFilterView;
    @Nullable
    private final View playerShellContainerView;
    @Nullable
    private final ViewGroup floaterShellOverlayContainer;
    @Nullable
    private final View.OnLayoutChangeListener playerShellLayoutChangeListener;
    @Nullable
    private final View.OnLayoutChangeListener floaterShellLayoutChangeListener;
    @Nullable
    private MaterialCardView floaterShellView;
    @Nullable
    private EditText searchInput;
    @Nullable
    private View searchButton;
    @Nullable
    private StationListFragment floaterStationListFragment;
    private int statusBarInsetPx;
    private int topContentFilterHeightPx;
    private boolean floaterShellVisible;

    public ShellChromeController(@NonNull View rootView,
                                 @Nullable View statusBarFilterView,
                                 @Nullable View bottomContentFilterView,
                                 @Nullable View playerShellContainerView,
                                 @Nullable ViewGroup floaterShellOverlayContainer) {
        this.rootView = rootView;
        this.statusBarFilterView = statusBarFilterView;
        this.bottomContentFilterView = bottomContentFilterView;
        this.playerShellContainerView = playerShellContainerView;
        this.floaterShellOverlayContainer = floaterShellOverlayContainer;
        this.playerShellLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateBottomContentFilter();
        this.floaterShellLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateFloaterShellLayout();
    }

    public void attach() {
        if (playerShellContainerView != null && playerShellLayoutChangeListener != null) {
            playerShellContainerView.addOnLayoutChangeListener(playerShellLayoutChangeListener);
        }
        ensureFloaterShell();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            statusBarInsetPx = systemBars.top;
            updateTopContentFilter();
            applyFloaterShellTopMargin();
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
        updateTopContentFilter();
        updateBottomContentFilter();
        applyFloaterShellVisibility(false, DEFAULT_SHELL_TRANSITION_TYPE, 0L);
        updateFloaterShellLayout();
    }

    public void detach() {
        if (playerShellContainerView != null && playerShellLayoutChangeListener != null) {
            playerShellContainerView.removeOnLayoutChangeListener(playerShellLayoutChangeListener);
        }
        if (floaterShellView != null && floaterShellLayoutChangeListener != null) {
            floaterShellView.removeOnLayoutChangeListener(floaterShellLayoutChangeListener);
        }
        if (floaterShellOverlayContainer != null && floaterShellView != null) {
            floaterShellOverlayContainer.removeView(floaterShellView);
        }
        ViewCompat.setOnApplyWindowInsetsListener(rootView, null);
        floaterShellView = null;
        searchInput = null;
        searchButton = null;
        floaterStationListFragment = null;
    }

    public void setFloaterShellVisible(boolean visible) {
        setFloaterShellVisible(visible, DEFAULT_SHELL_TRANSITION_TYPE, 0L);
    }

    public void setFloaterShellVisible(boolean visible, int transitionType) {
        setFloaterShellVisible(visible, transitionType, 0L);
    }

    public void setFloaterShellVisible(boolean visible, int transitionType, long transitionDelayMs) {
        if (floaterShellVisible == visible) {
            return;
        }
        floaterShellVisible = visible;
        applyFloaterShellVisibility(true, transitionType, transitionDelayMs);
        updateFloaterShellLayout();
        if (!visible) {
            setTopContentFilterHeightPx(0);
        }
    }

    public void setFloaterStationListFragment(@Nullable StationListFragment stationListFragment) {
        floaterStationListFragment = stationListFragment;
        updateFloaterShellLayout();
    }

    @Nullable
    public EditText getSearchInput() {
        return searchInput;
    }

    @Nullable
    public View getSearchButton() {
        return searchButton;
    }

    private void ensureFloaterShell() {
        if (floaterShellView != null || floaterShellOverlayContainer == null) {
            return;
        }

        View shellView = LayoutInflater.from(rootView.getContext())
                .inflate(R.layout.view_station_search_shell, floaterShellOverlayContainer, false);
        floaterShellOverlayContainer.addView(shellView);
        floaterShellView = (MaterialCardView) shellView;
        searchInput = floaterShellView.findViewById(R.id.station_search_input);
        searchButton = floaterShellView.findViewById(R.id.station_search_button);
        floaterShellView.addOnLayoutChangeListener(floaterShellLayoutChangeListener);
    }

    private void applyFloaterShellVisibility(boolean animate,
                                             int transitionType,
                                             long transitionDelayMs) {
        if (floaterShellView == null) {
            return;
        }
        if (!animate) {
            floaterShellView.setVisibility(floaterShellVisible ? View.VISIBLE : View.GONE);
            return;
        }
        setShellVisibilityWithTransition(floaterShellView, floaterShellOverlayContainer, floaterShellVisible, transitionType, transitionDelayMs);
    }

    private void setShellVisibilityWithTransition(@Nullable View shellView,
                                                   @Nullable ViewGroup transitionContainer,
                                                   boolean visible,
                                                   int transitionType,
                                                   long transitionDelayMs) {
        if (shellView == null) {
            return;
        }
        if (transitionContainer != null) {
            TransitionManager.beginDelayedTransition(transitionContainer, createShellTransition(transitionType, transitionDelayMs));
        }
        shellView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private Transition createShellTransition(int transitionType, long transitionDelayMs) {
        if (transitionType == FragmentTransaction.TRANSIT_FRAGMENT_FADE) {
            Fade fade = new Fade();
            fade.setStartDelay(transitionDelayMs);
            fade.setDuration(DEFAULT_SHELL_TRANSITION_DURATION_MS);
            return fade;
        }
        Slide slide = new Slide(Gravity.TOP);
        slide.setDuration(DEFAULT_SHELL_TRANSITION_DURATION_MS);
        slide.setStartDelay(transitionDelayMs);
        return slide;
    }

    private void updateFloaterShellLayout() {
        if (floaterShellView == null) {
            return;
        }

        if (!floaterShellVisible || floaterStationListFragment == null) {
            setTopContentFilterHeightPx(0);
            return;
        }

        int desiredTopPaddingPx = floaterShellView.getBottom() + UiDimensions.px(rootView.getContext(), R.dimen.search_list_gap);
        floaterStationListFragment.setSearchTopPaddingPx(desiredTopPaddingPx);
        floaterStationListFragment.setBottomRecyclerGapPx(UiDimensions.px(rootView.getContext(), R.dimen.search_list_bottom_gap));
        setTopContentFilterHeightPx(desiredTopPaddingPx);
    }

    private void applyFloaterShellTopMargin() {
        if (floaterShellView == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = floaterShellView.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int desiredTopMarginPx = statusBarInsetPx + UiDimensions.px(rootView.getContext(), R.dimen.search_top_gap);
        if (marginLayoutParams.topMargin != desiredTopMarginPx) {
            marginLayoutParams.topMargin = desiredTopMarginPx;
            floaterShellView.setLayoutParams(marginLayoutParams);
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
