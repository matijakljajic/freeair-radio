package com.matijakljajic.freeairradio.data.remote.radiobrowser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection.RadioBrowserServerSelector;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.dto.RadioBrowserStationDto;
import com.matijakljajic.freeairradio.data.repository.StationRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class RadioBrowserRepository implements StationRepository {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_SERVER_ATTEMPTS = 3;
    private static final String RADIO_BROWSER_ID_PREFIX = "RADIO_BROWSER:";
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
        this(RadioBrowserClient.getInstance(), new RadioBrowserServerSelector(context), new Handler(Looper.getMainLooper()));
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
        enqueueStationsAsync("load top stations", callback, api -> api.loadTopStations(DEFAULT_LIMIT, true));
    }

    @Override
    public void searchStationsByName(@NonNull String query, @NonNull LoadCallback callback) {
        enqueueStationsAsync("search stations", callback, api -> api.searchStationsByName(query.trim(), DEFAULT_LIMIT, true));
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

        enqueueClickAsync(stationUuid);
    }

    private void enqueueStationsAsync(@NonNull String action,
                                      @NonNull LoadCallback callback,
                                      @NonNull RequestFactory requestFactory) {
        startWorker(() -> enqueueStations(action, callback, requestFactory, 0));
    }

    private void enqueueStations(@NonNull String action,
                                 @NonNull LoadCallback callback,
                                 @NonNull RequestFactory requestFactory,
                                 int attempt) {
        String baseUrl = getSelectedBaseUrl(callback);
        if (baseUrl == null) {
            return;
        }

        RadioBrowserApi api = client.create(baseUrl);
        requestFactory.create(api).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<RadioBrowserStationDto>> call,
                                   @NonNull Response<List<RadioBrowserStationDto>> response) {
                if (!response.isSuccessful()) {
                    IOException error = new IOException("Failed to " + action + ": " + response.code());
                    if (shouldRetryWithAnotherServer(attempt)) {
                        retryStationsAsync(action, callback, requestFactory, attempt, baseUrl, error);
                        return;
                    }
                    postError(callback, error);
                    return;
                }
                serverSelector.rememberSuccess(baseUrl);
                postStationsLoaded(callback, mapStations(response.body()));
            }

            @Override
            public void onFailure(@NonNull Call<List<RadioBrowserStationDto>> call, @NonNull Throwable t) {
                if (shouldRetryWithAnotherServer(attempt)) {
                    retryStationsAsync(action, callback, requestFactory, attempt, baseUrl, t);
                    return;
                }
                postError(callback, t);
            }
        });
    }

    private boolean shouldRetryWithAnotherServer(int attempt) {
        return attempt + 1 < MAX_SERVER_ATTEMPTS;
    }

    private boolean selectAlternativeServer(@Nullable String failedBaseUrl) {
        return serverSelector.rotateToNextServer(failedBaseUrl)
                || serverSelector.refreshAndRotate(failedBaseUrl);
    }

    private void retryStationsAsync(@NonNull String action,
                                    @NonNull LoadCallback callback,
                                    @NonNull RequestFactory requestFactory,
                                    int attempt,
                                    @Nullable String failedBaseUrl,
                                    @NonNull Throwable error) {
        startWorker(() -> {
            if (selectAlternativeServer(failedBaseUrl)) {
                enqueueStations(action, callback, requestFactory, attempt + 1);
                return;
            }
            postError(callback, error);
        });
    }

    private void enqueueClickAsync(@NonNull String stationUuid) {
        startWorker(() -> enqueueClick(stationUuid));
    }

    private void startWorker(@NonNull Runnable work) {
        WORKER_EXECUTOR.execute(work);
    }

    private void enqueueClick(@NonNull String stationUuid) {
        String baseUrl = getSelectedBaseUrl(null);
        if (baseUrl == null) {
            return;
        }

        RadioBrowserApi api = client.create(baseUrl);
        api.reportStationUsage(stationUuid).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    serverSelector.rememberSuccess(baseUrl);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Fire-and-forget tracking. Selection must not be blocked by analytics.
            }
        });
    }

    private interface RequestFactory {
        @NonNull
        Call<List<RadioBrowserStationDto>> create(@NonNull RadioBrowserApi api);
    }

    @Nullable
    private String getSelectedBaseUrl(@Nullable StationRepository.LoadCallback callback) {
        String baseUrl = serverSelector.getSelectedBaseUrl();
        if (baseUrl == null && callback != null) {
            postError(callback, new IOException("No Radio Browser servers available"));
        }
        return baseUrl;
    }

    private void postStationsLoaded(@NonNull StationRepository.LoadCallback callback,
                                    @NonNull List<Station> stations) {
        postToMain(() -> callback.onStationsLoaded(stations));
    }

    private void postError(@NonNull StationRepository.LoadCallback callback,
                           @NonNull Throwable throwable) {
        postToMain(() -> callback.onError(throwable));
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
                .setCountry(trimNullable(dto.getCountry()))
                .setCountryCode(trimNullable(dto.getCountryCode()))
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
}
