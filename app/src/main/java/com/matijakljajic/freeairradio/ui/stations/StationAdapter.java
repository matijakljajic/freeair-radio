package com.matijakljajic.freeairradio.ui.stations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import android.widget.TextView;
import com.matijakljajic.freeairradio.ui.util.StationDisplayFormatter;

import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationViewHolder> {

    public interface OnStationInteractionListener {
        void onStationClick(Station station);

        void onStationLongClick(Station station);
    }

    private final List<Station> stations;
    private final OnStationInteractionListener onStationInteractionListener;

    public StationAdapter(List<Station> stations, OnStationInteractionListener onStationInteractionListener) {
        this.stations = stations;
        this.onStationInteractionListener = onStationInteractionListener;
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
        holder.bind(station);
        holder.cardView.setOnClickListener(v -> {
            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) {
                return;
            }
            onStationInteractionListener.onStationClick(station);
        });
        holder.cardView.setOnLongClickListener(v -> {
            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) {
                return true;
            }
            onStationInteractionListener.onStationLongClick(station);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    public static class StationViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView nameText;
        private final TextView detailsText;

        StationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.station_item_card);
            nameText = itemView.findViewById(R.id.station_item_name);
            detailsText = itemView.findViewById(R.id.station_item_details);
        }

        void bind(@NonNull Station station) {
            nameText.setText(station.getName());
            detailsText.setText(StationDisplayFormatter.formatStationDetails(station));
        }
    }
}
