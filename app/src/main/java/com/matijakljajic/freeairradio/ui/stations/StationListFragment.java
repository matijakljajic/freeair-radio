package com.matijakljajic.freeairradio.ui.stations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

public class StationListFragment extends StationFeedFragment {

    private static final String ARG_MODE = "arg_mode";
    private static final String STATE_QUERY = "state_query";

    public interface OnStationSelectedListener {
        void onStationSelected(@NonNull com.matijakljajic.freeairradio.data.model.Station station);
    }

    @Nullable
    private View rootView;
    @Nullable
    private View playerShellView;
    @NonNull
    private Mode mode = Mode.HOME;
    @NonNull
    private String currentQuery = "";
    private int searchTopPaddingPx;
    private int bottomRecyclerGapPx;
    private int topSystemInset;
    private final View.OnLayoutChangeListener playerShellLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateRecyclerPadding();

    public static StationListFragment newHomeInstance() {
        StationListFragment fragment = new StationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, Mode.HOME.name());
        fragment.setArguments(args);
        return fragment;
    }

    public static StationListFragment newSearchInstance() {
        StationListFragment fragment = new StationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, Mode.SEARCH.name());
        fragment.setArguments(args);
        return fragment;
    }

    public void setSearchTopPaddingPx(int searchTopPaddingPx) {
        int sanitizedPaddingPx = Math.max(0, searchTopPaddingPx);
        if (this.searchTopPaddingPx == sanitizedPaddingPx) {
            return;
        }
        this.searchTopPaddingPx = sanitizedPaddingPx;
        updateRootPadding(recyclerVisible());
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
        Bundle args = getArguments();
        if (args != null) {
            mode = Mode.fromName(args.getString(ARG_MODE, Mode.HOME.name()));
        }
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

        assert rootView != null;
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topSystemInset = mode.shouldApplyTopInset() ? systemBars.top : 0;
            updateRootPadding(recyclerVisible());
            updateRecyclerPadding();
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);

        playerShellView = requireActivity().findViewById(R.id.player_shell_container);
        if (playerShellView != null) {
            playerShellView.addOnLayoutChangeListener(playerShellLayoutChangeListener);
        }

        if (mode.shouldLoadOnStart()) {
            view.post(this::loadCurrentQuery);
        } else {
            showIdle(R.string.station_search_idle);
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
            if (currentQuery.isEmpty() && !mode.shouldLoadOnStart()) {
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
            loadTopStations(R.string.station_list_empty, R.string.station_list_error);
        } else {
            loadStationsByName(currentQuery, R.string.station_list_empty, R.string.station_list_error);
        }
    }

    private void updateRootPadding(boolean recyclerVisible) {
        if (rootView == null) {
            return;
        }

        int desiredTopPaddingPx = 0;
        int desiredBottomPaddingPx = 0;
        if (mode.shouldApplyTopInset()) {
            if (!recyclerVisible) {
                desiredTopPaddingPx = getTopContentPaddingPx();
                desiredBottomPaddingPx = getBottomRecyclerPaddingPx();
            }
        } else {
            desiredTopPaddingPx = getTopContentPaddingPx();
            desiredBottomPaddingPx = getBottomRecyclerPaddingPx();
        }

        rootView.setPadding(
                rootView.getPaddingLeft(),
                desiredTopPaddingPx,
                rootView.getPaddingRight(),
                desiredBottomPaddingPx
        );
    }

    private void updateRecyclerPadding() {
        View recyclerView = peekRecyclerView();
        if (recyclerView == null) {
            return;
        }

        recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                0,
                recyclerView.getPaddingRight(),
                0
        );
    }

    private boolean recyclerVisible() {
        View recyclerView = peekRecyclerView();
        return recyclerView != null && recyclerView.getVisibility() == View.VISIBLE;
    }

    private int getTopContentPaddingPx() {
        if (mode.shouldApplyTopInset()) {
            return topSystemInset + UiDimensions.px(requireContext(), R.dimen.top_content_gap);
        }
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

    private enum Mode {
        HOME(true, true),
        SEARCH(false, false);

        private final boolean applyTopInset;
        private final boolean loadOnStart;

        Mode(boolean applyTopInset, boolean loadOnStart) {
            this.applyTopInset = applyTopInset;
            this.loadOnStart = loadOnStart;
        }

        static Mode fromName(@NonNull String name) {
            for (Mode mode : values()) {
                if (mode.name().equals(name)) {
                    return mode;
                }
            }
            return HOME;
        }

        boolean shouldApplyTopInset() {
            return applyTopInset;
        }

        boolean shouldLoadOnStart() {
            return loadOnStart;
        }
    }
}
