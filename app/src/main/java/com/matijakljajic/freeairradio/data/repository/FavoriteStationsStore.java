package com.matijakljajic.freeairradio.data.repository;

import androidx.annotation.NonNull;

import com.matijakljajic.freeairradio.data.model.Station;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

public final class FavoriteStationsStore {

    public interface Listener {
        void onFavoritesChanged();
    }

    @NonNull
    private static final FavoriteStationsStore INSTANCE = new FavoriteStationsStore();

    @NonNull
    private final Map<String, Station> favoriteStations = new LinkedHashMap<>();
    @NonNull
    private final CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<>();

    private FavoriteStationsStore() {
    }

    @NonNull
    public static FavoriteStationsStore getInstance() {
        return INSTANCE;
    }

    public boolean isFavorite(@NonNull Station station) {
        synchronized (favoriteStations) {
            return favoriteStations.containsKey(station.getId());
        }
    }

    public void setFavorite(@NonNull Station station, boolean favorite) {
        boolean changed;
        synchronized (favoriteStations) {
            if (favorite) {
                Station existingStation = favoriteStations.get(station.getId());
                favoriteStations.put(station.getId(), station);
                changed = existingStation == null || !existingStation.equals(station);
            } else {
                changed = favoriteStations.remove(station.getId()) != null;
            }
        }

        if (changed) {
            notifyListeners();
        }
    }

    @NonNull
    public List<Station> getFavorites() {
        synchronized (favoriteStations) {
            return new ArrayList<>(favoriteStations.values());
        }
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onFavoritesChanged();
        }
    }
}
