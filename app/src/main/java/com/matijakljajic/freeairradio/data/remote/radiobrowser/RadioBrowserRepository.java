package com.matijakljajic.freeairradio.data.remote.radiobrowser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
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

public class RadioBrowserRepository implements StationRepository {

    private static final int DEFAULT_LIMIT = 50;
    private final RadioBrowserApi api;

    public RadioBrowserRepository() {
        this(RadioBrowserClient.getInstance().getApi());
    }

    RadioBrowserRepository(@NonNull RadioBrowserApi api) {
        this.api = api;
    }

    @Override
    public void loadTopStations(@NonNull LoadCallback callback) {
        enqueueStations(api.loadTopStations(DEFAULT_LIMIT, true), "load top stations", callback);
    }

    @Override
    public void searchStationsByName(@NonNull String query, @NonNull LoadCallback callback) {
        enqueueStations(api.searchStationsByName(query.trim(), DEFAULT_LIMIT, true), "search stations", callback);
    }

    private void enqueueStations(@NonNull Call<List<RadioBrowserStationDto>> call,
                                 @NonNull String action,
                                 @NonNull LoadCallback callback) {
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<RadioBrowserStationDto>> call,
                                   @NonNull Response<List<RadioBrowserStationDto>> response) {
                if (!response.isSuccessful()) {
                    callback.onError(new IOException("Failed to " + action + ": " + response.code()));
                    return;
                }
                callback.onStationsLoaded(mapStations(response.body()));
            }

            @Override
            public void onFailure(@NonNull Call<List<RadioBrowserStationDto>> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
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

        String stationUuid = dto.getStationUuid();
        String name = dto.getName();
        String streamUrl = dto.getUrl();
        if (stationUuid == null || name == null || streamUrl == null) {
            return null;
        }

        return Station.builder("RADIO_BROWSER:" + stationUuid, name, streamUrl, StationOrigin.RADIO_BROWSER)
                .setResolvedStreamUrl(dto.getUrlResolved())
                .setHomepage(dto.getHomepage())
                .setFavicon(dto.getFavicon())
                .setCountry(dto.getCountry())
                .setCountryCode(dto.getCountryCode())
                .setLanguage(dto.getLanguage())
                .setTags(dto.getTags())
                .setCodec(dto.getCodec())
                .setBitrate(dto.getBitrate())
                .setHls(mapHls(dto.getHls()))
                .build();
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
