package com.matijakljajic.freeairradio.data.repository;

import androidx.annotation.NonNull;

import com.matijakljajic.freeairradio.data.model.Station;

import java.util.List;

public interface StationRepository {

    void reportStationUsage(@NonNull Station station);

    interface CountryCodesCallback {
        void onCountryCodesLoaded(@NonNull java.util.List<String> countryCodes);

        void onError(@NonNull Throwable throwable);
    }

    interface LoadCallback {
        void onStationsLoaded(@NonNull List<Station> stations);

        void onError(@NonNull Throwable throwable);
    }

    void loadTopStations(@NonNull LoadCallback callback);

    void loadTopStationsByCountryCodes(@NonNull List<String> countryCodes,
                                       @NonNull LoadCallback callback);

    void loadAvailableCountryCodes(@NonNull CountryCodesCallback callback);

    void searchStationsByName(@NonNull String query, @NonNull LoadCallback callback);
}
