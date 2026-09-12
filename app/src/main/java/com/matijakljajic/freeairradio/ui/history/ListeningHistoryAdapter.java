package com.matijakljajic.freeairradio.ui.history;

import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.ListeningHistoryTrack;
import com.matijakljajic.freeairradio.data.model.ListeningHistoryEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ListeningHistoryAdapter extends RecyclerView.Adapter<ListeningHistoryAdapter.StationViewHolder> {

    private static final long DETAILS_REVEAL_DURATION_MS = 260L;
    @NonNull
    private static final Interpolator DETAILS_REVEAL_INTERPOLATOR = new FastOutSlowInInterpolator();
    @NonNull
    private final List<ListeningHistoryEntry> stations = new ArrayList<>();
    @NonNull
    private final Set<String> expandedStationIds = new HashSet<>();

    ListeningHistoryAdapter() {
        setHasStableIds(true);
    }

    void resetExpandedState() {
        if (expandedStationIds.isEmpty()) {
            return;
        }

        expandedStationIds.clear();
        notifyDataSetChanged();
    }

    void submitList(@NonNull List<ListeningHistoryEntry> newStations) {
        List<ListeningHistoryEntry> updatedStations = new ArrayList<>(newStations);
        List<ListeningHistoryEntry> previousStations = new ArrayList<>(stations);
        if (previousStations.equals(updatedStations)) {
            return;
        }

        stations.clear();
        stations.addAll(updatedStations);
        retainOnlyExpandedStationsStillPresent();
        DiffUtil.calculateDiff(new StationDiffCallback(previousStations, updatedStations))
                .dispatchUpdatesTo(this);
    }

    @Override
    public long getItemId(int position) {
        return stations.get(position).getStation().getId().hashCode();
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listening_history_entry, parent, false);
        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        ListeningHistoryEntry station = stations.get(position);
        String stationId = station.getStation().getId();
        holder.bind(
                station,
                expandedStationIds.contains(stationId),
                () -> toggleExpandedState(stationId, holder)
        );
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    private void toggleExpandedState(@NonNull String stationId, @NonNull StationViewHolder holder) {
        boolean expanded;
        if (expandedStationIds.contains(stationId)) {
            expandedStationIds.remove(stationId);
            expanded = false;
        } else {
            expandedStationIds.add(stationId);
            expanded = true;
        }

        holder.setExpandedAnimated(expanded);
    }

    private void retainOnlyExpandedStationsStillPresent() {
        HashSet<String> currentStationIds = new HashSet<>();
        for (ListeningHistoryEntry station : stations) {
            currentStationIds.add(station.getStation().getId());
        }
        expandedStationIds.retainAll(currentStationIds);
    }

    @Override
    public void onViewRecycled(@NonNull StationViewHolder holder) {
        holder.cancelAnimations();
        super.onViewRecycled(holder);
    }

    static final class StationViewHolder extends RecyclerView.ViewHolder {
        @NonNull
        private final TextView titleView;
        @NonNull
        private final TextView listenedAtView;
        @NonNull
        private final ImageView chevronView;
        @NonNull
        private final LinearLayout tracksContainer;
        @NonNull
        private final LayoutInflater inflater;
        @NonNull
        private final SimpleDateFormat timeFormatter =
                new SimpleDateFormat("HH:mm", Locale.getDefault());
        private ValueAnimator detailsAnimator;

        StationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.listening_history_station_title);
            listenedAtView = itemView.findViewById(R.id.listening_history_station_time);
            chevronView = itemView.findViewById(R.id.listening_history_station_chevron);
            tracksContainer = itemView.findViewById(R.id.listening_history_track_container);
            inflater = LayoutInflater.from(itemView.getContext());
        }

        void bind(@NonNull ListeningHistoryEntry station,
                  boolean expanded,
                  @NonNull Runnable toggleAction) {
            titleView.setText(station.getStation().getName());
            listenedAtView.setText(timeFormatter.format(new Date(station.getListenedAt())));
            bindTracks(station.getTracks());
            boolean hasTracks = tracksContainer.getChildCount() > 0;
            itemView.setOnClickListener(hasTracks ? v -> toggleAction.run() : null);
            itemView.setClickable(hasTracks);
            chevronView.setVisibility(hasTracks ? View.VISIBLE : View.INVISIBLE);
            setExpanded(hasTracks && expanded);
        }

        private void bindTracks(@NonNull List<ListeningHistoryTrack> tracks) {
            tracksContainer.removeAllViews();
            if (tracks.isEmpty()) {
                tracksContainer.setVisibility(View.GONE);
                return;
            }

            tracksContainer.setVisibility(View.VISIBLE);
            for (ListeningHistoryTrack track : tracks) {
                String displayText = track.buildDisplayText();
                if (displayText == null) {
                    continue;
                }

                View trackView = inflater.inflate(
                        R.layout.item_listening_history_track,
                        tracksContainer,
                        false
                );
                TextView trackTextView = trackView.findViewById(R.id.listening_history_track_text);
                trackTextView.setText(displayText);
                tracksContainer.addView(trackView);
            }
            tracksContainer.setVisibility(
                    tracksContainer.getChildCount() > 0 ? View.VISIBLE : View.GONE
            );
        }

        void setExpanded(boolean expanded) {
            boolean hasTracks = tracksContainer.getChildCount() > 0;
            cancelAnimations();
            applyExpandedState(hasTracks && expanded);
        }

        void setExpandedAnimated(boolean expanded) {
            if (tracksContainer.getChildCount() == 0) {
                applyExpandedState(false);
                return;
            }

            cancelAnimations();
            int expandedHeight = measureExpandedHeight();
            if (expandedHeight <= 0) {
                applyExpandedState(expanded);
                return;
            }

            int startHeight = expanded
                    ? 0
                    : Math.max(tracksContainer.getHeight(), expandedHeight);
            int endHeight = expanded ? expandedHeight : 0;

            if (expanded) {
                tracksContainer.setVisibility(View.VISIBLE);
            }

            setSongsContainerHeight(startHeight);
            chevronView.animate()
                    .rotation(expanded ? 180f : 0f)
                    .setDuration(DETAILS_REVEAL_DURATION_MS)
                    .setInterpolator(DETAILS_REVEAL_INTERPOLATOR)
                    .start();

            detailsAnimator = ValueAnimator.ofInt(startHeight, endHeight);
            detailsAnimator.setDuration(DETAILS_REVEAL_DURATION_MS);
            detailsAnimator.setInterpolator(DETAILS_REVEAL_INTERPOLATOR);
            detailsAnimator.addUpdateListener(animation ->
                    setSongsContainerHeight((int) animation.getAnimatedValue()));
            detailsAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    if (detailsAnimator != animation) {
                        return;
                    }

                    detailsAnimator = null;
                    applyExpandedState(expanded);
                }
            });
            detailsAnimator.start();
        }

        void cancelAnimations() {
            if (detailsAnimator != null) {
                detailsAnimator.cancel();
                detailsAnimator = null;
            }
            tracksContainer.animate().cancel();
            chevronView.animate().cancel();
        }

        private void applyExpandedState(boolean expanded) {
            boolean hasTracks = tracksContainer.getChildCount() > 0;
            if (!hasTracks || !expanded) {
                tracksContainer.setVisibility(View.GONE);
                setSongsContainerHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                chevronView.setRotation(0f);
                return;
            }

            tracksContainer.setVisibility(View.VISIBLE);
            setSongsContainerHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            chevronView.setRotation(180f);
        }

        private int measureExpandedHeight() {
            int width = tracksContainer.getWidth();
            if (width <= 0) {
                int parentWidth = ((View) tracksContainer.getParent()).getWidth();
                width = Math.max(0, parentWidth - tracksContainer.getPaddingLeft() - tracksContainer.getPaddingRight());
            }
            if (width <= 0) {
                return tracksContainer.getHeight();
            }

            int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            tracksContainer.measure(widthSpec, heightSpec);
            return tracksContainer.getMeasuredHeight();
        }

        private void setSongsContainerHeight(int height) {
            ViewGroup.LayoutParams layoutParams = tracksContainer.getLayoutParams();
            if (layoutParams.height == height) {
                return;
            }
            layoutParams.height = height;
            tracksContainer.setLayoutParams(layoutParams);
        }
    }

    private static final class StationDiffCallback extends DiffUtil.Callback {
        @NonNull
        private final List<ListeningHistoryEntry> oldStations;
        @NonNull
        private final List<ListeningHistoryEntry> newStations;

        private StationDiffCallback(@NonNull List<ListeningHistoryEntry> oldStations,
                                    @NonNull List<ListeningHistoryEntry> newStations) {
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
            return oldStations.get(oldItemPosition).getStation().getId()
                    .equals(newStations.get(newItemPosition).getStation().getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldStations.get(oldItemPosition).equals(newStations.get(newItemPosition));
        }
    }
}
