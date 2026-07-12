package com.matijakljajic.freeairradio.ui.stations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.util.StationDisplayFormatter;
import com.matijakljajic.freeairradio.ui.util.StationFaviconLoader;

public class StationAdapter extends ListAdapter<Station, StationAdapter.StationViewHolder> {

    public interface OnStationInteractionListener {
        void onStationClick(Station station);

        void onStationLongClick(Station station);
    }

    private final OnStationInteractionListener onStationInteractionListener;

    public StationAdapter(OnStationInteractionListener onStationInteractionListener) {
        super(DIFF_CALLBACK);
        this.onStationInteractionListener = onStationInteractionListener;
        setHasStableIds(true);
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
        if (holder.itemView.isAttachedToWindow()) {
            holder.loadFavicon(position);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull StationViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.loadFavicon(holder.getBindingAdapterPosition());
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull StationViewHolder holder) {
        holder.clearFavicon();
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onViewRecycled(@NonNull StationViewHolder holder) {
        holder.clearFavicon();
        super.onViewRecycled(holder);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId().hashCode();
    }

    public static class StationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView faviconView;
        private final TextView nameText;
        private final TextView detailsText;
        @Nullable
        private Station boundStation;
        @Nullable
        private String loadedFaviconStationId;

        StationViewHolder(@NonNull View itemView, @NonNull OnStationInteractionListener listener) {
            super(itemView);
            faviconView = itemView.findViewById(R.id.station_item_favicon);
            nameText = itemView.findViewById(R.id.station_item_name);
            detailsText = itemView.findViewById(R.id.station_item_details);
            itemView.setOnClickListener(v -> {
                if (boundStation != null) {
                    listener.onStationClick(boundStation);
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (boundStation != null) {
                    listener.onStationLongClick(boundStation);
                }
                return true;
            });
        }

        void bind(@NonNull Station station) {
            if (boundStation == null || !boundStation.getId().equals(station.getId())) {
                clearFavicon();
            }
            boundStation = station;
            nameText.setText(station.getName());
            detailsText.setText(StationDisplayFormatter.formatStationDetails(station));
        }

        void loadFavicon(int initialRevealOrder) {
            if (boundStation == null) {
                clearFavicon();
                return;
            }
            if (boundStation.getId().equals(loadedFaviconStationId)) {
                return;
            }

            StationFaviconLoader.loadInto(faviconView, boundStation, initialRevealOrder);
            loadedFaviconStationId = boundStation.getId();
        }

        void clearFavicon() {
            StationFaviconLoader.clear(faviconView);
            loadedFaviconStationId = null;
        }
    }
}
