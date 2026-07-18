package com.matijakljajic.freeairradio.ui.homepage;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
import com.matijakljajic.freeairradio.data.repository.LibraryRepository;
import com.matijakljajic.freeairradio.ui.localstations.LocalStationEditorFragment;
import com.matijakljajic.freeairradio.ui.settings.HomePageSettings;
import com.matijakljajic.freeairradio.ui.stations.StationAdapter;
import com.matijakljajic.freeairradio.ui.stations.StationFeedFragment;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

import java.util.List;

import android.widget.Toast;

@SuppressWarnings("unused")
public class HomePageFragment extends StationFeedFragment {

    private static final String STATE_SOURCE = "homepage_source";

    @NonNull
    private LibraryRepository libraryRepository;
    @NonNull
    private final LibraryRepository.FavoritesListener favoritesListener = this::refreshFavoritesIfVisible;
    @NonNull
    private HomePageSettings homePageSettings;

    @Nullable
    private RecyclerView homepageRecyclerView;
    @Nullable
    private PopupWindow sourcePopupWindow;
    @Nullable
    private HomePageHeaderAdapter headerAdapter;
    @Nullable
    private ItemTouchHelper favoritesReorderTouchHelper;
    private boolean favoriteOrderDirty;
    private boolean favoriteOrderPersisting;
    @Nullable
    private List<Station> pendingFavoriteOrder;
    @NonNull
    private HomePageSource currentSource = HomePageSource.NOW_POPULAR;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_homepage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homePageSettings = new HomePageSettings(requireContext());
        currentSource = resolveInitialSource(savedInstanceState);
        libraryRepository = LibraryRepository.getInstance(requireContext());
        getChildFragmentManager().setFragmentResultListener(
                LocalStationEditorFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> onLocalStationEditorResult(result)
        );
        headerAdapter = new HomePageHeaderAdapter(new HomePageHeaderAdapter.Listener() {
            @Override
            public void onSourceClicked(@NonNull View anchorView) {
                showSourceMenu(anchorView);
            }

            @Override
            public void onAddStationClicked() {
                handleAddStationClick();
            }
        });
        headerAdapter.setTitleResId(currentSource.getTitleResId());
        bindStationFeed(
                view,
                R.id.station_feed_recycler_view,
                R.id.station_feed_loading_view,
                R.id.station_feed_error_container,
                R.id.station_feed_error_text,
                R.id.station_feed_empty_view,
                R.id.station_feed_retry_button,
                this::loadHomepageStations
        );
        homepageRecyclerView = getRecyclerView();
        attachShellContentPadding(
                homepageRecyclerView,
                UiDimensions.px(requireContext(), R.dimen.top_content_gap)
        );
        bindFavoritesReorder();
        homepageRecyclerView.post(this::updateHeaderStateInset);
        libraryRepository.addFavoritesListener(favoritesListener);

