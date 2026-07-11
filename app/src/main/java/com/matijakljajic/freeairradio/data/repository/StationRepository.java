package com.matijakljajic.freeairradio.data.repository;

import androidx.annotation.NonNull;

import com.matijakljajic.freeairradio.data.model.Station;

import java.util.List;

public interface StationRepository {

    void reportStationUsage(@NonNull Station station);

    interface LoadCallback {
        void onStationsLoaded(@NonNull List<Station> stations);

        void onError(@NonNull Throwable throwable);
    }

    void loadTopStations(@NonNull LoadCallback callback);

    void searchStationsByName(@NonNull String query, @NonNull LoadCallback callback);
}
