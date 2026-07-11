package com.matijakljajic.freeairradio.ui.homepage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.matijakljajic.freeairradio.R;

final class HomePageHeaderAdapter extends RecyclerView.Adapter<HomePageHeaderAdapter.HeaderViewHolder> {

    @NonNull
    @Override
    public HeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_homepage_header, parent, false);
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderViewHolder holder, int position) {
        // Static header.
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static final class HeaderViewHolder extends RecyclerView.ViewHolder {
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
