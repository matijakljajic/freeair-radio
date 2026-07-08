package com.matijakljajic.freeairradio.ui.stations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.util.MarqueeTextView;
import com.matijakljajic.freeairradio.ui.util.StationDisplayFormatter;

import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationViewHolder> {

    public interface OnStationClickListener {
        void onStationClick(Station station);
    }

    private final List<Station> stations;
    private final OnStationClickListener onStationClickListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public StationAdapter(List<Station> stations, OnStationClickListener onStationClickListener) {
        this.stations = stations;
        this.onStationClickListener = onStationClickListener;
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_station, parent, false);
        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        Station station = stations.get(position);
        boolean isSelected = position == selectedPosition;
        holder.bind(station, isSelected);
        holder.cardView.setOnClickListener(v -> {
            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) {
                return;
            }
            selectStation(clickedPosition);
            onStationClickListener.onStationClick(station);
        });
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    private void selectStation(int position) {
        if (position == selectedPosition) {
            return;
        }

        int previousSelectedPosition = selectedPosition;
        selectedPosition = position;
        if (previousSelectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousSelectedPosition);
        }
        notifyItemChanged(selectedPosition);
    }

    public static class StationViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final TextView nameText;
        private final MarqueeTextView detailsText;

        StationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.station_item_card);
            nameText = itemView.findViewById(R.id.station_item_name);
            detailsText = itemView.findViewById(R.id.station_item_details);
        }

        void bind(@NonNull Station station, boolean isSelected) {
            nameText.setText(station.getName());
            detailsText.setText(StationDisplayFormatter.formatStationDetails(station));
            detailsText.setSelected(isSelected);
        }
    }
}
