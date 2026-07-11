package com.matijakljajic.freeairradio.ui.stations;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.RadioBrowserRepository;
import com.matijakljajic.freeairradio.data.repository.StationRepository;
import com.matijakljajic.freeairradio.ui.ShellChromeAwareFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class StationFeedFragment extends ShellChromeAwareFragment implements StationAdapter.OnStationInteractionListener {

    @Nullable
    private StationRepository stationRepository;
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
    private StationAdapter stationAdapter;
    private int requestSequence;

    protected final void bindStationFeed(@NonNull View rootView,
                                         int recyclerViewId,
                                         int loadingViewId,
                                         int errorContainerViewId,
                                         int errorTextViewId,
                                         int emptyViewId,
                                         int retryButtonId,
                                         @NonNull Runnable retryAction) {
        loadingView = rootView.findViewById(loadingViewId);
        errorContainerView = rootView.findViewById(errorContainerViewId);
        errorTextView = rootView.findViewById(errorTextViewId);
        emptyView = rootView.findViewById(emptyViewId);
        recyclerView = rootView.findViewById(recyclerViewId);

        Button retryButton = rootView.findViewById(retryButtonId);
        retryButton.setOnClickListener(v -> retryAction.run());

        if (recyclerView != null) {
            stationAdapter = new StationAdapter(this);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(stationAdapter);
            recyclerView.setNestedScrollingEnabled(false);
            if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
            }
            recyclerView.setClipToPadding(false);
        }
    }

    protected final void clearStationFeed() {
        stationRepository = null;
        loadingView = null;
        errorContainerView = null;
        errorTextView = null;
        emptyView = null;
        recyclerView = null;
        stationAdapter = null;
    }

    protected final void loadStations(@NonNull StationLoadOperation loadOperation,
                                      @StringRes int emptyMessageResId,
                                      @StringRes int errorMessageResId) {
        int requestId = ++requestSequence;
        renderState(ListUiState.LOADING, 0);
        loadOperation.load(getStationRepository(), new StationRepository.LoadCallback() {
            @Override
            public void onStationsLoaded(@NonNull List<Station> loadedStations) {
                if (isStaleRequest(requestId)) {
                    return;
                }
                showStations(loadedStations, emptyMessageResId);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                if (isStaleRequest(requestId)) {
                    return;
                }
                renderState(ListUiState.ERROR, errorMessageResId);
            }
        });
    }

    protected final void loadTopStations(@StringRes int emptyMessageResId,
                                         @StringRes int errorMessageResId) {
        loadStations(
                StationRepository::loadTopStations,
                emptyMessageResId,
                errorMessageResId
        );
    }

    protected final void loadStationsByName(@NonNull String query,
                                            @StringRes int emptyMessageResId,
                                            @StringRes int errorMessageResId) {
        loadStations(
                (repository, callback) -> repository.searchStationsByName(query, callback),
                emptyMessageResId,
                errorMessageResId
        );
    }

    protected final void showIdle(@StringRes int idleMessageResId) {
        requestSequence++;
        renderState(ListUiState.IDLE, idleMessageResId);
    }

    @NonNull
    protected StationRepository getStationRepository() {
        if (stationRepository == null) {
            stationRepository = createStationRepository();
        }
        return stationRepository;
    }

    @NonNull
    protected StationAdapter getStationAdapter() {
        if (stationAdapter == null) {
            throw new IllegalStateException("Station feed is not bound");
        }
        return stationAdapter;
    }

    @NonNull
    protected RecyclerView getRecyclerView() {
        if (recyclerView == null) {
            throw new IllegalStateException("Station feed is not bound");
        }
        return recyclerView;
    }

    @Nullable
    protected final RecyclerView peekRecyclerView() {
        return recyclerView;
    }

    @NonNull
    protected StationRepository createStationRepository() {
        return new RadioBrowserRepository(requireContext().getApplicationContext());
    }

    @Override
    public void onStationClick(Station station) {
        if (requireActivity() instanceof StationListFragment.OnStationSelectedListener) {
            ((StationListFragment.OnStationSelectedListener) requireActivity()).onStationSelected(station);
        }
    }

    @Override
    public void onStationLongClick(Station station) {
        StationDetailsFragment.newInstance(station)
                .show(getChildFragmentManager(), "station_details");
    }

    private boolean isStaleRequest(int requestId) {
        return requestId != requestSequence || !isAdded();
    }

    private void showStations(@NonNull List<Station> loadedStations, @StringRes int emptyMessageResId) {
        if (stationAdapter != null) {
            stationAdapter.submitList(new ArrayList<>(loadedStations));
        }
        if (loadedStations.isEmpty()) {
            renderState(ListUiState.EMPTY, emptyMessageResId);
        } else {
            renderState(ListUiState.CONTENT, 0);
        }
    }

    private void renderState(@NonNull ListUiState state, @StringRes int messageResId) {
        if (stationAdapter != null && state != ListUiState.CONTENT) {
            stationAdapter.submitList(Collections.emptyList());
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
    }

    @FunctionalInterface
    protected interface StationLoadOperation {
        void load(@NonNull StationRepository repository, @NonNull StationRepository.LoadCallback callback);
    }

    private enum ListUiState {
        LOADING,
        ERROR,
        EMPTY,
        IDLE,
        CONTENT
    }
}
