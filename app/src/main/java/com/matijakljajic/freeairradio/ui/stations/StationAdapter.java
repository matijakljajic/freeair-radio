package com.matijakljajic.freeairradio.ui.stations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.util.StationDisplayFormatter;
import com.matijakljajic.freeairradio.ui.util.StationFaviconLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationViewHolder> {

    public interface OnStationInteractionListener {
        void onStationClick(Station station);

        void onStationLongClick(Station station);
    }

    public interface DragHandleListener {
        boolean onFaviconLongClick(@NonNull Station station, @NonNull RecyclerView.ViewHolder viewHolder);
    }

    private final OnStationInteractionListener onStationInteractionListener;
    @Nullable
    private final DragHandleListener dragHandleListener;
    @NonNull
    private final List<Station> stations = new ArrayList<>();
    @NonNull
    private List<Station> dragStations = Collections.emptyList();
    private boolean dragReordering;

    public StationAdapter(OnStationInteractionListener onStationInteractionListener) {
        this(onStationInteractionListener, null);
    }

    public StationAdapter(@NonNull OnStationInteractionListener onStationInteractionListener,
                          @Nullable DragHandleListener dragHandleListener) {
        this.onStationInteractionListener = onStationInteractionListener;
        this.dragHandleListener = dragHandleListener;
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_station, parent, false);
        return new StationViewHolder(view, onStationInteractionListener, dragHandleListener);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        Station station = getStationAt(position);
        if (station == null) {
            holder.clear();
            return;
        }
        holder.bind(station);
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
    public int getItemCount() {
        return getDisplayedStations().size();
    }

    public void submitList(@NonNull List<Station> newStations) {
        submitList(newStations, null);
    }

    public void submitList(@NonNull List<Station> newStations,
                           @Nullable Runnable onCommitted) {
        List<Station> updatedStations = new ArrayList<>(newStations);
        List<Station> previousStations = new ArrayList<>(stations);
        if (previousStations.equals(updatedStations)) {
            if (onCommitted != null) {
                onCommitted.run();
            }
            return;
        }

        stations.clear();
        stations.addAll(updatedStations);

        if (dragReordering) {
            if (onCommitted != null) {
                onCommitted.run();
            }
            return;
        }

        DiffUtil.calculateDiff(new StationDiffCallback(previousStations, updatedStations))
                .dispatchUpdatesTo(this);
        if (onCommitted != null) {
            onCommitted.run();
        }
    }

    @NonNull
    public List<Station> getCurrentList() {
        return new ArrayList<>(stations);
    }

    public void beginDragReorder() {
        if (dragReordering) {
            return;
        }
        dragReordering = true;
        dragStations = new ArrayList<>(stations);
    }

    public void moveDraggedStation(int fromPosition, int toPosition) {
        if (fromPosition == toPosition
                || fromPosition < 0
                || toPosition < 0
                || !dragReordering
                || fromPosition >= dragStations.size()
                || toPosition >= dragStations.size()) {
            return;
        }

        if (fromPosition < toPosition) {
            for (int position = fromPosition; position < toPosition; position++) {
                Collections.swap(dragStations, position, position + 1);
            }
        } else {
            for (int position = fromPosition; position > toPosition; position--) {
                Collections.swap(dragStations, position, position - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @NonNull
    public List<Station> getDragReorderSnapshot() {
        return new ArrayList<>(getDisplayedStations());
    }

    public void clearDragReorderPreview() {
        if (!dragReordering) {
            return;
        }
        dragReordering = false;
        dragStations = Collections.emptyList();
        notifyDataSetChanged();
    }

    public void completeDragReorder(@NonNull List<Station> committedStations) {
        List<Station> finalStations = new ArrayList<>(committedStations);
        if (!dragReordering) {
            submitList(finalStations);
            return;
        }

        boolean changed = !stations.equals(finalStations);
        stations.clear();
        stations.addAll(finalStations);
        boolean visibleOrderChanged = !dragStations.equals(finalStations);
        dragReordering = false;
        dragStations = Collections.emptyList();
        if (changed || visibleOrderChanged) {
            notifyDataSetChanged();
        }
    }

    public boolean isDragReordering() {
        return dragReordering;
    }

    @Nullable
    private Station getStationAt(int position) {
        List<Station> displayedStations = getDisplayedStations();
        if (position < 0 || position >= displayedStations.size()) {
            return null;
        }
        return displayedStations.get(position);
    }

    @NonNull
    private List<Station> getDisplayedStations() {
        if (dragReordering) {
            return dragStations;
        }
        return stations;
    }

    private static final class StationDiffCallback extends DiffUtil.Callback {
        @NonNull
        private final List<Station> oldStations;
        @NonNull
        private final List<Station> newStations;

        private StationDiffCallback(@NonNull List<Station> oldStations,
                                    @NonNull List<Station> newStations) {
            this.oldStations = oldStations;
            this.newStations = newStations;
        }

        @Override
        public int getOldListSize() {
            return oldStations.size();
        }

        @Override
        public int getNewListSize() {
            return newStations.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldStations.get(oldItemPosition).getId()
                    .equals(newStations.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldStations.get(oldItemPosition).equals(newStations.get(newItemPosition));
        }
    }

    public static final class StationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView faviconView;
        private final TextView nameText;
        private final TextView detailsText;
        @Nullable
        private Station boundStation;
        @Nullable
        private String loadedFaviconStationId;

        StationViewHolder(@NonNull View itemView,
                          @NonNull OnStationInteractionListener listener,
                          @Nullable DragHandleListener dragHandleListener) {
            super(itemView);
            faviconView = itemView.findViewById(R.id.station_item_favicon);
            nameText = itemView.findViewById(R.id.station_item_name);
            detailsText = itemView.findViewById(R.id.station_item_details);
            itemView.setOnClickListener(v -> dispatchClick(listener));
            itemView.setOnLongClickListener(v -> dispatchLongClick(listener));
            faviconView.setOnClickListener(v -> dispatchClick(listener));
            faviconView.setOnLongClickListener(v -> {
                if (boundStation == null) {
                    return false;
                }
                if (dragHandleListener != null
                        && dragHandleListener.onFaviconLongClick(boundStation, this)) {
                    return true;
                }
                listener.onStationLongClick(boundStation);
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

        void clear() {
            clearFavicon();
            boundStation = null;
            nameText.setText(null);
            detailsText.setText(null);
        }

        private void dispatchClick(@NonNull OnStationInteractionListener listener) {
            if (boundStation != null) {
                listener.onStationClick(boundStation);
            }
        }

        private boolean dispatchLongClick(@NonNull OnStationInteractionListener listener) {
            if (boundStation != null) {
                listener.onStationLongClick(boundStation);
            }
            return true;
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
