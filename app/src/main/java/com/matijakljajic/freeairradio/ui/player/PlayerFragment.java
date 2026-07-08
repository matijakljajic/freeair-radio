package com.matijakljajic.freeairradio.ui.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.util.MarqueeTextView;

public class PlayerFragment extends Fragment {
    private static final long MARQUEE_START_DELAY_MS = 1000L;

    private View playerView;
    private MarqueeTextView stationNameText;
    private MarqueeTextView nowPlayingText;
    private Button playStopButton;
    private final Runnable activateMarqueeRunnable = this::activateMiniPlayerMarquee;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        playerView = view;
        stationNameText = view.findViewById(R.id.player_station_name);
        nowPlayingText = view.findViewById(R.id.player_now_playing);
        playStopButton = view.findViewById(R.id.player_play_stop_button);
        renderStation(null);
    }

    public void showStation(@Nullable Station station) {
        renderStation(station);
    }

    private void renderStation(@Nullable Station station) {
        if (stationNameText == null || nowPlayingText == null || playStopButton == null) {
            return;
        }

        if (station == null) {
            stationNameText.setText(R.string.player_no_station_selected);
        } else {
            stationNameText.setText(station.getName());
        }
        nowPlayingText.setText(R.string.player_now_playing_placeholder);

        scheduleMiniPlayerMarquee();

        playStopButton.setEnabled(false);
        playStopButton.setText(R.string.player_play_stop_placeholder);
    }

    @Override
    public void onDestroyView() {
        if (playerView != null) {
            playerView.removeCallbacks(activateMarqueeRunnable);
        }
        playerView = null;
        stationNameText = null;
        nowPlayingText = null;
        playStopButton = null;
        super.onDestroyView();
    }

    private void scheduleMiniPlayerMarquee() {
        if (playerView == null) {
            return;
        }

        playerView.removeCallbacks(activateMarqueeRunnable);
        stationNameText.setSelected(false);
        nowPlayingText.setSelected(false);
        playerView.postDelayed(activateMarqueeRunnable, MARQUEE_START_DELAY_MS);
    }

    private void activateMiniPlayerMarquee() {
        if (stationNameText == null || nowPlayingText == null || playerView == null) {
            return;
        }
        stationNameText.setSelected(true);
        nowPlayingText.setSelected(true);
    }
}
