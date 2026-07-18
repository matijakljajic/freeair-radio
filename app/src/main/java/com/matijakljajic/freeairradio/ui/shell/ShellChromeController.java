package com.matijakljajic.freeairradio.ui.shell;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.transition.ChangeBounds;
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
    private final View.OnLayoutChangeListener playerShellLayoutChangeListener;
    private final View.OnLayoutChangeListener floaterShellLayoutChangeListener;
    @Nullable
    private MaterialCardView floaterShellView;
    @Nullable
    private EditText searchInput;
    @Nullable
    private View searchButton;
    @Nullable
    private StationListFragment floaterStationListFragment;
    @Nullable
    private View contentPaddingView;
    private int statusBarInsetPx;
    private int topContentFilterHeightPx;
    private int bottomContentFilterHeightPx;
    private int contentPaddingTopGapPx;
    private int contentPaddingBaseLeftPx;
    private int contentPaddingBaseTopPx;
    private int contentPaddingBaseRightPx;
    private int contentPaddingBaseBottomPx;
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
        attachPlayerShellListener();
        ensureFloaterShell();
        installWindowInsetsListener();
        updateTopContentFilter();
        updateBottomContentFilter();
        applyFloaterShellVisibility(false, DEFAULT_SHELL_TRANSITION_TYPE, 0L);
        updateFloaterShellLayout();
    }

    public void detach() {
        detachPlayerShellListener();
        removeFloaterShellView();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, null);
        clearAttachedReferences();
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

    public void attachContentPaddingView(@NonNull View contentPaddingView,
                                         int topGapPx) {
        this.contentPaddingView = contentPaddingView;
        contentPaddingTopGapPx = Math.max(0, topGapPx);
        contentPaddingBaseLeftPx = contentPaddingView.getPaddingLeft();
        contentPaddingBaseTopPx = contentPaddingView.getPaddingTop();
        contentPaddingBaseRightPx = contentPaddingView.getPaddingRight();
        contentPaddingBaseBottomPx = contentPaddingView.getPaddingBottom();
        updateContentPadding();
    }

    public void detachContentPaddingView(@NonNull View contentPaddingView) {
        if (this.contentPaddingView == contentPaddingView) {
            this.contentPaddingView = null;
        }
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
        updateContentPadding();
    }

    private void updateTopContentFilter() {
        if (statusBarFilterView == null) {
            return;
        }

        int desiredHeight = Math.max(
                Math.round(statusBarInsetPx * 1.5f),
                topContentFilterHeightPx
        );
        animateFilterHeight(statusBarFilterView, desiredHeight);
    }

    private void updateBottomContentFilter() {
        if (bottomContentFilterView == null || playerShellContainerView == null) {
            bottomContentFilterHeightPx = 0;
            updateContentPadding();
            return;
        }

        int desiredHeight = playerShellContainerView.getHeight()
                + getPlayerShellBottomMarginPx()
                + UiDimensions.px(rootView.getContext(), R.dimen.bottom_content_gap);
        bottomContentFilterHeightPx = Math.max(0, desiredHeight);
        animateFilterHeight(bottomContentFilterView, desiredHeight);
        updateContentPadding();
    }

    private void updateContentPadding() {
        if (contentPaddingView == null) {
            return;
        }

        applyContentPaddingIfChanged(
                contentPaddingBaseLeftPx,
                resolveTopContentPaddingPx(),
                contentPaddingBaseRightPx,
                resolveBottomContentPaddingPx()
        );
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

    private void animateFilterHeight(@NonNull View filterView, int desiredHeight) {
        ViewGroup.LayoutParams layoutParams = filterView.getLayoutParams();
        if (layoutParams == null) {
            return;
        }

        int currentHeight = layoutParams.height;
        if (currentHeight == desiredHeight) {
            filterView.setVisibility(desiredHeight > 0 ? View.VISIBLE : View.GONE);
            return;
        }

        if (rootView.isInLayout() || filterView.isInLayout()) {
            layoutParams.height = desiredHeight;
            filterView.setLayoutParams(layoutParams);
            filterView.setVisibility(desiredHeight > 0 ? View.VISIBLE : View.GONE);
            return;
        }

        if (desiredHeight > 0) {
            filterView.setVisibility(View.VISIBLE);
        }
        Transition transition = new ChangeBounds();
        transition.setDuration(DEFAULT_SHELL_TRANSITION_DURATION_MS);
        TransitionManager.beginDelayedTransition((ViewGroup) rootView, transition);
        layoutParams.height = desiredHeight;
        filterView.setLayoutParams(layoutParams);
        filterView.setVisibility(desiredHeight > 0 ? View.VISIBLE : View.GONE);
    }

    private void attachPlayerShellListener() {
        if (playerShellContainerView != null) {
            playerShellContainerView.addOnLayoutChangeListener(playerShellLayoutChangeListener);
        }
    }

    private void detachPlayerShellListener() {
        if (playerShellContainerView != null) {
            playerShellContainerView.removeOnLayoutChangeListener(playerShellLayoutChangeListener);
        }
    }

    private void installWindowInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            statusBarInsetPx = systemBars.top;
            updateTopContentFilter();
            updateContentPadding();
            applyFloaterShellTopMargin();
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    private void removeFloaterShellView() {
        if (floaterShellView == null) {
            return;
        }
        floaterShellView.removeOnLayoutChangeListener(floaterShellLayoutChangeListener);
        if (floaterShellOverlayContainer != null) {
            floaterShellOverlayContainer.removeView(floaterShellView);
        }
    }

    private void clearAttachedReferences() {
        floaterShellView = null;
        searchInput = null;
        searchButton = null;
        floaterStationListFragment = null;
        contentPaddingView = null;
    }

    private int resolveTopContentPaddingPx() {
        return contentPaddingBaseTopPx + statusBarInsetPx + contentPaddingTopGapPx;
    }

    private int resolveBottomContentPaddingPx() {
        return contentPaddingBaseBottomPx + bottomContentFilterHeightPx;
    }

    private void applyContentPaddingIfChanged(int left, int top, int right, int bottom) {
        if (contentPaddingView == null) {
            return;
        }
        if (contentPaddingView.getPaddingLeft() == left
                && contentPaddingView.getPaddingTop() == top
                && contentPaddingView.getPaddingRight() == right
                && contentPaddingView.getPaddingBottom() == bottom) {
            return;
        }
        contentPaddingView.setPadding(left, top, right, bottom);
    }
}
