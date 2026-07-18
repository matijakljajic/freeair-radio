package com.matijakljajic.freeairradio.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.data.local.AppDatabase;
import com.matijakljajic.freeairradio.data.local.StationMapper;
import com.matijakljajic.freeairradio.data.local.dao.FavoriteStationDao;
import com.matijakljajic.freeairradio.data.local.dao.LocalStationDao;
import com.matijakljajic.freeairradio.data.local.dao.RecentlyPlayedDao;
import com.matijakljajic.freeairradio.data.local.entity.FavoriteStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.LocalStationEntity;
import com.matijakljajic.freeairradio.data.model.Station;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LibraryRepository {

    public interface FavoritesListener {
        void onFavoritesChanged();
    }

    private static final String TAG = "LibraryRepository";
    private static final long RECENTLY_PLAYED_RETENTION_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    @Nullable
    private static volatile LibraryRepository instance;

    @NonNull
    public static LibraryRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (LibraryRepository.class) {
                if (instance == null) {
                    instance = new LibraryRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @NonNull
    private final FavoriteStationDao favoriteStationDao;
    @NonNull
    private final LocalStationDao localStationDao;
    @NonNull
    private final RecentlyPlayedDao recentlyPlayedDao;
    @NonNull
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    @NonNull
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @NonNull
    private final Map<String, Station> favoriteStationsCache = new LinkedHashMap<>();
    @NonNull
    private final CopyOnWriteArraySet<FavoritesListener> favoritesListeners = new CopyOnWriteArraySet<>();
    private volatile boolean favoritesLoaded;

    private LibraryRepository(@NonNull Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        favoriteStationDao = database.favoriteStationDao();
        localStationDao = database.localStationDao();
        recentlyPlayedDao = database.recentlyPlayedDao();
        refreshFavoriteStationsAsync();
        cleanupExpiredRecentlyPlayedAsync();
    }

    public boolean isFavorite(@NonNull Station station) {
        synchronized (favoriteStationsCache) {
            return favoriteStationsCache.containsKey(station.getId());
        }
    }

    public boolean hasLoadedFavorites() {
        return favoritesLoaded;
    }

    @NonNull
    public List<Station> getFavoriteStationsSnapshot() {
        synchronized (favoriteStationsCache) {
            return new ArrayList<>(favoriteStationsCache.values());
        }
    }

    public void loadFavoriteStations(@NonNull StationRepository.LoadCallback callback) {
        ioExecutor.execute(() -> {
            try {
                List<Station> favorites = loadFavoriteStationsFromDatabase();
                boolean changed = replaceFavoriteCache(favorites);
                favoritesLoaded = true;
                if (changed) {
                    notifyFavoritesListeners();
                }
                postStationsLoaded(callback, favorites);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not load favorite stations", throwable);
                postError(callback, throwable);
            }
        });
    }

    public void loadLocalStations(@NonNull StationRepository.LoadCallback callback) {
        ioExecutor.execute(() -> {
            try {
                List<Station> stations = StationMapper.toLocalStations(localStationDao.getAll());
                postStationsLoaded(callback, stations);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not load local stations", throwable);
                postError(callback, throwable);
            }
        });
    }

    public void loadRecentlyPlayedStations(@NonNull StationRepository.LoadCallback callback) {
        ioExecutor.execute(() -> {
            try {
                cleanupExpiredRecentlyPlayed();
                List<Station> stations = StationMapper.toRecentlyPlayedStations(recentlyPlayedDao.getAll());
                postStationsLoaded(callback, stations);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not load recently played stations", throwable);
                postError(callback, throwable);
            }
        });
    }

    public void setFavorite(@NonNull Station station, boolean favorite) {
        boolean changed = updateFavoriteCache(station, favorite);
        if (changed) {
            notifyFavoritesListeners();
        }

        ioExecutor.execute(() -> {
            try {
                if (favorite) {
                    upsertFavoriteStation(station);
                } else {
                    favoriteStationDao.deleteById(station.getId());
                }

                List<Station> refreshedFavorites = loadFavoriteStationsFromDatabase();
                boolean refreshedChanged = replaceFavoriteCache(refreshedFavorites);
                favoritesLoaded = true;
                if (refreshedChanged) {
                    notifyFavoritesListeners();
                }
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not update favorite station state", throwable);
                refreshFavoriteStationsAsync();
            }
        });
    }

    public void saveLocalStation(@NonNull Station station) {
        ioExecutor.execute(() -> {
            try {
                LocalStationEntity existingEntity = localStationDao.findById(station.getId());
                localStationDao.upsert(StationMapper.toLocalStationEntity(
                        station,
                        existingEntity,
                        System.currentTimeMillis()
                ));
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not save local station", throwable);
            }
        });
    }

    public void deleteLocalStation(@NonNull String stationId) {
        ioExecutor.execute(() -> {
            try {
                localStationDao.deleteById(stationId);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not delete local station", throwable);
            }
        });
    }

    public void recordRecentlyPlayed(@NonNull Station station) {
        ioExecutor.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                cleanupExpiredRecentlyPlayed(now);
                recentlyPlayedDao.upsert(StationMapper.toRecentlyPlayedStationEntity(station, now));
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not record recently played station", throwable);
            }
        });
    }

    public void addFavoritesListener(@NonNull FavoritesListener listener) {
        favoritesListeners.add(listener);
        if (favoritesLoaded) {
            postToMain(listener::onFavoritesChanged);
        }
    }

    public void removeFavoritesListener(@NonNull FavoritesListener listener) {
        favoritesListeners.remove(listener);
    }

    private void refreshFavoriteStationsAsync() {
        ioExecutor.execute(() -> {
            try {
                List<Station> favorites = loadFavoriteStationsFromDatabase();
                boolean changed = replaceFavoriteCache(favorites);
                favoritesLoaded = true;
                if (changed) {
                    notifyFavoritesListeners();
                }
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not refresh favorite stations", throwable);
            }
        });
    }

    @NonNull
    private List<Station> loadFavoriteStationsFromDatabase() {
        return StationMapper.toFavoriteStations(favoriteStationDao.getAll());
    }

    private void upsertFavoriteStation(@NonNull Station station) {
        long now = System.currentTimeMillis();
        FavoriteStationEntity existingEntity = favoriteStationDao.findById(station.getId());
        favoriteStationDao.upsert(StationMapper.toFavoriteStationEntity(station, existingEntity, now));
    }

    private boolean replaceFavoriteCache(@NonNull List<Station> favoriteStations) {
        synchronized (favoriteStationsCache) {
            List<Station> currentFavorites = new ArrayList<>(favoriteStationsCache.values());
            if (currentFavorites.equals(favoriteStations)) {
                return false;
            }

            favoriteStationsCache.clear();
            for (Station station : favoriteStations) {
                favoriteStationsCache.put(station.getId(), station);
            }
            return true;
        }
    }

    private boolean updateFavoriteCache(@NonNull Station station, boolean favorite) {
        synchronized (favoriteStationsCache) {
            if (favorite) {
                Station existingStation = favoriteStationsCache.get(station.getId());
                favoriteStationsCache.put(station.getId(), station);
                return existingStation == null || !existingStation.equals(station);
            }

            return favoriteStationsCache.remove(station.getId()) != null;
        }
    }

    private void cleanupExpiredRecentlyPlayedAsync() {
        ioExecutor.execute(() -> {
            try {
                cleanupExpiredRecentlyPlayed();
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not clean up recently played stations", throwable);
            }
        });
    }

    private void cleanupExpiredRecentlyPlayed() {
        cleanupExpiredRecentlyPlayed(System.currentTimeMillis());
    }

    private void cleanupExpiredRecentlyPlayed(long now) {
        recentlyPlayedDao.deleteOlderThan(now - RECENTLY_PLAYED_RETENTION_MILLIS);
    }

    private void notifyFavoritesListeners() {
        for (FavoritesListener listener : favoritesListeners) {
            postToMain(listener::onFavoritesChanged);
        }
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
}
