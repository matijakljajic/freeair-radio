package com.matijakljajic.freeairradio.ui.settings;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.util.DialogWindowHelper;
import com.matijakljajic.freeairradio.ui.util.DropdownMenuHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class TopStationsLocationDialog {

    interface Listener {
        void onSelectionConfirmed(@NonNull TopStationsLocationSettings.Selection selection);
    }

    private TopStationsLocationDialog() {
    }

    static void show(@NonNull Fragment fragment,
                     @NonNull List<String> availableCountryCodes,
                     @NonNull TopStationsLocationSettings.Selection currentSelection,
                     @NonNull Listener listener) {
        if (!fragment.isAdded()) {
            return;
        }

        List<TopStationsLocationOption> options =
                buildOptions(fragment, availableCountryCodes);

        View contentView = fragment.getLayoutInflater()
                .inflate(R.layout.dialog_settings_choice, null, false);
        TextView titleView = contentView.findViewById(R.id.settings_choice_dialog_title);
        MaterialAutoCompleteTextView inputView = contentView.findViewById(R.id.settings_choice_dialog_input);
        Button closeButton = contentView.findViewById(R.id.settings_choice_dialog_close_button);
        Button confirmButton = contentView.findViewById(R.id.settings_choice_dialog_confirm_button);

        titleView.setText(R.string.settings_top_stations_location_section_title);

        List<String> labels = new ArrayList<>(options.size());
        for (TopStationsLocationOption option : options) {
            labels.add(option.label);
        }

        TopStationsLocationOption currentOption = findMatchingOption(options, currentSelection);
        final TopStationsLocationOption[] selectedOption = new TopStationsLocationOption[]{currentOption};
        DropdownMenuHelper.bindOptions(inputView, labels);
        inputView.setOnItemClickListener((parent, view, position, id) -> selectedOption[0] = options.get(position));
        if (currentOption != null) {
            inputView.setText(currentOption.label, false);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(fragment.requireContext())
                .setView(contentView)
                .create();

        closeButton.setOnClickListener(v -> dialog.dismiss());
        confirmButton.setOnClickListener(v -> {
            String rawValue = inputView.getText() != null ? inputView.getText().toString() : null;
            TopStationsLocationOption confirmedOption = selectedOption[0];
            if (confirmedOption == null || !hasExactLabelMatch(confirmedOption, rawValue)) {
                confirmedOption = findExactOption(options, rawValue);
            }
            if (confirmedOption == null) {
                inputView.setError(fragment.getString(R.string.settings_top_stations_location_invalid));
                return;
            }

            inputView.setError(null);
            listener.onSelectionConfirmed(confirmedOption.toSelection());
            dialog.dismiss();
        });

        dialog.show();
        DialogWindowHelper.applyWideCenteredLayout(dialog);
    }

    @NonNull
    private static List<TopStationsLocationOption> buildOptions(
            @NonNull Fragment fragment,
            @NonNull List<String> availableCountryCodes
    ) {
        List<TopStationsLocationOption> options = new ArrayList<>();
        options.add(TopStationsLocationOption.worldwide(
                fragment.getString(R.string.settings_top_stations_location_worldwide)
        ));
        for (TopStationsGeography.RegionOption regionGroup
                : TopStationsGeography.getRegionGroupOptions(availableCountryCodes)) {
            options.add(TopStationsLocationOption.region(regionGroup.id, regionGroup.label));
        }
        for (TopStationsGeography.RegionOption continent
                : TopStationsGeography.getContinentOptions(availableCountryCodes)) {
            options.add(TopStationsLocationOption.continent(continent.id, continent.label));
        }
        for (TopStationsGeography.RegionOption subregion
                : TopStationsGeography.getAllSubregionOptions(availableCountryCodes)) {
            options.add(TopStationsLocationOption.subregion(subregion.id, subregion.label));
        }
        for (TopStationsGeography.CountryOption country
                : TopStationsGeography.getAllCountryOptions(availableCountryCodes)) {
            options.add(TopStationsLocationOption.country(country.countryCode, country.label));
        }
        return options;
    }

    @Nullable
    private static TopStationsLocationOption findMatchingOption(
            @NonNull List<TopStationsLocationOption> options,
            @NonNull TopStationsLocationSettings.Selection selection
    ) {
        for (TopStationsLocationOption option : options) {
            if (option.matches(selection)) {
                return option;
            }
        }
        return null;
    }

    @Nullable
    private static TopStationsLocationOption findExactOption(
            @NonNull List<TopStationsLocationOption> options,
            @Nullable String rawValue
    ) {
        for (TopStationsLocationOption option : options) {
            if (hasExactLabelMatch(option, rawValue)) {
                return option;
            }
        }
        return null;
    }

    private static boolean hasExactLabelMatch(@NonNull TopStationsLocationOption option,
                                              @Nullable String rawValue) {
        String normalizedValue = normalizeLabel(rawValue);
        String normalizedLabel = normalizeLabel(option.label);
        return normalizedValue != null
                && normalizedLabel != null
                && normalizedValue.equals(normalizedLabel);
    }

    @Nullable
    private static String normalizeLabel(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("&", "and")
                .replaceAll("[^a-z0-9]+", "");
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private static final class TopStationsLocationOption {
        @NonNull
        private final TopStationsLocationSettings.Scope scope;
        @Nullable
        private final String id;
        @NonNull
        private final String label;

        private TopStationsLocationOption(@NonNull TopStationsLocationSettings.Scope scope,
                                          @Nullable String id,
                                          @NonNull String label) {
            this.scope = scope;
            this.id = id;
            this.label = label;
        }

        @NonNull
        private static TopStationsLocationOption worldwide(@NonNull String label) {
            return new TopStationsLocationOption(
                    TopStationsLocationSettings.Scope.WORLDWIDE,
                    null,
                    label
            );
        }

        @NonNull
        private static TopStationsLocationOption region(@NonNull String id, @NonNull String label) {
            return new TopStationsLocationOption(
                    TopStationsLocationSettings.Scope.REGION,
                    id,
                    label
            );
        }

        @NonNull
        private static TopStationsLocationOption continent(@NonNull String id, @NonNull String label) {
            return new TopStationsLocationOption(
                    TopStationsLocationSettings.Scope.CONTINENT,
                    id,
                    label
            );
        }

        @NonNull
        private static TopStationsLocationOption subregion(@NonNull String id, @NonNull String label) {
            return new TopStationsLocationOption(
                    TopStationsLocationSettings.Scope.SUBREGION,
                    id,
                    label
            );
        }

        @NonNull
        private static TopStationsLocationOption country(@NonNull String id, @NonNull String label) {
            return new TopStationsLocationOption(
                    TopStationsLocationSettings.Scope.COUNTRY,
                    id,
                    label
            );
        }

        private boolean matches(@NonNull TopStationsLocationSettings.Selection selection) {
            if (scope != selection.scope) {
                return false;
            }
            if (scope == TopStationsLocationSettings.Scope.WORLDWIDE) {
                return true;
            }
            return id != null && id.equals(selection.selectedId);
        }

        @NonNull
        private TopStationsLocationSettings.Selection toSelection() {
            switch (scope) {
                case REGION:
                    return TopStationsLocationSettings.Selection.region(requireId());
                case CONTINENT:
                    return TopStationsLocationSettings.Selection.continent(requireId());
                case SUBREGION:
                    return TopStationsLocationSettings.Selection.subregion(requireId());
                case COUNTRY:
                    return TopStationsLocationSettings.Selection.country(requireId());
                case WORLDWIDE:
                default:
                    return TopStationsLocationSettings.Selection.worldwide();
            }
        }

        @NonNull
        private String requireId() {
            if (id == null) {
                throw new IllegalStateException("Location option id is required for " + scope);
            }
            return id;
        }
    }
}
