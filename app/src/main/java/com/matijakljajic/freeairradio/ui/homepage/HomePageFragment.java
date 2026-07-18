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
import androidx.recyclerview.widget.RecyclerView;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.repository.LibraryRepository;
import com.matijakljajic.freeairradio.ui.stations.StationAdapter;
import com.matijakljajic.freeairradio.ui.stations.StationFeedFragment;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

import java.util.List;

@SuppressWarnings("unused")
public class HomePageFragment extends StationFeedFragment {

    private static final String STATE_SOURCE = "homepage_source";

    @NonNull
    private LibraryRepository libraryRepository;
    @NonNull
    private final LibraryRepository.FavoritesListener favoritesListener = this::refreshFavoritesIfVisible;

    @Nullable
    private RecyclerView homepageRecyclerView;
    @Nullable
    private PopupWindow sourcePopupWindow;
    @Nullable
    private HomePageHeaderAdapter headerAdapter;
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
        currentSource = HomePageSource.fromSavedState(savedInstanceState);
        libraryRepository = LibraryRepository.getInstance(requireContext());
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
        headerAdapter.setTitleResId(currentSource.titleResId);
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
        homepageRecyclerView = null;
        headerAdapter = null;
        setStateContainerTopInsetPx(0);
        clearStationFeed();
        super.onDestroyView();
    }

    private void loadHomepageStations() {
        if (currentSource == HomePageSource.FAVORITES) {
            loadStations(
                    (repository, callback) -> libraryRepository.loadFavoriteStations(callback),
                    R.string.station_list_empty_favorites,
                    R.string.station_list_error
            );
            return;
        }

        loadTopStations(R.string.station_list_empty, R.string.station_list_error);
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
            optionTextView.setText(source.titleResId);
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
            optionTextView.setText(source.titleResId);
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

        currentSource = source;
        if (headerAdapter != null) {
            headerAdapter.setTitleResId(source.titleResId);
        }
        loadHomepageStations();
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
            displayStations(
                    libraryRepository.getFavoriteStationsSnapshot(),
                    R.string.station_list_empty_favorites
            );
        });
    }

    private void handleAddStationClick() {
        // TODO: Open local station creation when that slice is implemented.
    }

    @NonNull
    @Override
    protected RecyclerView.Adapter<?> createRecyclerAdapter(@NonNull StationAdapter stationAdapter) {
        if (headerAdapter == null) {
            return stationAdapter;
        }
        return new ConcatAdapter(headerAdapter, stationAdapter);
    }

    @Override
    protected boolean keepsRecyclerVisibleDuringStateViews() {
        return true;
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

    private enum HomePageSource {
        NOW_POPULAR(R.string.station_list_title_now_popular),
        FAVORITES(R.string.station_list_title_favorites);

        private final int titleResId;

        HomePageSource(int titleResId) {
            this.titleResId = titleResId;
        }

        @NonNull
        private static HomePageSource fromSavedState(@Nullable Bundle savedInstanceState) {
            if (savedInstanceState == null) {
                return NOW_POPULAR;
            }

            String sourceName = savedInstanceState.getString(STATE_SOURCE);
            if (sourceName == null) {
                return NOW_POPULAR;
            }

            try {
                return HomePageSource.valueOf(sourceName);
            } catch (IllegalArgumentException exception) {
                return NOW_POPULAR;
            }
        }
    }
}
