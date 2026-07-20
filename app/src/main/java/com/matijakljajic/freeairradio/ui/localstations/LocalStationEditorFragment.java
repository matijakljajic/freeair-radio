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
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.local.LocalStationIdFactory;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.RadioBrowserRepository;
import com.matijakljajic.freeairradio.data.repository.LibraryRepository;
import com.matijakljajic.freeairradio.data.repository.StationRepository;
import com.matijakljajic.freeairradio.ui.settings.TopStationsGeography;
import com.matijakljajic.freeairradio.ui.util.DialogWindowHelper;
import com.matijakljajic.freeairradio.ui.util.DropdownMenuHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private MaterialAutoCompleteTextView countryEditText;
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
    private MaterialButton favoriteButton;
    @Nullable
    private MaterialButton closeButton;
    @Nullable
    private LibraryRepository libraryRepository;
    @Nullable
    private StationRepository stationRepository;
    @NonNull
    private List<String> supportedCountryCodes = Collections.emptyList();
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
        loadSupportedCountries();
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
        favoriteButton = null;
        closeButton = null;
        libraryRepository = null;
        stationRepository = null;
        supportedCountryCodes = Collections.emptyList();
        editedStation = null;
        saving = false;
        super.onDestroyView();
    }

    private void bindViews(@NonNull View contentView) {
        libraryRepository = LibraryRepository.getInstance(requireContext());
        stationRepository = new RadioBrowserRepository(requireContext().getApplicationContext());
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
        favoriteButton = contentView.findViewById(R.id.local_station_favorite_button);
        closeButton = contentView.findViewById(R.id.local_station_close_button);
    }

    private void loadSupportedCountries() {
        StationRepository repository = stationRepository;
        if (repository == null) {
            return;
        }

        repository.loadAvailableCountryCodes(new StationRepository.CountryCodesCallback() {
            @Override
            public void onCountryCodesLoaded(@NonNull List<String> countryCodes) {
                if (!isAdded()) {
                    return;
                }
                supportedCountryCodes = countryCodes;
                bindSupportedCountryOptions();
                normalizeVisibleCountry();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                supportedCountryCodes = Collections.emptyList();
            }
        });
    }

    private void bindSupportedCountryOptions() {
        MaterialAutoCompleteTextView countryView = countryEditText;
        if (countryView == null) {
            return;
        }

        List<TopStationsGeography.CountryOption> countryOptions =
                TopStationsGeography.getAllCountryOptions(supportedCountryCodes);
        List<String> labels = new ArrayList<>(countryOptions.size());
        for (TopStationsGeography.CountryOption countryOption : countryOptions) {
            labels.add(countryOption.label);
        }
        DropdownMenuHelper.bindOptions(countryView, labels);
    }

    private void normalizeVisibleCountry() {
        TopStationsGeography.CountryOption countryOption = resolveCountryOption();
        if (countryOption == null || countryEditText == null) {
            return;
        }
        countryEditText.setText(countryOption.label, false);
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
        setText(countryEditText, station.getCountryName());
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
        if (favoriteButton != null) {
            if (station == null || libraryRepository == null) {
                favoriteButton.setVisibility(View.GONE);
            } else {
                favoriteButton.setVisibility(View.VISIBLE);
                updateFavoriteButton(libraryRepository.isFavorite(station));
                favoriteButton.setOnClickListener(v -> toggleFavorite(station));
            }
        }
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }
    }

    private void toggleFavorite(@NonNull Station station) {
        if (libraryRepository == null) {
            return;
        }

        boolean nextFavorite = !libraryRepository.isFavorite(station);
        libraryRepository.setFavorite(station, nextFavorite);
        updateFavoriteButton(nextFavorite);
    }

    private void updateFavoriteButton(boolean favorite) {
        if (favoriteButton == null) {
            return;
        }
        favoriteButton.setText(favorite
                ? R.string.player_unfavorite_button
                : R.string.player_favorite_button);
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
        TopStationsGeography.CountryOption normalizedCountry = resolveCountryOption();
        String countryName = normalizedCountry != null
                ? normalizedCountry.label
                : readOptionalText(countryEditText);
        String countryCode = normalizedCountry != null
                ? normalizedCountry.countryCode
                : readExistingCountryCode(countryName);
        return Station.builder(
                        editedStation != null ? editedStation.getId() : LocalStationIdFactory.create(),
                        name,
                        streamUrl,
                        StationOrigin.LOCAL_USER
                )
                .setHomepage(homepage)
                .setCountryName(countryName)
                .setCountryCode(countryCode)
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

    private void setText(@Nullable TextView textView, @Nullable String value) {
        if (textView == null || value == null || Station.UNKNOWN.equals(value)) {
            return;
        }
        textView.setText(value);
    }

    @Nullable
    private String readOptionalText(@Nullable TextView textView) {
        if (textView == null || textView.getText() == null) {
            return null;
        }
        String value = textView.getText().toString().trim();
        return value.isEmpty() ? null : value;
    }

    @NonNull
    private String readRequiredText(@Nullable TextView textView) {
        String value = readOptionalText(textView);
        return value != null ? value : "";
    }

    @Nullable
    private TopStationsGeography.CountryOption resolveCountryOption() {
        if (supportedCountryCodes.isEmpty()) {
            return null;
        }
        return TopStationsGeography.findClosestCountryOption(
                readOptionalText(countryEditText),
                supportedCountryCodes
        );
    }

    @Nullable
    private String readExistingCountryCode(@Nullable String countryName) {
        if (editedStation == null
                || countryName == null
                || Station.UNKNOWN.equals(editedStation.getCountryCode())
                || !countryName.equalsIgnoreCase(editedStation.getCountryName())) {
            return null;
        }
        return editedStation.getCountryCode();
    }

    private boolean isSupportedUrl(@NonNull String rawUrl) {
        Uri uri = Uri.parse(rawUrl);
        String scheme = uri.getScheme();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && !TextUtils.isEmpty(uri.getHost());
    }
}
