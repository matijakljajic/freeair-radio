package com.matijakljajic.freeairradio.ui.stations;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.RadioBrowserRepository;
import com.matijakljajic.freeairradio.data.repository.StationRepository;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StationListFragment extends Fragment implements StationAdapter.OnStationInteractionListener {

    private static final String ARG_MODE = "arg_mode";
    private static final String STATE_QUERY = "state_query";

    public interface OnStationSelectedListener {
        void onStationSelected(Station station);
    }

    @Nullable
    private OnStationSelectedListener listener;
    @Nullable
    private StationRepository stationRepository;
    @Nullable
    private View rootView;
    @Nullable
    private ProgressBar loadingView;
    @Nullable
    private View errorContainerView;
    @Nullable
    private TextView errorTextView;
    @Nullable
    private TextView emptyView;
    @Nullable
    private RecyclerView recyclerView;
    @Nullable
    private View playerShellView;
    @Nullable
    private StationAdapter stationAdapter;
    private String currentQuery = "";
    @NonNull
    private Mode mode = Mode.HOME;
    private int searchTopPaddingPx;
    private int bottomRecyclerGapPx;
    private int topSystemInset;
    private int requestSequence;
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnStationSelectedListener) {
            listener = (OnStationSelectedListener) context;
        } else {
            throw new IllegalStateException("Host activity must implement OnStationSelectedListener");
        }
    }

    public void setSearchTopPaddingPx(int searchTopPaddingPx) {
        int sanitizedPaddingPx = Math.max(0, searchTopPaddingPx);
        if (this.searchTopPaddingPx == sanitizedPaddingPx) {
            return;
        }
        this.searchTopPaddingPx = sanitizedPaddingPx;
        updateRootPadding(recyclerView != null && recyclerView.getVisibility() != View.VISIBLE);
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
        loadingView = view.findViewById(R.id.station_list_loading_view);
        errorContainerView = view.findViewById(R.id.station_list_error_container);
        errorTextView = view.findViewById(R.id.station_list_error_text);
        emptyView = view.findViewById(R.id.station_list_empty_view);
        recyclerView = view.findViewById(R.id.station_list_recycler_view);

        Button retryButton = view.findViewById(R.id.station_list_retry_button);
        retryButton.setOnClickListener(v -> loadCurrentQuery());

        assert recyclerView != null;
        stationAdapter = new StationAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(stationAdapter);
        if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        recyclerView.setClipToPadding(false);

        assert rootView != null;
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topSystemInset = mode.shouldApplyTopInset() ? systemBars.top : 0;
            updateRootPadding(recyclerView != null && recyclerView.getVisibility() != View.VISIBLE);
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
            showIdle();
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
                showIdle();
                return;
            }
            loadCurrentQuery();
        }
    }

    @Override
    public void onStationClick(Station station) {
        if (listener != null) {
            listener.onStationSelected(station);
        }
    }

    @Override
    public void onStationLongClick(Station station) {
        StationDetailsFragment.newInstance(station)
                .show(getChildFragmentManager(), "station_details");
    }

    @Override
    public void onDestroyView() {
        if (playerShellView != null) {
            playerShellView.removeOnLayoutChangeListener(playerShellLayoutChangeListener);
            playerShellView = null;
        }
        rootView = null;
        loadingView = null;
        errorContainerView = null;
        errorTextView = null;
        emptyView = null;
        recyclerView = null;
        stationAdapter = null;
        super.onDestroyView();
    }

    private void loadCurrentQuery() {
        if (currentQuery.isEmpty()) {
            loadTopStations();
        } else {
            loadStationsByQuery(currentQuery);
        }
    }

    private void loadTopStations() {
        requestSequence++;
        int requestId = requestSequence;
        renderState(ListUiState.LOADING, 0);
        getStationRepository().loadTopStations(new StationRepository.LoadCallback() {
            @Override
            public void onStationsLoaded(@NonNull List<Station> loadedStations) {
                if (isStaleRequest(requestId)) {
                    return;
                }
                showStations(loadedStations);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                if (isStaleRequest(requestId)) {
                    return;
                }
                renderState(ListUiState.ERROR, R.string.station_list_error);
            }
        });
    }

    private void loadStationsByQuery(@NonNull String query) {
        requestSequence++;
        int requestId = requestSequence;
        renderState(ListUiState.LOADING, 0);
        getStationRepository().searchStationsByName(query, new StationRepository.LoadCallback() {
            @Override
            public void onStationsLoaded(@NonNull List<Station> loadedStations) {
                if (isStaleRequest(requestId)) {
                    return;
                }
                showStations(loadedStations);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                if (isStaleRequest(requestId)) {
                    return;
                }
                renderState(ListUiState.ERROR, R.string.station_list_error);
            }
        });
    }

    private boolean isStaleRequest(int requestId) {
        return requestId != requestSequence || !isAdded();
    }

    private void showStations(@NonNull List<Station> loadedStations) {
        if (stationAdapter != null) {
            stationAdapter.submitList(new ArrayList<>(loadedStations));
        }
        if (loadedStations.isEmpty()) {
            renderState(ListUiState.EMPTY, R.string.station_list_empty);
        } else {
            renderState(ListUiState.CONTENT, 0);
        }
    }

    private void showIdle() {
        requestSequence++;
        renderState(ListUiState.IDLE, R.string.station_search_idle);
    }

    @NonNull
    protected StationRepository createStationRepository() {
        return new RadioBrowserRepository();
    }

    @NonNull
    private StationRepository getStationRepository() {
        if (stationRepository == null) {
            stationRepository = createStationRepository();
        }
        return stationRepository;
    }

    private void renderState(@NonNull ListUiState state, @StringRes int messageResId) {
        if (stationAdapter != null) {
            if (state != ListUiState.CONTENT) {
                stationAdapter.submitList(Collections.emptyList());
            }
        }
        if (loadingView != null) {
            loadingView.setVisibility(state == ListUiState.LOADING ? View.VISIBLE : View.GONE);
        }
        if (errorContainerView != null) {
            errorContainerView.setVisibility(state == ListUiState.ERROR ? View.VISIBLE : View.GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(state == ListUiState.EMPTY || state == ListUiState.IDLE ? View.VISIBLE : View.GONE);
            if (state == ListUiState.EMPTY || state == ListUiState.IDLE) {
                emptyView.setText(messageResId);
            }
        }
        if (errorTextView != null && state == ListUiState.ERROR) {
            errorTextView.setText(messageResId);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(state == ListUiState.CONTENT ? View.VISIBLE : View.GONE);
        }
        updateRootPadding(state != ListUiState.CONTENT);
        updateRecyclerPadding();
    }

    private void updateRootPadding(boolean applyTopInset) {
        if (rootView == null) {
            return;
        }

        rootView.setPadding(
                rootView.getPaddingLeft(),
                applyTopInset ? getTopContentPaddingPx() : 0,
                rootView.getPaddingRight(),
                rootView.getPaddingBottom()
        );
    }

    private void updateRecyclerPadding() {
        if (recyclerView == null) {
            return;
        }

        recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                getTopContentPaddingPx(),
                recyclerView.getPaddingRight(),
                getBottomRecyclerPaddingPx()
        );
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

    private enum ListUiState {
        LOADING,
        ERROR,
        EMPTY,
        IDLE,
        CONTENT
    }
}
