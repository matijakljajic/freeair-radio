package com.matijakljajic.freeairradio.ui.util;

import androidx.annotation.NonNull;

import com.matijakljajic.freeairradio.data.model.Station;

import java.util.ArrayList;
import java.util.List;

public final class StationDisplayFormatter {

    private StationDisplayFormatter() {
    }

    @NonNull
    public static String formatStationDetails(@NonNull Station station) {
        String countryDisplay = formatCountryForDisplay(station.getCountry());
        String tags = formatTags(station);
        if (isUnknown(tags)) {
            return countryDisplay;
        }
        return countryDisplay + " • " + tags;
    }

    @NonNull
    public static String formatTags(@NonNull Station station) {
        return formatTagsForDisplay(station.getTags());
    }

    @NonNull
    private static String formatTagsForDisplay(@NonNull String tags) {
        if (isUnknown(tags)) {
            return Station.UNKNOWN;
        }

        String[] rawTags = tags.split(",");
        List<String> cleanedTags = new ArrayList<>(rawTags.length);
        for (String rawTag : rawTags) {
            String cleanedTag = rawTag.trim();
            if (!cleanedTag.isEmpty()) {
                cleanedTags.add(cleanedTag);
            }
        }

        if (cleanedTags.isEmpty()) {
            return Station.UNKNOWN;
        }

        return String.join(", ", cleanedTags);
    }

    @NonNull
    private static String formatCountryForDisplay(@NonNull String country) {
        // TODO: replace country text with a flag icon, and use a black flag for UNKNOWN.
        if (isUnknown(country)) {
            return Station.UNKNOWN;
        }
        return country;
    }

    private static boolean isUnknown(@NonNull String value) {
        return Station.UNKNOWN.equals(value);
    }
}
