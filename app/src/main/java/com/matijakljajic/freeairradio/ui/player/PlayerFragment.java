package com.matijakljajic.freeairradio.ui.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;

public class PlayerFragment extends Fragment {
    private TextView stationNameText;
    private TextView nowPlayingText;
    private Button playStopButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
            nowPlayingText.setText(R.string.player_now_playing_placeholder);
        } else {
            stationNameText.setText(station.getName());
            nowPlayingText.setText(R.string.player_now_playing_placeholder);
        }

        playStopButton.setEnabled(false);
        playStopButton.setText(R.string.player_play_stop_placeholder);
    }
}
