package com.matijakljajic.freeairradio.ui.localstations;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.local.LocalStationIdFactory;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
import com.matijakljajic.freeairradio.data.repository.LibraryRepository;
import com.matijakljajic.freeairradio.ui.util.DialogWindowHelper;

public class LocalStationEditorFragment extends DialogFragment {

    public static final String REQUEST_KEY = "local_station_editor_request";
    public static final String RESULT_KEY_OPEN_LOCAL_SOURCE = "result_open_local_source";

    private static final String ARG_STATION = "arg_station";

    @Nullable
    private TextInputLayout nameInputLayout;
    @Nullable
    private TextInputLayout streamUrlInputLayout;
    @Nullable
    private TextInputLayout homepageInputLayout;
    @Nullable
    private TextInputEditText nameEditText;
    @Nullable
    private TextInputEditText streamUrlEditText;
    @Nullable
    private TextInputEditText homepageEditText;
    @Nullable
    private TextInputEditText countryEditText;
    @Nullable
    private TextInputEditText languageEditText;
    @Nullable
    private TextInputEditText tagsEditText;
    @Nullable
    private TextView titleTextView;
    @Nullable
    private MaterialButton saveButton;
    @Nullable
    private MaterialButton deleteButton;
    @Nullable
    private MaterialButton closeButton;
    @Nullable
    private LibraryRepository libraryRepository;
    @Nullable
    private Station editedStation;
    private boolean saving;

    @NonNull
    public static LocalStationEditorFragment newCreateInstance() {
        return new LocalStationEditorFragment();
    }