        view.post(this::loadHomepageStations);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SOURCE, currentSource.name());
    }

    @Override
    public void onDestroyView() {
        libraryRepository.removeFavoritesListener(favoritesListener);
        dismissSourcePopup();
        if (homepageRecyclerView != null) {
            detachShellContentPadding(homepageRecyclerView);
        }
        if (favoritesReorderTouchHelper != null && homepageRecyclerView != null) {
            favoritesReorderTouchHelper.attachToRecyclerView(null);
        }
        homepageRecyclerView = null;
        favoritesReorderTouchHelper = null;
        favoriteOrderDirty = false;
        clearFavoriteReorderState();
        headerAdapter = null;
        setStateContainerTopInsetPx(0);
        clearStationFeed();
        super.onDestroyView();
    }

    private void loadHomepageStations() {
        switch (currentSource) {
            case FAVORITES:
                if (pendingFavoriteOrder != null) {
                    displayStations(
                            pendingFavoriteOrder,
                            R.string.station_list_empty_favorites
                    );
                    return;
                }
                loadStations(
                        (repository, callback) -> libraryRepository.loadFavoriteStations(callback),
                        R.string.station_list_empty_favorites,
                        R.string.station_list_error
                );
                return;
            case LOCAL_STATIONS:
                loadStations(
                        (repository, callback) -> libraryRepository.loadLocalStations(callback),
                        R.string.station_list_empty_local_stations,
                        R.string.station_list_error
                );
                return;
            case NOW_POPULAR:
            default:
                loadTopStations(R.string.station_list_empty, R.string.station_list_error);
        }
    }

    private void showSourceMenu(@NonNull View anchorView) {
        dismissSourcePopup();

        List<HomePageSource> selectableSources = getSelectableSources();
        if (selectableSources.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        ViewGroup popupAnchorParent = homepageRecyclerView != null
                ? homepageRecyclerView
                : (ViewGroup) requireActivity().findViewById(android.R.id.content);
        View popupRootView = inflater.inflate(
                R.layout.view_homepage_source_dropdown,
                popupAnchorParent,
                false
        );
        ViewGroup popupContent = popupRootView.findViewById(R.id.homepage_source_dropdown_options);
        popupRootView.setMinimumWidth(measureDropdownMinWidth(inflater, popupContent));

        PopupWindow popupWindow = new PopupWindow(
                popupRootView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);

        for (HomePageSource source : selectableSources) {
            View optionView = inflater.inflate(
                    R.layout.view_homepage_source_dropdown_option,
                    popupContent,
                    false
            );
            TextView optionTextView = optionView.findViewById(R.id.homepage_source_option_text);
            optionTextView.setText(source.getTitleResId());
            optionView.setOnClickListener(v -> {
                popupWindow.dismiss();
                switchSource(source);
            });
            popupContent.addView(optionView);
        }

        sourcePopupWindow = popupWindow;
        popupWindow.setOnDismissListener(() -> {
            if (sourcePopupWindow == popupWindow) {
                sourcePopupWindow = null;
            }
        });
        popupWindow.showAsDropDown(
                anchorView,
                -UiDimensions.px(requireContext(), R.dimen.homepage_source_dropdown_horizontal_padding),
                0
        );
    }

    private int measureDropdownMinWidth(@NonNull LayoutInflater inflater,
                                        @NonNull ViewGroup popupContent) {
        int longestOptionWidth = 0;
        for (HomePageSource source : HomePageSource.values()) {
            View optionView = inflater.inflate(
                    R.layout.view_homepage_source_dropdown_option,
                    popupContent,
                    false
            );
            TextView optionTextView = optionView.findViewById(R.id.homepage_source_option_text);
            optionTextView.setText(source.getTitleResId());
            optionView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            longestOptionWidth = Math.max(longestOptionWidth, optionView.getMeasuredWidth());
        }
        return longestOptionWidth + popupContent.getPaddingLeft() + popupContent.getPaddingRight();
    }

    private void switchSource(@NonNull HomePageSource source) {
        if (currentSource == source) {
            return;
        }

        if (source != HomePageSource.FAVORITES && getStationAdapter().isDragReordering()) {
            getStationAdapter().clearDragReorderPreview();
        }
        currentSource = source;
        if (headerAdapter != null) {
            headerAdapter.setTitleResId(source.getTitleResId());
        }
        loadHomepageStations();
    }

    @NonNull
    private HomePageSource resolveInitialSource(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return homePageSettings.getDefaultSource();
        }

        String sourceName = savedInstanceState.getString(STATE_SOURCE);
        if (sourceName == null) {
            return homePageSettings.getDefaultSource();
        }

        try {
            return HomePageSource.valueOf(sourceName);
        } catch (IllegalArgumentException exception) {
            return homePageSettings.getDefaultSource();
        }
    }

    @NonNull
    private List<HomePageSource> getSelectableSources() {
        List<HomePageSource> selectableSources = new java.util.ArrayList<>();
        for (HomePageSource source : HomePageSource.values()) {
            if (source != currentSource) {
                selectableSources.add(source);
            }
        }
        return selectableSources;
    }

    private void dismissSourcePopup() {
        if (sourcePopupWindow != null) {
            sourcePopupWindow.dismiss();
            sourcePopupWindow = null;
        }
    }

    private void refreshFavoritesIfVisible() {
        View view = getView();
        if (view == null || currentSource != HomePageSource.FAVORITES) {
            return;
        }
        view.post(() -> {
            if (!isAdded() || currentSource != HomePageSource.FAVORITES) {
                return;
            }
            if (!libraryRepository.hasLoadedFavorites()) {
                return;
            }
            if (favoriteOrderPersisting) {
                return;
            }
            displayStations(
                    libraryRepository.getFavoriteStationsSnapshot(),
                    R.string.station_list_empty_favorites
            );
        });
    }

    private void handleAddStationClick() {
        LocalStationEditorFragment.newCreateInstance()
                .show(getChildFragmentManager(), "local_station_editor");
    }

    @Override
    public void onStationLongClick(Station station) {
        if (station.getOrigin() == StationOrigin.LOCAL_USER) {
            LocalStationEditorFragment.newEditInstance(station)
                    .show(getChildFragmentManager(), "local_station_editor");
            return;
        }

        super.onStationLongClick(station);
    }

    private void onLocalStationEditorResult(@NonNull Bundle result) {
        if (result.getBoolean(LocalStationEditorFragment.RESULT_KEY_OPEN_LOCAL_SOURCE, false)) {
            if (currentSource == HomePageSource.LOCAL_STATIONS) {
                loadHomepageStations();
                return;
            }
            switchSource(HomePageSource.LOCAL_STATIONS);
            return;
        }

        if (currentSource == HomePageSource.LOCAL_STATIONS) {
            loadHomepageStations();
        }
    }

    @NonNull
    @Override
    protected RecyclerView.Adapter<?> createRecyclerAdapter(@NonNull StationAdapter stationAdapter) {
        if (headerAdapter == null) {
            return stationAdapter;
        }
        return new ConcatAdapter(headerAdapter, stationAdapter);
    }

    @Nullable
    @Override
    protected StationAdapter.DragHandleListener createStationDragHandleListener() {
        return (station, viewHolder) -> {
            if (currentSource != HomePageSource.FAVORITES || favoritesReorderTouchHelper == null) {
                return false;
            }
            getStationAdapter().beginDragReorder();
            favoritesReorderTouchHelper.startDrag(viewHolder);
            return true;
        };
    }

    @Override
    protected boolean keepsRecyclerVisibleDuringStateViews() {
        return true;
    }

    @Override
    protected boolean shouldCrossfadeStationListUpdates() {
        return false;
    }

    private void bindFavoritesReorder() {
        if (homepageRecyclerView == null) {
            return;
        }

        favoritesReorderTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                if (currentSource != HomePageSource.FAVORITES
                        || !(viewHolder instanceof StationAdapter.StationViewHolder)) {
                    return 0;
                }
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                if (!(target instanceof StationAdapter.StationViewHolder)) {
                    return false;
                }

                int fromPosition = viewHolder.getBindingAdapterPosition();
                int toPosition = target.getBindingAdapterPosition();
                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                    return false;
                }

                getStationAdapter().moveDraggedStation(fromPosition, toPosition);
                favoriteOrderDirty = true;
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (!favoriteOrderDirty || currentSource != HomePageSource.FAVORITES) {
                    return;
                }

                favoriteOrderDirty = false;
                persistFavoriteOrder();
            }
        });
        favoritesReorderTouchHelper.attachToRecyclerView(homepageRecyclerView);
    }

    private void persistFavoriteOrder() {
        List<Station> orderedFavorites = getStationAdapter().getDragReorderSnapshot();
        favoriteOrderPersisting = true;
        pendingFavoriteOrder = orderedFavorites;
        libraryRepository.reorderFavoriteStations(
                orderedFavorites,
                new LibraryRepository.WriteCallback() {
                    @Override
                    public void onSuccess() {
                        clearFavoriteReorderState();
                        if (getView() == null) {
                            return;
                        }
                        getStationAdapter().completeDragReorder(orderedFavorites);
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        clearFavoriteReorderState();
                        if (getView() != null) {
                            getStationAdapter().clearDragReorderPreview();
                        }
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), R.string.favorite_reorder_failed, Toast.LENGTH_SHORT).show();
                        loadHomepageStations();
                    }
                }
        );
    }

    private void clearFavoriteReorderState() {
        favoriteOrderPersisting = false;
        pendingFavoriteOrder = null;
    }

    private void updateHeaderStateInset() {
        if (!isAdded() || homepageRecyclerView == null) {
            return;
        }

        int availableWidth = homepageRecyclerView.getWidth()
                - homepageRecyclerView.getPaddingLeft()
                - homepageRecyclerView.getPaddingRight();
        if (availableWidth <= 0) {
            homepageRecyclerView.post(this::updateHeaderStateInset);
            return;
        }

        View headerView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_homepage_header, homepageRecyclerView, false);
        headerView.measure(
                View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int topInsetPx = homepageRecyclerView.getPaddingTop() + headerView.getMeasuredHeight();
        ViewGroup.LayoutParams layoutParams = headerView.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            topInsetPx += marginLayoutParams.topMargin + marginLayoutParams.bottomMargin;
        }
        setStateContainerTopInsetPx(topInsetPx);
    }

}
