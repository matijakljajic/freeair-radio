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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class RadioBrowserRepository implements StationRepository {

    private static final int DEFAULT_LIMIT = 50;
    private static final long SERVER_DISCOVERY_TIMEOUT_MILLIS = 7000L;
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
        startWorker("RadioBrowserStationLoad", () -> enqueueStations(action, callback, requestFactory));
    }

    private void enqueueStations(@NonNull String action,
                                 @NonNull LoadCallback callback,
                                 @NonNull RequestFactory requestFactory) {
        enqueueStations(action, callback, requestFactory, 0);
    }

    private void enqueueStations(@NonNull String action,
                                 @NonNull LoadCallback callback,
                                 @NonNull RequestFactory requestFactory,
                                 int attempt) {
        if (!serverSelector.awaitReady(SERVER_DISCOVERY_TIMEOUT_MILLIS)) {
            postError(callback, new IOException("Timed out waiting for Radio Browser servers"));
            return;
        }

        String baseUrl = serverSelector.getSelectedBaseUrl();
        if (baseUrl == null) {
            postError(callback, new IOException("No Radio Browser servers available"));
            return;
        }
        RadioBrowserApi api = client.create(baseUrl);
        Call<List<RadioBrowserStationDto>> call = requestFactory.create(api);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<RadioBrowserStationDto>> call,
                                   @NonNull Response<List<RadioBrowserStationDto>> response) {
                if (!response.isSuccessful()) {
                    if (attempt == 0 && serverSelector.rotateToNextServer(baseUrl)) {
                        enqueueStations(action, callback, requestFactory, attempt + 1);
                        return;
                    }
                    postError(callback, new IOException("Failed to " + action + ": " + response.code()));
                    return;
                }
                serverSelector.rememberSuccess(baseUrl);
                postStationsLoaded(callback, mapStations(response.body()));
            }

            @Override
            public void onFailure(@NonNull Call<List<RadioBrowserStationDto>> call, @NonNull Throwable t) {
                if (attempt == 0 && serverSelector.rotateToNextServer(baseUrl)) {
                    enqueueStations(action, callback, requestFactory, attempt + 1);
                    return;
                }
                postError(callback, t);
            }
        });
    }

    private void enqueueClickAsync(@NonNull String stationUuid) {
        startWorker("RadioBrowserStationClick", () -> enqueueClick(stationUuid));
    }

    private void startWorker(@NonNull String name, @NonNull Runnable work) {
        new Thread(work, name).start();
    }

    private void enqueueClick(@NonNull String stationUuid) {
        if (!serverSelector.awaitReady(SERVER_DISCOVERY_TIMEOUT_MILLIS)) {
            return;
        }

        String baseUrl = serverSelector.getSelectedBaseUrl();
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
        String prefix = "RADIO_BROWSER:";
        if (!stationId.startsWith(prefix)) {
            return null;
        }

        String stationUuid = stationId.substring(prefix.length()).trim();
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
