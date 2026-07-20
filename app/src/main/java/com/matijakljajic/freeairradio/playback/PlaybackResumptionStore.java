package com.matijakljajic.freeairradio.playback;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import java.util.Locale;

public final class PlaybackResumptionStore {

    private static final String PREFS_NAME = "playback_resumption";
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_STREAM_URL = "stream_url";
    private static final String KEY_RESOLVED_STREAM_URL = "resolved_stream_url";
    private static final String KEY_HOMEPAGE = "homepage";
    private static final String KEY_FAVICON = "favicon";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_COUNTRY_CODE = "country_code";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_TAGS = "tags";
    private static final String KEY_CODEC = "codec";
    private static final String KEY_BITRATE = "bitrate";
    private static final String KEY_HLS = "hls";
    private static final String KEY_ORIGIN = "origin";
    private static final int NULLABLE_BOOLEAN_UNKNOWN = -1;
    private static final int NULLABLE_BOOLEAN_FALSE = 0;
    private static final int NULLABLE_BOOLEAN_TRUE = 1;

    @NonNull
    private final SharedPreferences sharedPreferences;

    public PlaybackResumptionStore(@NonNull Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveLastPlayedStation(@NonNull Station station) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ID, station.getId());
        editor.putString(KEY_NAME, station.getName());
        editor.putString(KEY_STREAM_URL, station.getStreamUrl());
        putNullableString(editor, KEY_RESOLVED_STREAM_URL, station.getResolvedStreamUrl());
        putNullableString(editor, KEY_HOMEPAGE, station.getHomepage());
        putNullableString(editor, KEY_FAVICON, station.getFavicon());
        editor.putString(KEY_COUNTRY, station.getCountryName());
        editor.putString(KEY_COUNTRY_CODE, station.getCountryCode());
        editor.putString(KEY_LANGUAGE, station.getLanguage());
        editor.putString(KEY_TAGS, station.getTags());
        editor.putString(KEY_CODEC, station.getCodec());
        editor.putInt(KEY_BITRATE, station.getBitrate());
        editor.putInt(KEY_HLS, encodeNullableBoolean(station.getHls()));
        editor.putString(KEY_ORIGIN, station.getOrigin().name());
        editor.apply();
    }

    @Nullable
    public Station getLastPlayedStation() {
        String id = sharedPreferences.getString(KEY_ID, null);
        String name = sharedPreferences.getString(KEY_NAME, null);
        String streamUrl = sharedPreferences.getString(KEY_STREAM_URL, null);
        if (id == null || name == null || streamUrl == null) {
            return null;
        }

        Station.Builder builder = Station.builder(
                id,
                name,
                streamUrl,
                parseOrigin(id, sharedPreferences.getString(KEY_ORIGIN, null))
        );
        builder.setResolvedStreamUrl(sharedPreferences.getString(KEY_RESOLVED_STREAM_URL, null));
        builder.setHomepage(sharedPreferences.getString(KEY_HOMEPAGE, null));
        builder.setFavicon(sharedPreferences.getString(KEY_FAVICON, null));
        builder.setCountryName(sharedPreferences.getString(KEY_COUNTRY, null));
        builder.setCountryCode(sharedPreferences.getString(KEY_COUNTRY_CODE, null));
        builder.setLanguage(sharedPreferences.getString(KEY_LANGUAGE, null));
        builder.setTags(sharedPreferences.getString(KEY_TAGS, null));
        builder.setCodec(sharedPreferences.getString(KEY_CODEC, null));
        builder.setBitrate(sharedPreferences.getInt(KEY_BITRATE, 0));
        builder.setHls(decodeNullableBoolean(sharedPreferences.getInt(KEY_HLS, NULLABLE_BOOLEAN_UNKNOWN)));
        return builder.build();
    }

    private static void putNullableString(@NonNull SharedPreferences.Editor editor,
                                          @NonNull String key,
                                          @Nullable String value) {
        if (value == null) {
            editor.remove(key);
            return;
        }
        editor.putString(key, value);
    }

    private static int encodeNullableBoolean(@Nullable Boolean value) {
        if (value == null) {
            return NULLABLE_BOOLEAN_UNKNOWN;
        }
        return value ? NULLABLE_BOOLEAN_TRUE : NULLABLE_BOOLEAN_FALSE;
    }

    @Nullable
    private static Boolean decodeNullableBoolean(int value) {
        if (value == NULLABLE_BOOLEAN_TRUE) {
            return true;
        }
        if (value == NULLABLE_BOOLEAN_FALSE) {
            return false;
        }
        return null;
    }

    @NonNull
    private static StationOrigin parseOrigin(@NonNull String stationId,
                                             @Nullable String originName) {
        if (originName != null) {
            try {
                return StationOrigin.valueOf(originName.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fall back to the station-id prefix when saved data is missing or malformed.
            }
        }

        return stationId.startsWith("LOCAL:")
                ? StationOrigin.LOCAL_USER
                : StationOrigin.RADIO_BROWSER;
    }
}
