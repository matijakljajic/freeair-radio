package com.matijakljajic.freeairradio.ui.stations;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.seed.StationSeedData;

import java.util.List;

public class StationListFragment extends Fragment implements
        StationAdapter.OnStationInteractionListener {

    private static final int LIST_TOP_PADDING_DP = 16;
    private static final int LIST_BOTTOM_PADDING_DP = 16;

    public interface OnStationSelectedListener {
        void onStationSelected(Station station);
    }

    private final List<Station> stations = StationSeedData.createDemoStations();
    @Nullable
    private OnStationSelectedListener listener;
    @Nullable
    private RecyclerView recyclerView;
    @Nullable
    private View playerShellView;
    private int topSystemInset;
    private final View.OnLayoutChangeListener playerShellLayoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateRecyclerPadding();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnStationSelectedListener) {
            listener = (OnStationSelectedListener) context;
        } else {
            throw new IllegalStateException("Host activity must implement OnStationSelectedListener");
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
        recyclerView = view.findViewById(R.id.station_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new StationAdapter(stations, this));
        if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        recyclerView.setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topSystemInset = systemBars.top;
            updateRecyclerPadding();
            return insets;
        });
        ViewCompat.requestApplyInsets(recyclerView);

        playerShellView = requireActivity().findViewById(R.id.player_shell_container);
        if (playerShellView != null) {
            playerShellView.addOnLayoutChangeListener(playerShellLayoutChangeListener);
        }

        view.post(this::updateRecyclerPadding);
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
        recyclerView = null;
        super.onDestroyView();
    }

    private void updateRecyclerPadding() {
        if (recyclerView == null) {
            return;
        }

        int topPadding = getTopRecyclerPaddingPx();
        int bottomPadding = getBottomRecyclerPaddingPx();
        recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                topPadding,
                recyclerView.getPaddingRight(),
                bottomPadding
        );
    }

    private int getTopRecyclerPaddingPx() {
        return topSystemInset + dpToPx(LIST_TOP_PADDING_DP);
    }

    private int getBottomRecyclerPaddingPx() {
        return getPlayerShellHeight() + dpToPx(LIST_BOTTOM_PADDING_DP);
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

}
