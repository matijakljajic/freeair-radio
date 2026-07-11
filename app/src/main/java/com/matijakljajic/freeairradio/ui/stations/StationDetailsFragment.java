package com.matijakljajic.freeairradio.ui.stations;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.ui.util.StationDisplayFormatter;

public class StationDetailsFragment extends DialogFragment {

    private static final String ARG_STATION = "arg_station";

    public static StationDetailsFragment newInstance(@NonNull Station station) {
        StationDetailsFragment fragment = new StationDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STATION, station);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Station station = requireStation();
        View contentView = getLayoutInflater()
                .inflate(R.layout.fragment_station_details, null, false);
        bindContent(contentView, station);
        contentView.findViewById(R.id.station_details_ok_button).setOnClickListener(v -> dismiss());

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(contentView)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER_HORIZONTAL);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    @NonNull
    private Station requireStation() {
        Bundle args = requireArguments();
        Station station = BundleCompat.getSerializable(args, ARG_STATION, Station.class);
        if (station == null) {
            throw new IllegalStateException("Station details requires a station");
        }
        return station;
    }

    private void bindContent(@NonNull View contentView, @NonNull Station station) {
        ((TextView) contentView.findViewById(R.id.station_details_name)).setText(station.getName());
        bindCompactTextRow(contentView, R.id.row_country, R.string.station_details_country, station.getCountry());
        bindCompactTextRow(contentView, R.id.row_language, R.string.station_details_language, station.getLanguage());
        bindCompactTextRow(contentView, R.id.row_codec, R.string.station_details_codec, station.getCodec());
        bindBitrateRow(contentView, station);
        bindTallTextRow(contentView, R.id.row_tags, R.string.station_details_tags,
                StationDisplayFormatter.formatTags(station));
        bindLinkRow(contentView, R.id.row_homepage, R.string.station_details_homepage, station.getHomepage());
        bindLinkRow(contentView, R.id.row_stream_url, R.string.station_details_stream_url, station.getStreamUrl());
    }

    private void bindCompactTextRow(@NonNull View contentView, int rowId, int labelStringId, @NonNull String value) {
        StationDetailRowView row = contentView.findViewById(rowId);
        row.bindCompactText(labelStringId, value);
    }

    private void bindTallTextRow(@NonNull View contentView, int rowId, int labelStringId, @NonNull String value) {
        StationDetailRowView row = contentView.findViewById(rowId);
        row.bindTallText(labelStringId, value);
    }

    private void bindBitrateRow(@NonNull View contentView, @NonNull Station station) {
        StationDetailRowView row = contentView.findViewById(R.id.row_bitrate);
        row.bindCompactBitrate(R.string.station_details_bitrate, station.getBitrate());
    }

    private void bindLinkRow(@NonNull View contentView, int rowId, int labelStringId, @Nullable String value) {
        StationDetailRowView row = contentView.findViewById(rowId);
        row.bindTallLink(labelStringId, value, value == null ? null : v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(value))));
    }
}
