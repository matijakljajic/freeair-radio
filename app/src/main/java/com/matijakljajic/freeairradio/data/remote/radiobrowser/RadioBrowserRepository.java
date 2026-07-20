package com.matijakljajic.freeairradio.data.remote.radiobrowser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.dto.RadioBrowserCountryCodeDto;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.dto.RadioBrowserStationDto;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection.RadioBrowserServerSelector;
import com.matijakljajic.freeairradio.data.repository.StationRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public final class RadioBrowserRepository implements StationRepository {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_SERVER_ATTEMPTS = 3;
    private static final String RADIO_BROWSER_ID_PREFIX = "RADIO_BROWSER:";
    private static final String ORDER_CLICK_COUNT = "clickcount";
    private static final String ORDER_NAME = "name";
    @NonNull
    private static final Object COUNTRY_CODES_CACHE_LOCK = new Object();
    @Nullable
    private static List<String> cachedCountryCodes;
    @NonNull
    private static final ExecutorService WORKER_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RadioBrowserRepository");
        thread.setDaemon(true);
        return thread;
    });

    @NonNull
    private final RadioBrowserClient client;
    @NonNull
    private final RadioBrowserServerSelector serverSelector;
    @NonNull
    private final Handler mainHandler;

    public RadioBrowserRepository(@NonNull Context context) {
        this(
                RadioBrowserClient.getInstance(),
                new RadioBrowserServerSelector(context),
                new Handler(Looper.getMainLooper())
        );
    }

    RadioBrowserRepository(@NonNull RadioBrowserClient client,
                           @NonNull RadioBrowserServerSelector serverSelector,
                           @NonNull Handler mainHandler) {
        this.client = client;
        this.serverSelector = serverSelector;
        this.mainHandler = mainHandler;
    }

    @Override
    public void loadTopStations(@NonNull LoadCallback callback) {
        loadStationsAsync(
                callback,
                api -> fetchStations(api.loadTopStations(DEFAULT_LIMIT, true), "load top stations")
        );
    }

    @Override
    public void loadTopStationsByCountryCodes(@NonNull List<String> countryCodes,
                                              @NonNull LoadCallback callback) {
        List<String> normalizedCountryCodes = normalizeCountryCodes(countryCodes);
        if (normalizedCountryCodes.isEmpty()) {
            loadTopStations(callback);
            return;
        }

        loadStationsAsync(
                callback,
                api -> fetchTopStationsByCountryCodes(api, normalizedCountryCodes)
        );
    }

    @Override
    public void loadAvailableCountryCodes(@NonNull CountryCodesCallback callback) {
        List<String> cachedCountryCodes = getCachedCountryCodes();
        if (cachedCountryCodes != null) {
            postCountryCodesLoaded(callback, cachedCountryCodes);
            return;
        }
        startWorker(() -> loadCountryCodes(callback, 0));
    }

    @Override
    public void searchStationsByName(@NonNull String query, @NonNull LoadCallback callback) {
        String normalizedQuery = query.trim();
        loadStationsAsync(
                callback,
                api -> fetchStations(
                        api.searchStationsByName(normalizedQuery, DEFAULT_LIMIT, true),
                        "search stations"
                )
        );
    }

    @Override
    public void reportStationUsage(@NonNull Station station) {
        if (station.getOrigin() != StationOrigin.RADIO_BROWSER) {
            return;
        }

        String stationUuid = extractStationUuid(station.getId());
        if (stationUuid == null) {
            return;
        }

        startWorker(() -> reportStationUsage(stationUuid));
    }

    private void loadStationsAsync(@NonNull LoadCallback callback,
                                   @NonNull StationRequest request) {
        startWorker(() -> loadStations(request, callback, 0));
    }

    private void loadStations(@NonNull StationRequest request,
                              @NonNull LoadCallback callback,
                              int attempt) {
        String baseUrl = serverSelector.getSelectedBaseUrl();
        if (baseUrl == null) {
            postError(callback, new IOException("No Radio Browser servers available"));
            return;
        }

        try {
            List<RadioBrowserStationDto> stations = request.fetch(client.create(baseUrl));
            serverSelector.rememberSuccess(baseUrl);
            postStationsLoaded(callback, mapStations(stations));
        } catch (IOException exception) {
            retryStationsOrError(request, callback, attempt, baseUrl, exception);
        }
    }

    private void retryStationsOrError(@NonNull StationRequest request,
                                      @NonNull LoadCallback callback,
                                      int attempt,
                                      @Nullable String failedBaseUrl,
                                      @NonNull IOException error) {
        if (attempt + 1 < MAX_SERVER_ATTEMPTS && selectAlternativeServer(failedBaseUrl)) {
            loadStations(request, callback, attempt + 1);
            return;
        }
        postError(callback, error);
    }

    private void loadCountryCodes(@NonNull CountryCodesCallback callback, int attempt) {
        String baseUrl = serverSelector.getSelectedBaseUrl();
        if (baseUrl == null) {
            postCountryCodesError(callback, new IOException("No Radio Browser servers available"));
            return;
        }

        try {
            List<String> countryCodes = fetchCountryCodes(client.create(baseUrl));
            cacheCountryCodes(countryCodes);
            serverSelector.rememberSuccess(baseUrl);
            postCountryCodesLoaded(callback, countryCodes);
        } catch (IOException exception) {
            retryCountryCodesOrError(callback, attempt, baseUrl, exception);
        }
    }

    private void retryCountryCodesOrError(@NonNull CountryCodesCallback callback,
                                          int attempt,
                                          @Nullable String failedBaseUrl,
                                          @NonNull IOException error) {
        if (attempt + 1 < MAX_SERVER_ATTEMPTS && selectAlternativeServer(failedBaseUrl)) {
            loadCountryCodes(callback, attempt + 1);
            return;
        }
        postCountryCodesError(callback, error);
    }

    private boolean selectAlternativeServer(@Nullable String failedBaseUrl) {
        return serverSelector.rotateToNextServer(failedBaseUrl)
                || serverSelector.refreshAndRotate(failedBaseUrl);
    }

    private void reportStationUsage(@NonNull String stationUuid) {
        String baseUrl = serverSelector.getSelectedBaseUrl();
        if (baseUrl == null) {
            return;
        }

        try {
            Response<Void> response = client.create(baseUrl)
                    .reportStationUsage(stationUuid)
                    .execute();
            if (response.isSuccessful()) {
                serverSelector.rememberSuccess(baseUrl);
            }
        } catch (IOException ignored) {
            // Fire-and-forget tracking must never block playback.
        }
    }

    @NonNull
    private List<RadioBrowserStationDto> fetchTopStationsByCountryCodes(@NonNull RadioBrowserApi api,
                                                                        @NonNull List<String> countryCodes)
            throws IOException {
        Map<String, RadioBrowserStationDto> mergedStations = new LinkedHashMap<>();

        for (String countryCode : countryCodes) {
            List<RadioBrowserStationDto> stations = fetchStations(
                    api.loadTopStationsByCountryCode(
                            countryCode,
                            DEFAULT_LIMIT,
                            true,
                            ORDER_CLICK_COUNT,
                            true
                    ),
                    "load top stations for " + countryCode
            );
            mergeStations(mergedStations, stations);
        }

        List<RadioBrowserStationDto> sortedStations = new ArrayList<>(mergedStations.values());
        sortedStations.sort(Comparator
                .comparingInt(RadioBrowserStationDto::getClickCount)
                .reversed()
                .thenComparing(dto -> safeString(dto.getName()), String.CASE_INSENSITIVE_ORDER));

        if (sortedStations.size() > DEFAULT_LIMIT) {
            return new ArrayList<>(sortedStations.subList(0, DEFAULT_LIMIT));
        }
        return sortedStations;
    }

    @NonNull
    private List<String> fetchCountryCodes(@NonNull RadioBrowserApi api) throws IOException {
        Response<List<RadioBrowserCountryCodeDto>> response = api.loadCountryCodes(true, ORDER_NAME).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Failed to load country codes: " + response.code());
        }
        return mapCountryCodes(response.body());
    }

    @NonNull
    private List<RadioBrowserStationDto> fetchStations(@NonNull retrofit2.Call<List<RadioBrowserStationDto>> call,
                                                       @NonNull String action)
            throws IOException {
        Response<List<RadioBrowserStationDto>> response = call.execute();
        if (!response.isSuccessful()) {
            throw new IOException("Failed to " + action + ": " + response.code());
        }
        List<RadioBrowserStationDto> stations = response.body();
        return stations == null ? Collections.emptyList() : stations;
    }

    private void mergeStations(@NonNull Map<String, RadioBrowserStationDto> mergedStations,
                               @NonNull List<RadioBrowserStationDto> stations) {
        for (RadioBrowserStationDto station : stations) {
            String stationUuid = trimRequired(station.getStationUuid());
            if (stationUuid == null) {
                continue;
            }

            RadioBrowserStationDto currentStation = mergedStations.get(stationUuid);
            if (currentStation == null || station.getClickCount() > currentStation.getClickCount()) {
                mergedStations.put(stationUuid, station);
            }
        }
    }

    private void startWorker(@NonNull Runnable work) {
        WORKER_EXECUTOR.execute(work);
    }

    private void postStationsLoaded(@NonNull LoadCallback callback,
                                    @NonNull List<Station> stations) {
        postToMain(() -> callback.onStationsLoaded(stations));
    }

    private void postError(@NonNull LoadCallback callback,
                           @NonNull Throwable throwable) {
        postToMain(() -> callback.onError(throwable));
    }

    private void postCountryCodesLoaded(@NonNull CountryCodesCallback callback,
                                        @NonNull List<String> countryCodes) {
        postToMain(() -> callback.onCountryCodesLoaded(countryCodes));
    }

    private void postCountryCodesError(@NonNull CountryCodesCallback callback,
                                       @NonNull Throwable throwable) {
        postToMain(() -> callback.onError(throwable));
    }

    @Nullable
    private static List<String> getCachedCountryCodes() {
        synchronized (COUNTRY_CODES_CACHE_LOCK) {
            return cachedCountryCodes == null ? null : new ArrayList<>(cachedCountryCodes);
        }
    }

    private static void cacheCountryCodes(@NonNull List<String> countryCodes) {
        synchronized (COUNTRY_CODES_CACHE_LOCK) {
            cachedCountryCodes = new ArrayList<>(countryCodes);
        }
    }

    private void postToMain(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
            return;
        }
        mainHandler.post(action);
    }

    @Nullable
    private static String extractStationUuid(@NonNull String stationId) {
        if (!stationId.startsWith(RADIO_BROWSER_ID_PREFIX)) {
            return null;
        }

        String stationUuid = stationId.substring(RADIO_BROWSER_ID_PREFIX.length()).trim();
        return stationUuid.isEmpty() ? null : stationUuid;
    }

    @NonNull
    private static List<String> normalizeCountryCodes(@NonNull List<String> countryCodes) {
        Set<String> normalizedCountryCodes = new LinkedHashSet<>();
        for (String countryCode : countryCodes) {
            String normalizedCountryCode = trimCountryCode(countryCode);
            if (normalizedCountryCode != null) {
                normalizedCountryCodes.add(normalizedCountryCode);
            }
        }
        return new ArrayList<>(normalizedCountryCodes);
    }

    @Nullable
    private static String trimCountryCode(@Nullable String countryCode) {
        String normalizedCountryCode = trimNullable(countryCode);
        if (normalizedCountryCode == null) {
            return null;
        }

        normalizedCountryCode = normalizedCountryCode.toUpperCase(Locale.ROOT);
        return normalizedCountryCode.length() == 2 ? normalizedCountryCode : null;
    }

    @NonNull
    static List<Station> mapStations(@Nullable List<RadioBrowserStationDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }

        List<Station> stations = new ArrayList<>(dtos.size());
        for (RadioBrowserStationDto dto : dtos) {
            Station station = mapStation(dto);
            if (station != null) {
                stations.add(station);
            }
        }
        return stations;
    }

    @NonNull
    static List<String> mapCountryCodes(@Nullable List<RadioBrowserCountryCodeDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> countryCodes = new ArrayList<>(dtos.size());
        for (RadioBrowserCountryCodeDto dto : dtos) {
            String countryCode = trimCountryCode(dto.getCountryCode());
            if (countryCode != null && !countryCodes.contains(countryCode)) {
                countryCodes.add(countryCode);
            }
        }
        return countryCodes;
    }

    @Nullable
    static Station mapStation(@Nullable RadioBrowserStationDto dto) {
        if (dto == null) {
            return null;
        }

        String stationUuid = trimRequired(dto.getStationUuid());
        String name = trimRequired(dto.getName());
        String streamUrl = trimRequired(dto.getUrl());
        if (stationUuid == null || name == null || streamUrl == null) {
            return null;
        }

        return Station.builder("RADIO_BROWSER:" + stationUuid, name, streamUrl, StationOrigin.RADIO_BROWSER)
                .setResolvedStreamUrl(trimNullable(dto.getUrlResolved()))
                .setHomepage(trimNullable(dto.getHomepage()))
                .setFavicon(trimNullable(dto.getFavicon()))
                .setCountryName(trimNullable(dto.getCountry()))
                .setCountryCode(trimCountryCode(dto.getCountryCode()))
                .setLanguage(trimNullable(dto.getLanguage()))
                .setTags(trimNullable(dto.getTags()))
                .setCodec(trimNullable(dto.getCodec()))
                .setBitrate(dto.getBitrate())
                .setHls(mapHls(dto.getHls()))
                .build();
    }

    @Nullable
    private static String trimRequired(@Nullable String value) {
        String trimmedValue = trimNullable(value);
        return trimmedValue == null || trimmedValue.isEmpty() ? null : trimmedValue;
    }

    @Nullable
    private static String trimNullable(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    @NonNull
    private static String safeString(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Nullable
    private static Boolean mapHls(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private interface StationRequest {
        @NonNull
        List<RadioBrowserStationDto> fetch(@NonNull RadioBrowserApi api) throws IOException;
    }
}
