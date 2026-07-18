package com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matijakljajic.freeairradio.BuildConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class RadioBrowserServerDirectory {

    private static volatile List<String> cachedServers = Collections.emptyList();
    private static final String BOOTSTRAP_BASE_URL = "https://all.api.radio-browser.info/";
    private static final String SERVER_DIRECTORY_URL = "https://all.api.radio-browser.info/json/servers";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .callTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();

    private RadioBrowserServerDirectory() {
    }

    @NonNull
    public static List<String> getCachedServers() {
        return cachedServers;
    }

    @NonNull
    public static String getBootstrapBaseUrl() {
        return BOOTSTRAP_BASE_URL;
    }

    @NonNull
    public static List<String> loadServers(boolean forceRefresh) {
        if (!forceRefresh && !cachedServers.isEmpty()) {
            return cachedServers;
        }
        return refresh();
    }

    @NonNull
    public static List<String> refresh() {
        cachedServers = discoverFreshBaseUrls();
        return cachedServers;
    }

    @NonNull
    private static List<String> discoverFreshBaseUrls() {
        List<String> directoryBaseUrls = discoverViaServerDirectory();
        if (!directoryBaseUrls.isEmpty()) {
            return shuffle(directoryBaseUrls);
        }

        List<String> dnsBaseUrls = discoverViaDns();
        if (dnsBaseUrls.isEmpty()) {
            return Collections.emptyList();
        }

        return shuffle(dnsBaseUrls);
    }

    @NonNull
    private static List<String> discoverViaDns() {
        Set<String> discoveredBaseUrls = new LinkedHashSet<>();
        try {
            InetAddress[] addresses = InetAddress.getAllByName("all.api.radio-browser.info");
            for (InetAddress address : addresses) {
                String host = address.getCanonicalHostName();
                if (!host.trim().isEmpty()) {
                    discoveredBaseUrls.add(normalizeBaseUrl(host));
                }
            }
        } catch (UnknownHostException ignored) {
            return Collections.emptyList();
        }
        return new ArrayList<>(discoveredBaseUrls);
    }

    @NonNull
    private static List<String> discoverViaServerDirectory() {
        Request request = new Request.Builder()
                .url(SERVER_DIRECTORY_URL)
                .header("User-Agent", BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME)
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return Collections.emptyList();
            }

            return parseBaseUrls(response.body().string());
        } catch (IOException ignored) {
            return Collections.emptyList();
        }
    }

    @NonNull
    private static List<String> shuffle(@NonNull List<String> baseUrls) {
        List<String> randomizedBaseUrls = new ArrayList<>(new LinkedHashSet<>(baseUrls));
        Collections.shuffle(randomizedBaseUrls, ThreadLocalRandom.current());
        return randomizedBaseUrls;
    }

    @NonNull
    static List<String> parseBaseUrls(@NonNull String json) {
        JsonElement parsedJson = JsonParser.parseString(json);
        JsonArray serverArray = extractServerArray(parsedJson);
        if (serverArray == null || serverArray.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> discoveredBaseUrls = new LinkedHashSet<>();
        for (JsonElement element : serverArray) {
            if (element == null || element.isJsonNull()) {
                continue;
            }

            String baseUrl = extractBaseUrl(element);
            if (baseUrl != null) {
                discoveredBaseUrls.add(baseUrl);
            }
        }

        if (discoveredBaseUrls.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(discoveredBaseUrls);
    }

    @Nullable
    private static JsonArray extractServerArray(@NonNull JsonElement parsedJson) {
        if (parsedJson.isJsonArray()) {
            return parsedJson.getAsJsonArray();
        }

        if (!parsedJson.isJsonObject()) {
            return null;
        }

        JsonObject object = parsedJson.getAsJsonObject();
        JsonElement servers = object.get("servers");
        if (servers != null && servers.isJsonArray()) {
            return servers.getAsJsonArray();
        }

        JsonElement data = object.get("data");
        if (data != null && data.isJsonArray()) {
            return data.getAsJsonArray();
        }

        return null;
    }

    @Nullable
    private static String extractBaseUrl(@NonNull JsonElement element) {
        if (element.isJsonPrimitive()) {
            return normalizeBaseUrl(element.getAsString());
        }

        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        String[] candidateKeys = {"url", "base_url", "baseUrl", "name", "server", "host", "server_name"};
        for (String candidateKey : candidateKeys) {
            JsonElement candidate = object.get(candidateKey);
            if (candidate != null && !candidate.isJsonNull()) {
                String normalized = normalizeBaseUrl(candidate.getAsString());
                if (normalized != null) {
                    return normalized;
                }
            }
        }
        return null;
    }

    @Nullable
    private static String normalizeBaseUrl(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        if (trimmedValue.startsWith("http://") || trimmedValue.startsWith("https://")) {
            return ensureTrailingSlash(trimmedValue);
        }

        return "https://" + ensureTrailingSlash(trimmedValue);
    }

    @NonNull
    private static String ensureTrailingSlash(@NonNull String value) {
        return value.endsWith("/") ? value : value + "/";
    }
}
