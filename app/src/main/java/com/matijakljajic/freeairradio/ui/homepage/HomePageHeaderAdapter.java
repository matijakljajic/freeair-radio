package com.matijakljajic.freeairradio.ui.homepage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.matijakljajic.freeairradio.R;

final class HomePageHeaderAdapter extends RecyclerView.Adapter<HomePageHeaderAdapter.HeaderViewHolder> {

    private static final long CHEVRON_ROTATION_DURATION_MS = 260L;
    @NonNull
    private static final Interpolator CHEVRON_ROTATION_INTERPOLATOR = new FastOutSlowInInterpolator();

    interface Listener {
        void onSourceClicked(@NonNull View anchorView);

        void onAddStationClicked();
    }

    @NonNull
    private final Listener listener;
    @StringRes
    private int titleResId = R.string.station_list_title_now_popular;
    private boolean sourceMenuExpanded;

    HomePageHeaderAdapter(@NonNull Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    void setTitleResId(@StringRes int titleResId) {
        if (this.titleResId == titleResId) {
            return;
        }
        this.titleResId = titleResId;
        notifyItemChanged(0);
    }

    void setSourceMenuExpanded(boolean sourceMenuExpanded) {
        if (this.sourceMenuExpanded == sourceMenuExpanded) {
            return;
        }
        this.sourceMenuExpanded = sourceMenuExpanded;
        notifyItemChanged(0);
    }

    @Override
    public long getItemId(int position) {
        return Long.MIN_VALUE;
    }

    @NonNull
    @Override
    public HeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_homepage_header, parent, false);
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderViewHolder holder, int position) {
        holder.bind(titleResId, sourceMenuExpanded, listener);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static final class HeaderViewHolder extends RecyclerView.ViewHolder {
        @NonNull
        private final View sourceButton;
        @NonNull
        private final TextView titleView;
        @NonNull
        private final ImageView chevronView;
        @NonNull
        private final MaterialButton addStationButton;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            sourceButton = itemView.findViewById(R.id.homepage_source_button);
            titleView = itemView.findViewById(R.id.homepage_title);
            chevronView = itemView.findViewById(R.id.homepage_title_chevron);
            addStationButton = itemView.findViewById(R.id.homepage_add_station_button);
        }

        void bind(@StringRes int titleResId,
                  boolean sourceMenuExpanded,
                  @NonNull Listener listener) {
            titleView.setText(titleResId);
            sourceButton.setOnClickListener(listener::onSourceClicked);
            addStationButton.setOnClickListener(v -> listener.onAddStationClicked());
            rotateChevron(sourceMenuExpanded);
        }

        private void rotateChevron(boolean expanded) {
            chevronView.animate().cancel();
            chevronView.animate()
                    .rotation(expanded ? 180f : 0f)
                    .setDuration(CHEVRON_ROTATION_DURATION_MS)
                    .setInterpolator(CHEVRON_ROTATION_INTERPOLATOR)
                    .start();
        }
    }
}
