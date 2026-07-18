package com.matijakljajic.freeairradio.ui.homepage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.matijakljajic.freeairradio.R;

final class HomePageHeaderAdapter extends RecyclerView.Adapter<HomePageHeaderAdapter.HeaderViewHolder> {

    interface Listener {
        void onSourceClicked(@NonNull View anchorView);

        void onAddStationClicked();
    }

    @NonNull
    private final Listener listener;
    @StringRes
    private int titleResId = R.string.station_list_title_now_popular;

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
        holder.bind(titleResId, listener);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static final class HeaderViewHolder extends RecyclerView.ViewHolder {
        @NonNull
        private final TextView titleView;
        @NonNull
        private final MaterialButton addStationButton;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.homepage_title);
            addStationButton = itemView.findViewById(R.id.homepage_add_station_button);
        }

        void bind(@StringRes int titleResId, @NonNull Listener listener) {
            titleView.setText(titleResId);
            titleView.setOnClickListener(listener::onSourceClicked);
            addStationButton.setOnClickListener(v -> listener.onAddStationClicked());
        }
    }
}
