package com.matijakljajic.freeairradio.ui.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.R;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TopStationsGeography {

    private static final Comparator<RegionOption> REGION_LABEL_COMPARATOR =
            Comparator.comparing(option -> option.label, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<CountryOption> COUNTRY_LABEL_COMPARATOR =
            Comparator.comparing(option -> option.label, String.CASE_INSENSITIVE_ORDER);
    @NonNull
    private static final Map<String, RegionOption> CONTINENTS = new LinkedHashMap<>();
    @NonNull
    private static final Map<String, RegionGroupSpec> REGION_GROUPS = new LinkedHashMap<>();
    @NonNull
    private static final Map<String, SubregionSpec> SUBREGIONS = new LinkedHashMap<>();
    @NonNull
    private static final Map<String, CountryGeo> COUNTRIES = new LinkedHashMap<>();
    @NonNull
    private static final Map<String, String> COUNTRY_LABEL_OVERRIDES = new LinkedHashMap<>();

    static {
        COUNTRY_LABEL_OVERRIDES.put("BQ", "Caribbean Netherlands");
        COUNTRY_LABEL_OVERRIDES.put("CW", "Curacao");
        COUNTRY_LABEL_OVERRIDES.put("MF", "Saint Martin");
        COUNTRY_LABEL_OVERRIDES.put("SX", "Sint Maarten");
        COUNTRY_LABEL_OVERRIDES.put("XK", "Kosovo");

        addCustomRegionGroups();

        addSubregion("africa", "Africa", "northern_africa", "Northern Africa",
                "DZ", "EG", "LY", "MA", "SD", "TN", "EH");
        addSubregion("africa", "Africa", "western_africa", "Western Africa",
                "BJ", "BF", "CV", "CI", "GM", "GH", "GN", "GW", "LR", "ML",
                "MR", "NE", "NG", "SH", "SN", "SL", "TG");
        addSubregion("africa", "Africa", "middle_africa", "Middle Africa",
                "AO", "CM", "CF", "TD", "CG", "CD", "GQ", "GA", "ST");
        addSubregion("africa", "Africa", "eastern_africa", "Eastern Africa",
                "BI", "KM", "DJ", "ER", "ET", "KE", "MG", "MW", "MU", "YT",
                "MZ", "RE", "RW", "SC", "SO", "SS", "TZ", "UG", "ZM", "ZW");
        addSubregion("africa", "Africa", "southern_africa", "Southern Africa",
                "BW", "LS", "NA", "SZ", "ZA");

        addSubregion("americas", "Americas", "northern_america", "Northern America",
                "BM", "CA", "GL", "PM", "US");
        addSubregion("americas", "Americas", "caribbean", "Caribbean",
                "AI", "AG", "AW", "BS", "BB", "BQ", "VG", "KY", "CU", "CW",
                "DM", "DO", "GD", "GP", "HT", "JM", "MQ", "MS", "PR", "BL",
                "KN", "LC", "MF", "VC", "SX", "TT", "TC", "VI");
        addSubregion("americas", "Americas", "central_america", "Central America",
                "BZ", "CR", "SV", "GT", "HN", "MX", "NI", "PA");
        addSubregion("americas", "Americas", "south_america", "South America",
                "AR", "BO", "BR", "CL", "CO", "EC", "FK", "GF", "GY", "PY",
                "PE", "SR", "UY", "VE");

        addSubregion("asia", "Asia", "central_asia", "Central Asia",
                "KZ", "KG", "TJ", "TM", "UZ");
        addSubregion("asia", "Asia", "eastern_asia", "Eastern Asia",
                "CN", "HK", "JP", "KP", "KR", "MO", "MN", "TW");
        addSubregion("asia", "Asia", "southeastern_asia", "South-eastern Asia",
                "BN", "KH", "ID", "LA", "MY", "MM", "PH", "SG", "TH", "TL", "VN");
        addSubregion("asia", "Asia", "southern_asia", "Southern Asia",
                "AF", "BD", "BT", "IN", "MV", "NP", "PK", "LK");
        addSubregion("asia", "Asia", "western_asia", "Western Asia",
                "AM", "AZ", "BH", "CY", "GE", "IQ", "IL", "IR", "JO", "KW",
                "LB", "OM", "PS", "QA", "SA", "SY", "TR", "AE", "YE");

        addSubregion("europe", "Europe", "northern_europe", "Northern Europe",
                "AX", "DK", "EE", "FO", "FI", "GG", "IS", "IE", "IM", "JE",
                "LV", "LT", "NO", "SJ", "SE", "GB");
        addSubregion("europe", "Europe", "eastern_europe", "Eastern Europe",
                "BY", "BG", "CZ", "HU", "PL", "MD", "RO", "RU", "SK", "UA");
        addSubregion("europe", "Europe", "southern_europe", "Southern Europe",
                "AL", "AD", "BA", "HR", "GI", "GR", "VA", "IT", "XK", "MT",
                "ME", "MK", "PT", "SM", "RS", "SI", "ES");
        addSubregion("europe", "Europe", "western_europe", "Western Europe",
                "AT", "BE", "FR", "DE", "LI", "LU", "MC", "NL", "CH");

        addSubregion("oceania", "Oceania", "australia_new_zealand", "Australia and New Zealand",
                "AU", "NF", "NZ");
        addSubregion("oceania", "Oceania", "melanesia", "Melanesia",
                "FJ", "NC", "PG", "SB", "VU");
        addSubregion("oceania", "Oceania", "micronesia", "Micronesia",
                "GU", "KI", "MH", "FM", "NR", "MP", "PW");
        addSubregion("oceania", "Oceania", "polynesia", "Polynesia",
                "AS", "CK", "PF", "NU", "PN", "WS", "TK", "TO", "TV", "WF");
    }

    private TopStationsGeography() {
    }

    @NonNull
    public static String getSelectionLabel(@NonNull Context context,
                                           @NonNull TopStationsLocationSettings.Selection selection) {
        if (selection.scope == TopStationsLocationSettings.Scope.WORLDWIDE) {
            return context.getString(R.string.settings_top_stations_location_worldwide);
        }

        String selectedId = selection.selectedId;
        if (selectedId == null) {
            return context.getString(R.string.settings_top_stations_location_worldwide);
        }

        if (selection.scope == TopStationsLocationSettings.Scope.CONTINENT) {
            RegionOption continent = CONTINENTS.get(selectedId);
            return continent != null ? continent.label : selectedId;
        }
        if (selection.scope == TopStationsLocationSettings.Scope.REGION) {
            RegionGroupSpec regionGroup = REGION_GROUPS.get(selectedId);
            return regionGroup != null ? regionGroup.label : selectedId;
        }
        if (selection.scope == TopStationsLocationSettings.Scope.SUBREGION) {
            SubregionSpec subregion = SUBREGIONS.get(selectedId);
            return subregion != null ? subregion.label : selectedId;
        }
        return displayCountryName(selectedId);
    }

    @NonNull
    public static List<RegionOption> getContinentOptions(@NonNull List<String> availableCountryCodes) {
        Map<String, RegionOption> continents = new LinkedHashMap<>();
        for (CountryGeo country : collectAvailableCountries(availableCountryCodes)) {
            continents.put(country.continentId, CONTINENTS.get(country.continentId));
        }

        List<RegionOption> options = new ArrayList<>(continents.values());
        options.sort(REGION_LABEL_COMPARATOR);
        return options;
    }

    @NonNull
    public static List<RegionOption> getRegionGroupOptions(@NonNull List<String> availableCountryCodes) {
        Set<String> availableCodes = new LinkedHashSet<>(normalizeCountryCodes(availableCountryCodes));
        List<RegionOption> options = new ArrayList<>();

        for (RegionGroupSpec regionGroup : REGION_GROUPS.values()) {
            if (regionGroup.hasAnyCountry(availableCodes)) {
                options.add(new RegionOption(regionGroup.id, regionGroup.label));
            }
        }

        options.sort(REGION_LABEL_COMPARATOR);
        return options;
    }

    @NonNull
    public static List<RegionOption> getAllSubregionOptions(@NonNull List<String> availableCountryCodes) {
        return collectSubregionOptions(null, availableCountryCodes);
    }

    @NonNull
    public static List<CountryOption> getAllCountryOptions(@NonNull List<String> availableCountryCodes) {
        return collectCountryOptions(null, availableCountryCodes);
    }

    @Nullable
    public static CountryOption findClosestCountryOption(@Nullable String rawCountry,
                                                         @NonNull List<String> availableCountryCodes) {
        String normalizedQuery = normalizeMatchingValue(rawCountry);
        if (normalizedQuery == null) {
            return null;
        }

        List<CountryOption> options = collectCountryOptions(null, availableCountryCodes);
        CountryOption bestOption = null;
        int bestScore = Integer.MIN_VALUE;
        for (CountryOption option : options) {
            int score = scoreCountryMatch(normalizedQuery, option);
            if (score > bestScore) {
                bestScore = score;
                bestOption = option;
            }
        }

        return bestScore > 0 ? bestOption : null;
    }

    @NonNull
    private static List<RegionOption> collectSubregionOptions(@Nullable String continentId,
                                                              @NonNull List<String> availableCountryCodes) {
        Map<String, RegionOption> subregions = new LinkedHashMap<>();
        for (CountryGeo country : collectAvailableCountries(availableCountryCodes)) {
            if (continentId != null && !continentId.equals(country.continentId)) {
                continue;
            }
            SubregionSpec subregion = SUBREGIONS.get(country.subregionId);
            if (subregion != null) {
                subregions.put(subregion.id, new RegionOption(subregion.id, subregion.label));
            }
        }

        List<RegionOption> options = new ArrayList<>(subregions.values());
        options.sort(REGION_LABEL_COMPARATOR);
        return options;
    }

    @NonNull
    private static List<CountryOption> collectCountryOptions(@Nullable String subregionId,
                                                             @NonNull List<String> availableCountryCodes) {
        List<CountryOption> countries = new ArrayList<>();
        for (CountryGeo country : collectAvailableCountries(availableCountryCodes)) {
            if (subregionId != null && !subregionId.equals(country.subregionId)) {
                continue;
            }
            countries.add(new CountryOption(country.countryCode, displayCountryName(country.countryCode)));
        }

        countries.sort(COUNTRY_LABEL_COMPARATOR);
        return countries;
    }

    @NonNull
    public static List<String> resolveCountryCodes(@NonNull TopStationsLocationSettings.Selection selection,
                                                   @NonNull List<String> availableCountryCodes) {
        List<CountryGeo> availableCountries = collectAvailableCountries(availableCountryCodes);
        if (availableCountries.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> resolvedCountryCodes = new ArrayList<>();
        for (CountryGeo country : availableCountries) {
            if (matchesSelection(country, selection)) {
                resolvedCountryCodes.add(country.countryCode);
            }
        }
        return resolvedCountryCodes;
    }

    private static int scoreCountryMatch(@NonNull String normalizedQuery,
                                         @NonNull CountryOption option) {
        String normalizedCountryCode = normalizeMatchingValue(option.countryCode);
        String normalizedLabel = normalizeMatchingValue(option.label);
        if (normalizedCountryCode == null || normalizedLabel == null) {
            return Integer.MIN_VALUE;
        }

        if (normalizedQuery.equals(normalizedCountryCode)) {
            return 10_000;
        }
        if (normalizedQuery.equals(normalizedLabel)) {
            return 9_000;
        }

        if (normalizedQuery.length() >= 3) {
            if (normalizedLabel.startsWith(normalizedQuery) || normalizedQuery.startsWith(normalizedLabel)) {
                return 6_000 - Math.abs(normalizedLabel.length() - normalizedQuery.length());
            }
            if (normalizedLabel.contains(normalizedQuery) || normalizedQuery.contains(normalizedLabel)) {
                return 5_000 - Math.abs(normalizedLabel.length() - normalizedQuery.length());
            }
        }

        int distance = levenshteinDistance(normalizedQuery, normalizedLabel);
        int maxDistance = Math.max(1, Math.min(4, normalizedLabel.length() / 4));
        if (distance <= maxDistance) {
            return 3_000 - (distance * 100) - Math.abs(normalizedLabel.length() - normalizedQuery.length());
        }
        return Integer.MIN_VALUE;
    }

    private static boolean matchesSelection(@NonNull CountryGeo country,
                                            @NonNull TopStationsLocationSettings.Selection selection) {
        if (selection.selectedId == null) {
            return false;
        }

        switch (selection.scope) {
            case REGION:
                RegionGroupSpec regionGroup = REGION_GROUPS.get(selection.selectedId);
                if (regionGroup == null) {
                    return false;
                }
                return regionGroup.countryCodes.contains(country.countryCode);
            case CONTINENT:
                return selection.selectedId.equals(country.continentId);
            case SUBREGION:
                return selection.selectedId.equals(country.subregionId);
            case COUNTRY:
                return selection.selectedId.equals(country.countryCode);
            case WORLDWIDE:
            default:
                return false;
        }
    }

    @NonNull
    private static List<CountryGeo> collectAvailableCountries(@NonNull List<String> availableCountryCodes) {
        Set<String> uniqueCountryCodes = new LinkedHashSet<>(normalizeCountryCodes(availableCountryCodes));

        List<CountryGeo> countries = new ArrayList<>(uniqueCountryCodes.size());
        for (String countryCode : uniqueCountryCodes) {
            CountryGeo country = COUNTRIES.get(countryCode);
            if (country != null) {
                countries.add(country);
            }
        }
        countries.sort(Comparator.comparing(country -> displayCountryName(country.countryCode), String.CASE_INSENSITIVE_ORDER));
        return countries;
    }

    @NonNull
    private static String displayCountryName(@NonNull String countryCode) {
        String overrideLabel = COUNTRY_LABEL_OVERRIDES.get(countryCode);
        if (overrideLabel != null) {
            return overrideLabel;
        }

        String label = new Locale.Builder()
                .setRegion(countryCode)
                .build()
                .getDisplayCountry(Locale.getDefault());
        if (label == null) {
            return countryCode;
        }

        String trimmedLabel = label.trim();
        if (trimmedLabel.isEmpty() || countryCode.equalsIgnoreCase(trimmedLabel)) {
            return countryCode;
        }
        return trimmedLabel;
    }

    @Nullable
    private static String normalizeMatchingValue(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        String normalized = Normalizer.normalize(trimmedValue, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{Alnum}]+", "");
        return normalized.isEmpty() ? null : normalized;
    }

    private static int levenshteinDistance(@NonNull String left, @NonNull String right) {
        int[] costs = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) {
            costs[column] = column;
        }

        for (int row = 1; row <= left.length(); row++) {
            int previousDiagonal = costs[0];
            costs[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int previousAbove = costs[column];
                int substitutionCost = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
                costs[column] = Math.min(
                        Math.min(costs[column] + 1, costs[column - 1] + 1),
                        previousDiagonal + substitutionCost
                );
                previousDiagonal = previousAbove;
            }
        }
        return costs[right.length()];
    }

    @NonNull
    private static List<String> normalizeCountryCodes(@NonNull List<String> countryCodes) {
        List<String> normalizedCountryCodes = new ArrayList<>(countryCodes.size());
        for (String countryCode : countryCodes) {
            String normalizedCountryCode = normalizeCountryCode(countryCode);
            if (normalizedCountryCode != null && !normalizedCountryCodes.contains(normalizedCountryCode)) {
                normalizedCountryCodes.add(normalizedCountryCode);
            }
        }
        return normalizedCountryCodes;
    }

    @Nullable
    private static String normalizeCountryCode(@Nullable String countryCode) {
        if (countryCode == null) {
            return null;
        }
        String normalizedCountryCode = countryCode.trim().toUpperCase(Locale.ROOT);
        return normalizedCountryCode.length() == 2 ? normalizedCountryCode : null;
    }

    private static void addSubregion(@NonNull String continentId,
                                     @NonNull String continentLabel,
                                     @NonNull String subregionId,
                                     @NonNull String subregionLabel,
                                     @NonNull String... countryCodes) {
        CONTINENTS.put(continentId, new RegionOption(continentId, continentLabel));
        SUBREGIONS.put(subregionId, new SubregionSpec(subregionId, subregionLabel, continentId));
        for (String countryCode : countryCodes) {
            COUNTRIES.put(countryCode, new CountryGeo(countryCode, continentId, subregionId));
        }
    }

    private static void addCustomRegionGroups() {
        addRegionGroup("ex_yugoslavia", "Ex-Yugoslavia",
                "BA", "HR", "ME", "MK", "RS", "SI", "XK");
        addRegionGroup("nordics", "Nordics",
                "DK", "FI", "IS", "NO", "SE");
        addRegionGroup("baltics", "Baltics",
                "EE", "LV", "LT");
        addRegionGroup("benelux", "Benelux",
                "BE", "NL", "LU");
        addRegionGroup("dach", "DACH",
                "DE", "AT", "CH");
        addRegionGroup("iberia", "Iberia",
                "ES", "PT", "AD");
        addRegionGroup("british_irish", "British & Irish",
                "GB", "IE");
        addRegionGroup("scandinavia", "Scandinavia",
                "DK", "NO", "SE");
        addRegionGroup("caucasus", "Caucasus",
                "AM", "AZ", "GE");
        addRegionGroup("maghreb", "Maghreb",
                "DZ", "LY", "MR", "MA", "TN");
        addRegionGroup("levant", "Levant",
                "LB", "JO", "PS", "SY", "IL", "CY");
        addRegionGroup("gulf", "Gulf",
                "BH", "KW", "OM", "QA", "SA", "AE");
        addRegionGroup("southern_cone", "Southern Cone",
                "AR", "CL", "UY", "PY");
        addRegionGroup("andean", "Andean",
                "BO", "CO", "EC", "PE", "VE");
    }

    private static void addRegionGroup(@NonNull String regionId,
                                       @NonNull String regionLabel,
                                       @NonNull String... countryCodes) {
        REGION_GROUPS.put(regionId, new RegionGroupSpec(regionId, regionLabel, countryCodes));
    }

    public static final class RegionOption {
        @NonNull
        public final String id;
        @NonNull
        public final String label;

        RegionOption(@NonNull String id, @NonNull String label) {
            this.id = id;
            this.label = label;
        }
    }

    public static final class CountryOption {
        @NonNull
        public final String countryCode;
        @NonNull
        public final String label;

        CountryOption(@NonNull String countryCode, @NonNull String label) {
            this.countryCode = countryCode;
            this.label = label;
        }
    }

    private static final class CountryGeo {
        @NonNull
        private final String countryCode;
        @NonNull
        private final String continentId;
        @NonNull
        private final String subregionId;

        CountryGeo(@NonNull String countryCode,
                   @NonNull String continentId,
                   @NonNull String subregionId) {
            this.countryCode = countryCode;
            this.continentId = continentId;
            this.subregionId = subregionId;
        }
    }

    private static final class RegionGroupSpec {
        @NonNull
        private final String id;
        @NonNull
        private final String label;
        @NonNull
        private final Set<String> countryCodes;

        RegionGroupSpec(@NonNull String id,
                        @NonNull String label,
                        @NonNull String[] countryCodes) {
            this.id = id;
            this.label = label;
            this.countryCodes = new LinkedHashSet<>();
            for (String countryCode : countryCodes) {
                String normalizedCountryCode = normalizeCountryCode(countryCode);
                if (normalizedCountryCode != null) {
                    this.countryCodes.add(normalizedCountryCode);
                }
            }
        }

        private boolean hasAnyCountry(@NonNull Set<String> availableCountryCodes) {
            for (String countryCode : countryCodes) {
                if (availableCountryCodes.contains(countryCode)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class SubregionSpec {
        @NonNull
        private final String id;
        @NonNull
        private final String label;
        @NonNull
        private final String continentId;

        SubregionSpec(@NonNull String id,
                      @NonNull String label,
                      @NonNull String continentId) {
            this.id = id;
            this.label = label;
            this.continentId = continentId;
        }
    }
}
