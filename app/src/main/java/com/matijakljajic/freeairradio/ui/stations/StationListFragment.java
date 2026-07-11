package com.matijakljajic.freeairradio.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

public class StationListFragment extends StationFeedFragment {

    private static final String STATE_QUERY = "state_query";

    @Nullable
    private View rootView;
    @Nullable
    private View playerShellView;
    @NonNull
    private String currentQuery = "";
    private int searchTopPaddingPx;
    private int bottomRecyclerGapPx;
    private final View.OnLayoutChangeListener playerShellLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateRootPadding();

    public static StationListFragment newSearchInstance() {
        return new StationListFragment();
    }

    public void setSearchTopPaddingPx(int searchTopPaddingPx) {
        int sanitizedPaddingPx = Math.max(0, searchTopPaddingPx);
        if (this.searchTopPaddingPx == sanitizedPaddingPx) {
            return;
        }
        this.searchTopPaddingPx = sanitizedPaddingPx;
        updateRootPadding();
    }

    public void setBottomRecyclerGapPx(int bottomRecyclerGapPx) {
        int sanitizedGapPx = Math.max(0, bottomRecyclerGapPx);
        if (this.bottomRecyclerGapPx == sanitizedGapPx) {
            return;
        }
        this.bottomRecyclerGapPx = sanitizedGapPx;
        updateRootPadding();
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
        return inflater.inflate(R.layout.fragment_station_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view.findViewById(R.id.station_list_root);
        bindStationFeed(
                view,
                R.id.station_feed_recycler_view,
                R.id.station_feed_loading_view,
                R.id.station_feed_error_container,
                R.id.station_feed_error_text,
                R.id.station_feed_empty_view,
                R.id.station_feed_retry_button,
                this::loadCurrentQuery
        );

        playerShellView = requireActivity().findViewById(R.id.player_shell_container);
        if (playerShellView != null) {
            playerShellView.addOnLayoutChangeListener(playerShellLayoutChangeListener);
        }

        updateRootPadding();
        if (currentQuery.isEmpty()) {
            showIdle(R.string.station_search_idle);
        } else {
            view.post(this::loadCurrentQuery);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, currentQuery);
    }

    public void submitQuery(@Nullable String query) {
        currentQuery = normalizeQuery(query);
        if (isAdded()) {
            if (currentQuery.isEmpty()) {
                showIdle(R.string.station_search_idle);
                return;
            }
            loadCurrentQuery();
        }
    }

    @Override
    public void onDestroyView() {
        if (playerShellView != null) {
            playerShellView.removeOnLayoutChangeListener(playerShellLayoutChangeListener);
            playerShellView = null;
        }
        rootView = null;
        clearStationFeed();
        super.onDestroyView();
    }

    private void loadCurrentQuery() {
        if (currentQuery.isEmpty()) {
            showIdle(R.string.station_search_idle);
            return;
        }
        loadStationsByName(currentQuery, R.string.station_list_empty, R.string.station_list_error);
    }

    private void updateRootPadding() {
        if (rootView == null) {
            return;
        }

        rootView.setPadding(
                rootView.getPaddingLeft(),
                getTopContentPaddingPx(),
                rootView.getPaddingRight(),
                getBottomRecyclerPaddingPx()
        );
    }

    private int getTopContentPaddingPx() {
        return searchTopPaddingPx;
    }

    private int getBottomRecyclerPaddingPx() {
        return getPlayerShellHeight() + getBottomRecyclerGapPx();
    }

    private int getBottomRecyclerGapPx() {
        if (bottomRecyclerGapPx > 0) {
            return bottomRecyclerGapPx;
        }
        return UiDimensions.px(requireContext(), R.dimen.list_bottom_padding);
    }

    private int getPlayerShellHeight() {
        if (playerShellView == null) {
            return 0;
        }

        ViewGroup.LayoutParams layoutParams = playerShellView.getLayoutParams();
        int height = playerShellView.getHeight();
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
