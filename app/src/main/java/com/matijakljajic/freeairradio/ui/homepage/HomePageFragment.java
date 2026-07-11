package com.matijakljajic.freeairradio.ui.homepage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.stations.StationFeedFragment;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

public class HomePageFragment extends StationFeedFragment {

    @Nullable
    private RecyclerView homepageRecyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_homepage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        bindHomepageList();

        view.post(this::loadHomepageStations);
    }

    @Override
    public void onDestroyView() {
        if (homepageRecyclerView != null) {
            detachShellContentPadding(homepageRecyclerView);
        }
        homepageRecyclerView = null;
        clearStationFeed();
        super.onDestroyView();
    }

    private void loadHomepageStations() {
        loadTopStations(R.string.station_list_empty, R.string.station_list_error);
    }

    private void bindHomepageList() {
        homepageRecyclerView = getRecyclerView();
        attachShellContentPadding(homepageRecyclerView, UiDimensions.px(requireContext(), R.dimen.top_content_gap));
        homepageRecyclerView.setNestedScrollingEnabled(true);
        ViewGroup.LayoutParams layoutParams = homepageRecyclerView.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        homepageRecyclerView.setLayoutParams(layoutParams);
        homepageRecyclerView.setAdapter(new ConcatAdapter(new HomePageHeaderAdapter(), getStationAdapter()));
    }
}
