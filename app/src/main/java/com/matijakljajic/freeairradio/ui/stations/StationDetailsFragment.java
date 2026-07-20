package com.matijakljajic.freeairradio.ui.stations;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.repository.LibraryRepository;
import com.matijakljajic.freeairradio.ui.util.DialogWindowHelper;
import com.matijakljajic.freeairradio.ui.util.StationDisplayFormatter;

@SuppressWarnings("unused")
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
        LibraryRepository libraryRepository = LibraryRepository.getInstance(requireContext());
        View contentView = getLayoutInflater()
                .inflate(R.layout.dialog_station_details, null, false);
        bindContent(contentView, station);
        bindFavoriteButton(contentView, station, libraryRepository);
        contentView.findViewById(R.id.station_details_ok_button).setOnClickListener(v -> dismiss());

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(contentView)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        DialogWindowHelper.applyWideCenteredLayout(getDialog());
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
        row(contentView, R.id.row_country)
                .bindCompactText(R.string.station_details_country, station.getCountryName());
        row(contentView, R.id.row_language)
                .bindCompactText(R.string.station_details_language, station.getLanguage());
        row(contentView, R.id.row_codec)
                .bindCompactText(R.string.station_details_codec, station.getCodec());
        row(contentView, R.id.row_bitrate)
                .bindCompactBitrate(R.string.station_details_bitrate, station.getBitrate());
        row(contentView, R.id.row_tags)
                .bindTallText(R.string.station_details_tags, StationDisplayFormatter.formatTags(station));
        bindLinkRow(contentView, R.id.row_homepage, R.string.station_details_homepage, station.getHomepage());
        bindLinkRow(contentView, R.id.row_stream_url, R.string.station_details_stream_url, station.getStreamUrl());
    }

    private void bindFavoriteButton(@NonNull View contentView,
                                    @NonNull Station station,
                                    @NonNull LibraryRepository libraryRepository) {
        MaterialButton favoriteButton = contentView.findViewById(R.id.station_details_favorite_button);
        boolean initialFavorite = libraryRepository.isFavorite(station);
        updateFavoriteButton(favoriteButton, initialFavorite);
        favoriteButton.setOnClickListener(v -> {
            boolean nextFavorite = !libraryRepository.isFavorite(station);
            libraryRepository.setFavorite(station, nextFavorite);
            updateFavoriteButton(favoriteButton, nextFavorite);
        });
    }

    private void bindLinkRow(@NonNull View contentView, int rowId, int labelStringId, @Nullable String value) {
        row(contentView, rowId).bindTallLink(
                labelStringId,
                value,
                value == null ? null : v -> openUrl(value)
        );
    }

    @NonNull
    private StationDetailRowView row(@NonNull View contentView, int rowId) {
        return contentView.findViewById(rowId);
    }

    private void updateFavoriteButton(@NonNull MaterialButton button, boolean favorite) {
        button.setText(favorite
                ? R.string.player_unfavorite_button
                : R.string.player_favorite_button);
    }

    private void openUrl(@NonNull String value) {
        startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(value)));
    }
}
