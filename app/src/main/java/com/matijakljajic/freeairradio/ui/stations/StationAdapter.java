package com.matijakljajic.freeairradio.ui.stations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.util.StationDisplayFormatter;

public class StationAdapter extends ListAdapter<Station, StationAdapter.StationViewHolder> {

    public interface OnStationInteractionListener {
        void onStationClick(Station station);

        void onStationLongClick(Station station);
    }

    private final OnStationInteractionListener onStationInteractionListener;

    public StationAdapter(OnStationInteractionListener onStationInteractionListener) {
        super(DIFF_CALLBACK);
        this.onStationInteractionListener = onStationInteractionListener;
    }

    private static final DiffUtil.ItemCallback<Station> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Station oldItem, @NonNull Station newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Station oldItem, @NonNull Station newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_station, parent, false);
        return new StationViewHolder(view, onStationInteractionListener);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static class StationViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView nameText;
        private final TextView detailsText;
        @Nullable
        private Station boundStation;

        StationViewHolder(@NonNull View itemView, @NonNull OnStationInteractionListener listener) {
            super(itemView);
            cardView = itemView.findViewById(R.id.station_item_card);
            nameText = itemView.findViewById(R.id.station_item_name);
            detailsText = itemView.findViewById(R.id.station_item_details);
            cardView.setOnClickListener(v -> {
                if (boundStation != null) {
                    listener.onStationClick(boundStation);
                }
            });
            cardView.setOnLongClickListener(v -> {
                if (boundStation != null) {
                    listener.onStationLongClick(boundStation);
                }
                return true;
            });
        }

        void bind(@NonNull Station station) {
            boundStation = station;
            nameText.setText(station.getName());
            detailsText.setText(StationDisplayFormatter.formatStationDetails(station));
        }
    }
}
