package com.matijakljajic.freeairradio.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

@SuppressWarnings("unused")
public class StationSearchResultsFragment extends StationFeedFragment {

    private static final String STATE_QUERY = "state_query";

    @Nullable
    private View bottomControlsView;
    @Nullable
    private RecyclerView stationRecyclerView;
    @NonNull
    private String currentQuery = "";
    private int searchTopPaddingPx;
    private int bottomRecyclerGapPx;
    private final View.OnLayoutChangeListener bottomControlsLayoutChangeListener =
            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateRecyclerPadding();

    public void setSearchTopPaddingPx(int searchTopPaddingPx) {
        int sanitizedPaddingPx = Math.max(0, searchTopPaddingPx);
        if (this.searchTopPaddingPx == sanitizedPaddingPx) {
            return;
        }
        this.searchTopPaddingPx = sanitizedPaddingPx;
        updateRecyclerPadding();
    }

    public void setBottomRecyclerGapPx(int bottomRecyclerGapPx) {
        int sanitizedGapPx = Math.max(0, bottomRecyclerGapPx);
        if (this.bottomRecyclerGapPx == sanitizedGapPx) {
            return;
        }
        this.bottomRecyclerGapPx = sanitizedGapPx;
        updateRecyclerPadding();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString(STATE_QUERY, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_station_feed_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindStationFeed(view, this::refreshCurrentQuery);
        stationRecyclerView = getRecyclerView();
        bindBottomControlsObserver();
        updateRecyclerPadding();
        view.post(this::refreshCurrentQuery);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, currentQuery);
    }

    public void submitQuery(@Nullable String query) {
        currentQuery = normalizeQuery(query);
        if (isAdded()) {
            refreshCurrentQuery();
        }
    }

    @Override
    public void onDestroyView() {
        unbindBottomControlsObserver();
        stationRecyclerView = null;
        clearStationFeed();
        super.onDestroyView();
    }

    private void bindBottomControlsObserver() {
        bottomControlsView = requireActivity().findViewById(R.id.bottom_controls_container);
        if (bottomControlsView != null) {
            bottomControlsView.addOnLayoutChangeListener(bottomControlsLayoutChangeListener);
        }
    }

    private void unbindBottomControlsObserver() {
        if (bottomControlsView != null) {
            bottomControlsView.removeOnLayoutChangeListener(bottomControlsLayoutChangeListener);
            bottomControlsView = null;
        }
    }

    private void refreshCurrentQuery() {
        if (currentQuery.isEmpty()) {
            showIdle(R.string.station_search_idle);
            return;
        }
        loadStationsByName(currentQuery, R.string.station_list_empty, R.string.station_list_error);
    }

    private void updateRecyclerPadding() {
        if (stationRecyclerView == null) {
            return;
        }

        setStateContainerTopInsetPx(searchTopPaddingPx);
        applyRecyclerPadding(searchTopPaddingPx, resolveBottomRecyclerPaddingPx());
    }

    private void applyRecyclerPadding(int topPaddingPx, int bottomPaddingPx) {
        if (stationRecyclerView == null) {
            return;
        }
        stationRecyclerView.setPadding(
                stationRecyclerView.getPaddingLeft(),
                topPaddingPx,
                stationRecyclerView.getPaddingRight(),
                bottomPaddingPx
        );
    }

    private int resolveBottomRecyclerPaddingPx() {
        return getBottomControlsHeight() + resolveBottomRecyclerGapPx();
    }

    private int resolveBottomRecyclerGapPx() {
        if (bottomRecyclerGapPx > 0) {
            return bottomRecyclerGapPx;
        }
        return UiDimensions.px(requireContext(), R.dimen.list_bottom_padding);
    }

    private int getBottomControlsHeight() {
        if (bottomControlsView == null) {
            return 0;
        }

        ViewGroup.LayoutParams layoutParams = bottomControlsView.getLayoutParams();
        int height = bottomControlsView.getHeight();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            height += marginLayoutParams.topMargin + marginLayoutParams.bottomMargin;
        }
        return height;
    }

    @NonNull
    private String normalizeQuery(@Nullable String query) {
        if (query == null) {
            return "";
        }
        return query.trim();
    }
}