    @NonNull
    public static LocalStationEditorFragment newEditInstance(@NonNull Station station) {
        LocalStationEditorFragment fragment = new LocalStationEditorFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STATION, station);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        editedStation = readEditedStation();
        View contentView = getLayoutInflater().inflate(R.layout.dialog_local_station_editor, null, false);
        bindViews(contentView);
        populateFields(editedStation);
        bindButtons(editedStation);

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(contentView)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        DialogWindowHelper.applyWideCenteredLayout(getDialog());
    }

    @Override
    public void onDestroyView() {
        nameInputLayout = null;
        streamUrlInputLayout = null;
        homepageInputLayout = null;
        nameEditText = null;
        streamUrlEditText = null;
        homepageEditText = null;
        countryEditText = null;
        languageEditText = null;
        tagsEditText = null;
        titleTextView = null;
        saveButton = null;
        deleteButton = null;
        closeButton = null;
        libraryRepository = null;
        editedStation = null;
        saving = false;
        super.onDestroyView();
    }

    private void bindViews(@NonNull View contentView) {
        libraryRepository = LibraryRepository.getInstance(requireContext());
        nameInputLayout = contentView.findViewById(R.id.local_station_name_input_layout);
        streamUrlInputLayout = contentView.findViewById(R.id.local_station_stream_url_input_layout);
        homepageInputLayout = contentView.findViewById(R.id.local_station_homepage_input_layout);
        nameEditText = contentView.findViewById(R.id.local_station_name_input);
        streamUrlEditText = contentView.findViewById(R.id.local_station_stream_url_input);
        homepageEditText = contentView.findViewById(R.id.local_station_homepage_input);
        countryEditText = contentView.findViewById(R.id.local_station_country_input);
        languageEditText = contentView.findViewById(R.id.local_station_language_input);
        tagsEditText = contentView.findViewById(R.id.local_station_tags_input);
        titleTextView = contentView.findViewById(R.id.local_station_editor_title);
        saveButton = contentView.findViewById(R.id.local_station_save_button);
        deleteButton = contentView.findViewById(R.id.local_station_delete_button);
        closeButton = contentView.findViewById(R.id.local_station_close_button);
    }

    private void populateFields(@Nullable Station station) {
        if (titleTextView != null) {
            titleTextView.setText(station == null
                    ? R.string.local_station_add_title
                    : R.string.local_station_edit_title);
        }
        if (station == null) {
            return;
        }

        setText(nameEditText, station.getName());
        setText(streamUrlEditText, station.getStreamUrl());
        setText(homepageEditText, station.getHomepage());
        setText(countryEditText, station.getCountry());
        setText(languageEditText, station.getLanguage());
        setText(tagsEditText, station.getTags());
    }

    private void bindButtons(@Nullable Station station) {
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> attemptSave());
        }
        if (deleteButton != null) {
            deleteButton.setVisibility(station != null ? View.VISIBLE : View.GONE);
            deleteButton.setOnClickListener(v -> deleteStation());
        }
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }
    }

    private void attemptSave() {
        if (saving || libraryRepository == null) {
            return;
        }

        clearErrors();
        String name = readRequiredText(nameEditText);
        String streamUrl = readRequiredText(streamUrlEditText);
        String homepage = readOptionalText(homepageEditText);
        if (!isValidInput(name, streamUrl, homepage)) {
            return;
        }

        Station station = buildStation(name, streamUrl, homepage);
        saving = true;
        libraryRepository.saveLocalStation(station, new LibraryRepository.WriteCallback() {
            @Override
            public void onSuccess() {
                saving = false;
                Bundle result = new Bundle();
                result.putBoolean(RESULT_KEY_OPEN_LOCAL_SOURCE, editedStation == null);
                getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                dismissAllowingStateLoss();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                saving = false;
                Toast.makeText(requireContext(), R.string.local_station_error_save_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteStation() {
        if (editedStation == null || saving || libraryRepository == null) {
            return;
        }

        saving = true;
        libraryRepository.deleteLocalStation(editedStation.getId(), new LibraryRepository.WriteCallback() {
            @Override
            public void onSuccess() {
                saving = false;
                getParentFragmentManager().setFragmentResult(REQUEST_KEY, Bundle.EMPTY);
                dismissAllowingStateLoss();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                saving = false;
                Toast.makeText(requireContext(), R.string.local_station_error_delete_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    private Station readEditedStation() {
        Bundle args = getArguments();
        if (args == null) {
            return null;
        }
        return BundleCompat.getSerializable(args, ARG_STATION, Station.class);
    }

    @NonNull
    private Station buildStation(@NonNull String name,
                                 @NonNull String streamUrl,
                                 @Nullable String homepage) {
        return Station.builder(
                        editedStation != null ? editedStation.getId() : LocalStationIdFactory.create(),
                        name,
                        streamUrl,
                        StationOrigin.LOCAL_USER
                )
                .setHomepage(homepage)
                .setCountry(readOptionalText(countryEditText))
                .setLanguage(readOptionalText(languageEditText))
                .setTags(readOptionalText(tagsEditText))
                .build();
    }

    private boolean isValidInput(@NonNull String name,
                                 @NonNull String streamUrl,
                                 @Nullable String homepage) {
        boolean hasError = false;
        if (TextUtils.isEmpty(name)) {
            setError(nameInputLayout, R.string.local_station_error_name_required);
            hasError = true;
        }
        if (TextUtils.isEmpty(streamUrl)) {
            setError(streamUrlInputLayout, R.string.local_station_error_stream_url_required);
            hasError = true;
        } else if (!isSupportedUrl(streamUrl)) {
            setError(streamUrlInputLayout, R.string.local_station_error_stream_url_invalid);
            hasError = true;
        }
        if (!TextUtils.isEmpty(homepage) && !isSupportedUrl(homepage)) {
            setError(homepageInputLayout, R.string.local_station_error_homepage_invalid);
            hasError = true;
        }
        return !hasError;
    }

    private void clearErrors() {
        clearError(nameInputLayout);
        clearError(streamUrlInputLayout);
        clearError(homepageInputLayout);
    }

    private void clearError(@Nullable TextInputLayout inputLayout) {
        if (inputLayout != null) {
            inputLayout.setError(null);
        }
    }

    private void setError(@Nullable TextInputLayout inputLayout, int stringResId) {
        if (inputLayout != null) {
            inputLayout.setError(getString(stringResId));
        }
    }

    private void setText(@Nullable TextInputEditText editText, @Nullable String value) {
        if (editText == null || value == null || Station.UNKNOWN.equals(value)) {
            return;
        }
        editText.setText(value);
    }

    @Nullable
    private String readOptionalText(@Nullable TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return null;
        }
        String value = editText.getText().toString().trim();
        return value.isEmpty() ? null : value;
    }

    @NonNull
    private String readRequiredText(@Nullable TextInputEditText editText) {
        String value = readOptionalText(editText);
        return value != null ? value : "";
    }

    private boolean isSupportedUrl(@NonNull String rawUrl) {
        Uri uri = Uri.parse(rawUrl);
        String scheme = uri.getScheme();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && !TextUtils.isEmpty(uri.getHost());
    }
}
