package com.matijakljajic.freeairradio.ui.shell;

import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.transition.ChangeBounds;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.stations.StationSearchResultsFragment;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

public final class AppShellController {

    private static final long TRANSITION_DURATION_MS = 300L;

    @NonNull
    private final ViewGroup rootView;
    @NonNull
    private final View statusBarFilterView;
    @NonNull
    private final View bottomContentFilterView;
    @NonNull
    private final View bottomControlsContainerView;
    @NonNull
    private final ViewGroup searchOverlayContainer;
    private final View.OnLayoutChangeListener bottomControlsLayoutChangeListener;
    private final View.OnLayoutChangeListener searchBarLayoutChangeListener;
    @Nullable
    private MaterialCardView searchBarView;
    @Nullable
    private EditText searchInput;
    @Nullable
    private View searchButton;
    @Nullable
    private StationSearchResultsFragment searchResultsFragment;
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
    private int searchBarBaseTopMarginPx;
    private boolean searchBarVisible;

    public AppShellController(@NonNull ViewGroup rootView,
                              @NonNull View statusBarFilterView,
                              @NonNull View bottomContentFilterView,
                              @NonNull View bottomControlsContainerView,
                              @NonNull ViewGroup searchOverlayContainer) {
        this.rootView = rootView;
        this.statusBarFilterView = statusBarFilterView;
        this.bottomContentFilterView = bottomContentFilterView;
        this.bottomControlsContainerView = bottomControlsContainerView;
        this.searchOverlayContainer = searchOverlayContainer;
        this.bottomControlsLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateBottomContentFilter();
        this.searchBarLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateSearchBarLayout();
    }

    public void attach() {
        attachBottomControlsListener();
        bindSearchBar();
        installWindowInsetsListener();
        updateTopContentFilter();
        updateBottomContentFilter();
        applySearchBarVisibility(false);
        updateSearchBarLayout();
    }

    public void detach() {
        detachBottomControlsListener();
        unbindSearchBar();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, null);
        clearAttachedReferences();
    }

    public void setSearchBarVisible(boolean visible) {
        if (searchBarVisible == visible) {
            return;
        }
        searchBarVisible = visible;
        applySearchBarVisibility(true);
        updateSearchBarLayout();
        if (!visible) {
            setTopContentFilterHeightPx(0);
        }
    }

    public void setSearchResultsFragment(@Nullable StationSearchResultsFragment searchResultsFragment) {
        this.searchResultsFragment = searchResultsFragment;
        updateSearchBarLayout();
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

    private void bindSearchBar() {
        if (searchBarView != null) {
            return;
        }

        searchBarView = searchOverlayContainer.findViewById(R.id.station_search_bar);
        if (searchBarView == null) {
            return;
        }
        searchInput = searchBarView.findViewById(R.id.station_search_input);
        searchButton = searchBarView.findViewById(R.id.station_search_button);
        searchBarBaseTopMarginPx = getTopMargin(searchBarView);
        searchBarView.addOnLayoutChangeListener(searchBarLayoutChangeListener);
    }

    private void applySearchBarVisibility(boolean animate) {
        if (searchBarView == null) {
            return;
        }
        if (!animate) {
            searchBarView.setVisibility(searchBarVisible ? View.VISIBLE : View.GONE);
            return;
        }
        setSearchBarVisibilityWithTransition(searchBarView, searchBarVisible);
    }

    private void setSearchBarVisibilityWithTransition(@NonNull View searchBar, boolean visible) {
        TransitionManager.beginDelayedTransition(searchOverlayContainer, createSearchBarTransition());
        searchBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private Transition createSearchBarTransition() {
        Slide slide = new Slide(Gravity.TOP);
        slide.setDuration(TRANSITION_DURATION_MS);
        return slide;
    }

    private void updateSearchBarLayout() {
        if (searchBarView == null) {
            return;
        }

        if (!searchBarVisible || searchResultsFragment == null) {
            setTopContentFilterHeightPx(0);
            return;
        }

        int desiredTopPaddingPx = searchBarView.getBottom() + UiDimensions.px(rootView.getContext(), R.dimen.search_list_gap);
        searchResultsFragment.setSearchTopPaddingPx(desiredTopPaddingPx);
        searchResultsFragment.setBottomRecyclerGapPx(UiDimensions.px(rootView.getContext(), R.dimen.search_list_bottom_gap));
        setTopContentFilterHeightPx(desiredTopPaddingPx);
    }

    private void applySearchBarTopMargin() {
        setSearchBarTopMargin(statusBarInsetPx + searchBarBaseTopMarginPx);
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
        int desiredHeight = Math.max(
                Math.round(statusBarInsetPx * 1.5f),
                topContentFilterHeightPx
        );
        animateFilterHeight(statusBarFilterView, desiredHeight);
    }

    private void updateBottomContentFilter() {
        int desiredHeight = bottomControlsContainerView.getHeight()
                + getBottomControlsBottomMarginPx()
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

    private int getBottomControlsBottomMarginPx() {
        ViewGroup.LayoutParams layoutParams = bottomControlsContainerView.getLayoutParams();
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
        transition.setDuration(TRANSITION_DURATION_MS);
        TransitionManager.beginDelayedTransition(rootView, transition);
        layoutParams.height = desiredHeight;
        filterView.setLayoutParams(layoutParams);
        filterView.setVisibility(desiredHeight > 0 ? View.VISIBLE : View.GONE);
    }

    private void attachBottomControlsListener() {
        bottomControlsContainerView.addOnLayoutChangeListener(bottomControlsLayoutChangeListener);
    }

    private void detachBottomControlsListener() {
        bottomControlsContainerView.removeOnLayoutChangeListener(bottomControlsLayoutChangeListener);
    }

    private void installWindowInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            statusBarInsetPx = systemBars.top;
            updateTopContentFilter();
            updateContentPadding();
            applySearchBarTopMargin();
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    private void unbindSearchBar() {
        if (searchBarView == null) {
            return;
        }
        searchBarView.removeOnLayoutChangeListener(searchBarLayoutChangeListener);
        setSearchBarTopMargin(searchBarBaseTopMarginPx);
    }

    private void clearAttachedReferences() {
        searchBarView = null;
        searchInput = null;
        searchButton = null;
        searchResultsFragment = null;
        contentPaddingView = null;
        searchBarBaseTopMarginPx = 0;
    }

    private int resolveTopContentPaddingPx() {
        return contentPaddingBaseTopPx + statusBarInsetPx + contentPaddingTopGapPx;
    }

    private int getTopMargin(@NonNull View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        return layoutParams instanceof ViewGroup.MarginLayoutParams
                ? ((ViewGroup.MarginLayoutParams) layoutParams).topMargin
                : 0;
    }

    private void setSearchBarTopMargin(int topMarginPx) {
        if (searchBarView == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = searchBarView.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        if (marginLayoutParams.topMargin == topMarginPx) {
            return;
        }

        marginLayoutParams.topMargin = topMarginPx;
        searchBarView.setLayoutParams(marginLayoutParams);
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
