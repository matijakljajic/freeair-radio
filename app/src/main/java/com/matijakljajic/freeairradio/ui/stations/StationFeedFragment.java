package com.matijakljajic.freeairradio.ui.stations;

import android.util.Log;
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

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.RadioBrowserRepository;
import com.matijakljajic.freeairradio.data.repository.StationRepository;
import com.matijakljajic.freeairradio.ui.shell.ShellChromeAwareFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public abstract class StationFeedFragment extends ShellChromeAwareFragment implements StationAdapter.OnStationInteractionListener {

    private static final String TAG = "StationFeedFragment";
    private static final long CONTENT_FADE_OUT_DURATION_MS = 150L;
    private static final long CONTENT_FADE_IN_DURATION_MS = 240L;
    private static final float CONTENT_FADE_OUT_ALPHA = 0f;

    public interface OnStationSelectedListener {
        void onStationSelected(@NonNull Station station);
    }

    @Nullable
    private StationRepository stationRepository;
    @Nullable
    private View stateContainerView;
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
    private boolean hasRenderedContent;
    private int stateContainerTopInsetPx;

    protected final void bindStationFeed(@NonNull View rootView,
                                         int recyclerViewId,
                                         int loadingViewId,
                                         int errorContainerViewId,
                                         int errorTextViewId,
                                         int emptyViewId,
                                         int retryButtonId,
                                         @NonNull Runnable retryAction) {
        bindStateViews(rootView, loadingViewId, errorContainerViewId, errorTextViewId, emptyViewId, retryButtonId, retryAction);
        bindRecyclerView(rootView, recyclerViewId);
    }

    protected final void clearStationFeed() {
        stationRepository = null;
        loadingView = null;
        stateContainerView = null;
        errorContainerView = null;
        errorTextView = null;
        emptyView = null;
        if (recyclerView != null) {
            recyclerView.animate().cancel();
            recyclerView.setAlpha(1f);
        }
        recyclerView = null;
        stationAdapter = null;
        hasRenderedContent = false;
        stateContainerTopInsetPx = 0;
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
                renderStations(loadedStations, emptyMessageResId);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                if (isStaleRequest(requestId)) {
                    return;
                }
                Log.w(TAG, "Station load failed", throwable);
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

    protected final void displayStations(@NonNull List<Station> stations,
                                         @StringRes int emptyMessageResId) {
        requestSequence++;
        renderStations(stations, emptyMessageResId);
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

    @NonNull
    protected StationRepository createStationRepository() {
        return new RadioBrowserRepository(requireContext().getApplicationContext());
    }

    @NonNull
    protected RecyclerView.Adapter<?> createRecyclerAdapter(@NonNull StationAdapter stationAdapter) {
        return stationAdapter;
    }

    @Nullable
    protected StationAdapter.DragHandleListener createStationDragHandleListener() {
        return null;
    }

    protected boolean keepsRecyclerVisibleDuringStateViews() {
        return false;
    }

    protected boolean shouldCrossfadeStationListUpdates() {
        return true;
    }

    protected final void setStateContainerTopInsetPx(int topInsetPx) {
        int sanitizedInsetPx = Math.max(0, topInsetPx);
        if (stateContainerTopInsetPx == sanitizedInsetPx) {
            return;
        }
        stateContainerTopInsetPx = sanitizedInsetPx;
        applyStateContainerTopInset();
    }

    @Override
    public void onStationClick(Station station) {
        if (requireActivity() instanceof OnStationSelectedListener) {
            ((OnStationSelectedListener) requireActivity()).onStationSelected(station);
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

    private void renderStations(@NonNull List<Station> loadedStations, @StringRes int emptyMessageResId) {
        List<Station> stations = new ArrayList<>(loadedStations);
        boolean hadVisibleContent = hasVisibleStationContent();

        if (loadedStations.isEmpty()) {
            renderEmptyStations(stations, emptyMessageResId);
            return;
        }

        hasRenderedContent = true;
        if (hadVisibleContent && shouldCrossfadeStationListUpdates()) {
            crossfadeStationList(stations, this::showContentState);
            return;
        }

        submitStationList(stations, () -> {
            prepareRecyclerFadeIn(hadVisibleContent);
            showContentState();
            animateRecyclerFadeInIfNeeded(hadVisibleContent);
        });
    }

    private void renderState(@NonNull ListUiState state, @StringRes int messageResId) {
        boolean keepCurrentStationsVisible = shouldKeepCurrentStationsVisible(state);
        boolean keepRecyclerVisible = shouldKeepRecyclerVisible(state, keepCurrentStationsVisible);

        if (state != ListUiState.LOADING && state != ListUiState.CONTENT) {
            hasRenderedContent = false;
        }

        clearStationListForState(state, keepCurrentStationsVisible);
        updateLoadingVisibility(state, keepCurrentStationsVisible);
        updateStateContainerVisibility(state, keepCurrentStationsVisible);
        updateErrorState(state, messageResId);
        updateEmptyState(state, messageResId);
        updateRecyclerVisibility(state, keepRecyclerVisible);
    }

    private void crossfadeStationList(@NonNull List<Station> stations, @NonNull Runnable onCommitted) {
        RecyclerView currentRecyclerView = recyclerView;
        if (currentRecyclerView == null) {
            submitStationList(stations, onCommitted);
            return;
        }

        RecyclerView.ItemAnimator originalItemAnimator = currentRecyclerView.getItemAnimator();
        currentRecyclerView.animate().cancel();
        currentRecyclerView.animate()
                .alpha(CONTENT_FADE_OUT_ALPHA)
                .setDuration(CONTENT_FADE_OUT_DURATION_MS)
                .withEndAction(() -> {
                    if (recyclerView != currentRecyclerView) {
                        return;
                    }

                    currentRecyclerView.setItemAnimator(null);
                    submitStationList(stations, () -> {
                        if (recyclerView != currentRecyclerView) {
                            return;
                        }
                        onCommitted.run();
                        currentRecyclerView.animate().cancel();
                        currentRecyclerView.animate()
                                .alpha(1f)
                                .setDuration(CONTENT_FADE_IN_DURATION_MS)
                                .withEndAction(() -> currentRecyclerView.setItemAnimator(originalItemAnimator))
                                .start();
                    });
                })
                .start();
    }

    private void submitStationList(@NonNull List<Station> stations, @NonNull Runnable onCommitted) {
        if (stationAdapter == null) {
            onCommitted.run();
            return;
        }
        stationAdapter.submitList(stations, onCommitted);
    }

    private void bindStateViews(@NonNull View rootView,
                                int loadingViewId,
                                int errorContainerViewId,
                                int errorTextViewId,
                                int emptyViewId,
                                int retryButtonId,
                                @NonNull Runnable retryAction) {
        loadingView = rootView.findViewById(loadingViewId);
        stateContainerView = rootView.findViewById(R.id.station_feed_state_container);
        errorContainerView = rootView.findViewById(errorContainerViewId);
        errorTextView = rootView.findViewById(errorTextViewId);
        emptyView = rootView.findViewById(emptyViewId);

        Button retryButton = rootView.findViewById(retryButtonId);
        retryButton.setOnClickListener(v -> retryAction.run());
    }

    private void bindRecyclerView(@NonNull View rootView, int recyclerViewId) {
        recyclerView = rootView.findViewById(recyclerViewId);
        if (recyclerView == null) {
            return;
        }

        stationAdapter = new StationAdapter(this, createStationDragHandleListener());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(createRecyclerAdapter(stationAdapter));
        if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        recyclerView.setClipToPadding(false);
    }

    private void renderEmptyStations(@NonNull List<Station> stations, @StringRes int emptyMessageResId) {
        hasRenderedContent = false;
        submitStationList(stations, () -> renderState(ListUiState.EMPTY, emptyMessageResId));
    }

    private void showContentState() {
        renderState(ListUiState.CONTENT, 0);
    }

    private void prepareRecyclerFadeIn(boolean hadVisibleContent) {
        if (hadVisibleContent || recyclerView == null) {
            return;
        }
        recyclerView.animate().cancel();
        recyclerView.setAlpha(0f);
    }

    private void animateRecyclerFadeInIfNeeded(boolean hadVisibleContent) {
        if (hadVisibleContent || recyclerView == null) {
            return;
        }
        recyclerView.animate()
                .alpha(1f)
                .setDuration(CONTENT_FADE_IN_DURATION_MS)
                .start();
    }

    private boolean shouldKeepCurrentStationsVisible(@NonNull ListUiState state) {
        return state == ListUiState.LOADING && hasRenderedContent;
    }

    private boolean shouldKeepRecyclerVisible(@NonNull ListUiState state, boolean keepCurrentStationsVisible) {
        return keepCurrentStationsVisible
                || (state != ListUiState.CONTENT && keepsRecyclerVisibleDuringStateViews());
    }

    private void clearStationListForState(@NonNull ListUiState state, boolean keepCurrentStationsVisible) {
        if (stationAdapter != null && state != ListUiState.CONTENT && !keepCurrentStationsVisible) {
            stationAdapter.submitList(Collections.emptyList());
        }
    }

    private void updateLoadingVisibility(@NonNull ListUiState state, boolean keepCurrentStationsVisible) {
        if (loadingView == null) {
            return;
        }
        loadingView.setVisibility(state == ListUiState.LOADING && !keepCurrentStationsVisible
                ? View.VISIBLE
                : View.GONE);
    }

    private void updateStateContainerVisibility(@NonNull ListUiState state, boolean keepCurrentStationsVisible) {
        if (stateContainerView == null) {
            return;
        }
        applyStateContainerTopInset();
        stateContainerView.setVisibility(state == ListUiState.CONTENT || keepCurrentStationsVisible
                ? View.GONE
                : View.VISIBLE);
    }

    private void updateErrorState(@NonNull ListUiState state, @StringRes int messageResId) {
        if (errorContainerView != null) {
            errorContainerView.setVisibility(state == ListUiState.ERROR ? View.VISIBLE : View.GONE);
        }
        if (errorTextView != null && state == ListUiState.ERROR) {
            errorTextView.setText(messageResId);
        }
    }

    private void updateEmptyState(@NonNull ListUiState state, @StringRes int messageResId) {
        if (emptyView == null) {
            return;
        }
        boolean showEmpty = state == ListUiState.EMPTY || state == ListUiState.IDLE;
        emptyView.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        if (showEmpty) {
            emptyView.setText(messageResId);
        }
    }

    private void updateRecyclerVisibility(@NonNull ListUiState state, boolean keepRecyclerVisible) {
        if (recyclerView == null) {
            return;
        }
        recyclerView.setVisibility(state == ListUiState.CONTENT || keepRecyclerVisible
                ? View.VISIBLE
                : View.GONE);
    }

    private boolean hasVisibleStationContent() {
        return stationAdapter != null
                && !stationAdapter.getCurrentList().isEmpty()
                && recyclerView != null
                && recyclerView.getVisibility() == View.VISIBLE;
    }

    private void applyStateContainerTopInset() {
        if (stateContainerView == null) {
            return;
        }
        int upwardBiasPx = stateContainerView.getResources()
                .getDimensionPixelSize(R.dimen.station_feed_state_vertical_offset);
        stateContainerView.setPadding(
                stateContainerView.getPaddingLeft(),
                stateContainerTopInsetPx,
                stateContainerView.getPaddingRight(),
                stateContainerTopInsetPx + (upwardBiasPx * 2)
        );
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
