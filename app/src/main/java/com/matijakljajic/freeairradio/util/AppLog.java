package com.matijakljajic.freeairradio.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.BuildConfig;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AppLog {

    private static final String NONE = "<none>";

    private AppLog() {
    }

    public static void d(@NonNull String tag, @NonNull String message) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }

    @NonNull
    public static String value(@Nullable String value) {
        return value != null ? value : NONE;
    }

    @NonNull
    public static String stationName(@Nullable String stationName) {
        return value(stationName);
    }

    @NonNull
    public static String redactUrl(@Nullable String url) {
        if (url == null) {
            return NONE;
        }

        try {
            URI uri = URI.create(url);
            String query = uri.getQuery();
            if (query == null || query.isEmpty()) {
                return uri.toString();
            }

            String[] queryParts = query.split("&");
            List<String> redactedParts = new ArrayList<>(queryParts.length);
            for (String queryPart : queryParts) {
                int separatorIndex = queryPart.indexOf('=');
                if (separatorIndex <= 0) {
                    redactedParts.add(queryPart);
                    continue;
                }

                String key = queryPart.substring(0, separatorIndex);
                String value = queryPart.substring(separatorIndex + 1);
                redactedParts.add(key + "=" + (isSensitiveQueryKey(key) ? "<redacted>" : value));
            }

            return new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    String.join("&", redactedParts),
                    uri.getFragment()
            ).toString();
        } catch (Exception ignored) {
            return url;
        }
    }

    private static boolean isSensitiveQueryKey(@NonNull String key) {
        String lowerCaseKey = key.toLowerCase(Locale.ROOT);
        return "token".equals(lowerCaseKey)
                || "sig".equals(lowerCaseKey)
                || "signature".equals(lowerCaseKey)
                || "expires".equals(lowerCaseKey)
                || "auth".equals(lowerCaseKey)
                || "key".equals(lowerCaseKey)
                || "session".equals(lowerCaseKey);
    }
}
