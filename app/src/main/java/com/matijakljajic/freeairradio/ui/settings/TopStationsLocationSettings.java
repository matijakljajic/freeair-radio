package com.matijakljajic.freeairradio.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class TopStationsLocationSettings {

    private static final String PREFS_NAME = "top_stations_location_settings";
    private static final String KEY_SCOPE = "scope";
    private static final String KEY_SELECTED_ID = "selected_id";

    @NonNull
    private final SharedPreferences sharedPreferences;

    public TopStationsLocationSettings(@NonNull Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public Selection getSelection() {
        return new Selection(
                Scope.fromId(sharedPreferences.getString(KEY_SCOPE, null)),
                trimOrNull(sharedPreferences.getString(KEY_SELECTED_ID, null))
        );
    }

    public void setSelection(@NonNull Selection selection) {
        SharedPreferences.Editor editor = sharedPreferences.edit()
                .putString(KEY_SCOPE, selection.scope.id);

        putNullable(
                editor,
                KEY_SELECTED_ID,
                selection.scope == Scope.WORLDWIDE ? null : selection.selectedId
        );
        editor.apply();
    }

    private static void putNullable(@NonNull SharedPreferences.Editor editor,
                                    @NonNull String key,
                                    @Nullable String value) {
        String trimmedValue = trimOrNull(value);
        if (trimmedValue == null) {
            editor.remove(key);
            return;
        }
        editor.putString(key, trimmedValue);
    }

    @Nullable
    private static String trimOrNull(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    public enum Scope {
        WORLDWIDE("worldwide"),
        REGION("region"),
        CONTINENT("continent"),
        SUBREGION("subregion"),
        COUNTRY("country");

        @NonNull
        private final String id;

        Scope(@NonNull String id) {
            this.id = id;
        }

        @NonNull
        static Scope fromId(@Nullable String id) {
            if (id != null) {
                for (Scope scope : values()) {
                    if (scope.id.equals(id)) {
                        return scope;
                    }
                }
            }
            return WORLDWIDE;
        }
    }

    public static final class Selection {
        @NonNull
        public final Scope scope;
        @Nullable
        public final String selectedId;

        public Selection(@NonNull Scope scope,
                         @Nullable String selectedId) {
            this.scope = scope;
            this.selectedId = scope == Scope.WORLDWIDE ? null : trimOrNull(selectedId);
        }

        @NonNull
        public static Selection worldwide() {
            return new Selection(Scope.WORLDWIDE, null);
        }

        @NonNull
        public static Selection continent(@NonNull String continentId) {
            return new Selection(Scope.CONTINENT, continentId);
        }

        @NonNull
        public static Selection region(@NonNull String regionId) {
            return new Selection(Scope.REGION, regionId);
        }

        @NonNull
        public static Selection subregion(@NonNull String subregionId) {
            return new Selection(Scope.SUBREGION, subregionId);
        }

        @NonNull
        public static Selection country(@NonNull String countryCode) {
            return new Selection(Scope.COUNTRY, countryCode);
        }
    }
}
