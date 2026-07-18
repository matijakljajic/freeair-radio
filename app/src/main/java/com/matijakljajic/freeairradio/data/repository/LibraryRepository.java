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

    public interface WriteCallback {
        void onSuccess();

        void onError(@NonNull Throwable throwable);
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
                List<Station> favorites = refreshFavoriteStationsFromDatabase(true);
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
        boolean changed = applyOptimisticFavoriteChange(station, favorite);
        if (changed) {
            notifyFavoritesListeners();
        }

        ioExecutor.execute(() -> {
            try {
                persistFavoriteState(station, favorite);
                refreshFavoriteStationsFromDatabase(true);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not update favorite station state", throwable);
                refreshFavoriteStationsAsync();
            }
        });
    }

    public void saveLocalStation(@NonNull Station station, @Nullable WriteCallback callback) {
        ioExecutor.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                persistLocalStation(station, now);
                updateFavoriteSnapshotForLocalStation(station, now);
                refreshFavoriteStationsFromDatabase(true);
                postWriteSuccess(callback);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not save local station", throwable);
                postWriteError(callback, throwable);
            }
        });
    }

    public void deleteLocalStation(@NonNull String stationId, @Nullable WriteCallback callback) {
        ioExecutor.execute(() -> {
            try {
                deleteLocalStationFromTables(stationId);
                refreshFavoriteStationsFromDatabase(true);
                postWriteSuccess(callback);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not delete local station", throwable);
                postWriteError(callback, throwable);
            }
        });
    }

    public void clearFavoriteStations(@Nullable WriteCallback callback) {
        ioExecutor.execute(() -> {
            try {
                favoriteStationDao.clearAll();
                boolean favoritesChanged = applyFavoriteStations(new ArrayList<>());
                if (favoritesChanged) {
                    notifyFavoritesListeners();
                }
                postWriteSuccess(callback);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not clear favorite stations", throwable);
                postWriteError(callback, throwable);
            }
        });
    }

    public void reorderFavoriteStations(@NonNull List<Station> orderedStations,
                                        @Nullable WriteCallback callback) {
        List<Station> reorderedFavorites = new ArrayList<>(orderedStations);
        ioExecutor.execute(() -> {
            try {
                persistFavoriteOrder(reorderedFavorites);
                refreshFavoriteStationsFromDatabase(true);
                postWriteSuccess(callback);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not reorder favorite stations", throwable);
                refreshFavoriteStationsAsync();
                postWriteError(callback, throwable);
            }
        });
    }

    public void clearLocalStations(@Nullable WriteCallback callback) {
        ioExecutor.execute(() -> {
            try {
                localStationDao.clearAll();
                postWriteSuccess(callback);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not clear local stations", throwable);
                postWriteError(callback, throwable);
            }
        });
    }

    public void clearRecentlyPlayedStations(@Nullable WriteCallback callback) {
        ioExecutor.execute(() -> {
            try {
                recentlyPlayedDao.clearAll();
                postWriteSuccess(callback);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not clear recently played stations", throwable);
                postWriteError(callback, throwable);
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
                refreshFavoriteStationsFromDatabase(true);
            } catch (Throwable throwable) {
                Log.w(TAG, "Could not refresh favorite stations", throwable);
            }
        });
    }

    @NonNull
    private List<Station> refreshFavoriteStationsFromDatabase(boolean notifyListeners) {
        List<Station> favoriteStations = loadFavoriteStationsFromDatabase();
        boolean changed = applyFavoriteStations(favoriteStations);
        if (notifyListeners && changed) {
            notifyFavoritesListeners();
        }
        return favoriteStations;
    }

    @NonNull
    private List<Station> loadFavoriteStationsFromDatabase() {
        return StationMapper.toFavoriteStations(favoriteStationDao.getAll());
    }

    private void persistFavoriteState(@NonNull Station station, boolean favorite) {
        if (favorite) {
            upsertFavoriteStation(station);
            return;
        }
        favoriteStationDao.deleteById(station.getId());
    }

    private void upsertFavoriteStation(@NonNull Station station) {
        long now = System.currentTimeMillis();
        FavoriteStationEntity existingEntity = favoriteStationDao.findById(station.getId());
        long displayOrder = existingEntity != null
                ? existingEntity.displayOrder
                : favoriteStationDao.getNextDisplayOrder();
        favoriteStationDao.upsert(StationMapper.toFavoriteStationEntity(
                station,
                existingEntity,
                displayOrder,
                now
        ));
    }

    private void persistLocalStation(@NonNull Station station, long now) {
        LocalStationEntity existingEntity = localStationDao.findById(station.getId());
        localStationDao.upsert(StationMapper.toLocalStationEntity(station, existingEntity, now));
    }

    private void updateFavoriteSnapshotForLocalStation(@NonNull Station station, long now) {
        FavoriteStationEntity favoriteEntity = favoriteStationDao.findById(station.getId());
        if (favoriteEntity == null) {
            return;
        }

        favoriteStationDao.upsert(StationMapper.toFavoriteStationEntity(
                station,
                favoriteEntity,
                favoriteEntity.displayOrder,
                now
        ));
    }

    private void deleteLocalStationFromTables(@NonNull String stationId) {
        localStationDao.deleteById(stationId);
        favoriteStationDao.deleteById(stationId);
        recentlyPlayedDao.deleteById(stationId);
    }

    private void persistFavoriteOrder(@NonNull List<Station> orderedStations) {
        long updatedAt = System.currentTimeMillis();
        for (int index = 0; index < orderedStations.size(); index++) {
            favoriteStationDao.updateOrder(
                    orderedStations.get(index).getId(),
                    index,
                    updatedAt
            );
        }
    }

    private boolean applyFavoriteStations(@NonNull List<Station> favoriteStations) {
        favoritesLoaded = true;
        return replaceFavoriteCache(favoriteStations);
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

    private boolean applyOptimisticFavoriteChange(@NonNull Station station, boolean favorite) {
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

    private void postWriteSuccess(@Nullable WriteCallback callback) {
        if (callback == null) {
            return;
        }
        postToMain(callback::onSuccess);
    }

    private void postWriteError(@Nullable WriteCallback callback, @NonNull Throwable throwable) {
        if (callback == null) {
            return;
        }
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
