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
import com.matijakljajic.freeairradio.data.local.dao.ListeningHistoryTrackDao;
import com.matijakljajic.freeairradio.data.local.dao.ListeningHistoryStationDao;
import com.matijakljajic.freeairradio.data.local.entity.FavoriteStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.LocalStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.ListeningHistoryTrackEntity;
import com.matijakljajic.freeairradio.data.local.entity.ListeningHistoryStationEntity;
import com.matijakljajic.freeairradio.data.model.ListeningHistoryTrack;
import com.matijakljajic.freeairradio.data.model.ListeningHistoryEntry;
import com.matijakljajic.freeairradio.data.model.Station;

import java.util.ArrayList;
import java.util.Collections;
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

    public interface ListeningHistoryListener {
        void onListeningHistoryChanged();
    }

    public interface ListeningHistoryCallback {
        void onListeningHistoryLoaded(@NonNull List<ListeningHistoryEntry> entries);

        void onError(@NonNull Throwable throwable);
    }

    public interface WriteCallback {
        void onSuccess();

        void onError(@NonNull Throwable throwable);
    }

    private static final String TAG = "LibraryRepository";
    private static final long LISTENING_HISTORY_STATION_RETENTION_MILLIS =
            3L * 24L * 60L * 60L * 1000L;
    private static final long LISTENING_HISTORY_TRACK_RETENTION_MILLIS =
            3L * 24L * 60L * 60L * 1000L;
    private static final int MAX_HISTORY_TRACKS_PER_STATION = 12;

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
    private final AppDatabase database;
    @NonNull
    private final FavoriteStationDao favoriteStationDao;
    @NonNull
    private final LocalStationDao localStationDao;
    @NonNull
    private final ListeningHistoryTrackDao listeningHistoryTrackDao;
    @NonNull
    private final ListeningHistoryStationDao listeningHistoryStationDao;
    @NonNull
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    @NonNull
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @NonNull
    private final Map<String, Station> favoriteStationsCache = new LinkedHashMap<>();
    @NonNull
    private final CopyOnWriteArraySet<FavoritesListener> favoritesListeners = new CopyOnWriteArraySet<>();
    @NonNull
    private final Object listeningHistoryLock = new Object();
    @NonNull
    private final List<ListeningHistoryEntry> listeningHistoryCache = new ArrayList<>();
    @NonNull
    private final CopyOnWriteArraySet<ListeningHistoryListener> listeningHistoryListeners =
            new CopyOnWriteArraySet<>();
    private volatile boolean favoritesLoaded;
    private volatile boolean listeningHistoryLoaded;

    private LibraryRepository(@NonNull Context context) {
        database = AppDatabase.getInstance(context);
        favoriteStationDao = database.favoriteStationDao();
        localStationDao = database.localStationDao();
        listeningHistoryTrackDao = database.listeningHistoryTrackDao();
        listeningHistoryStationDao = database.listeningHistoryStationDao();
        refreshFavoriteStationsAsync();
        cleanupExpiredListeningHistoryAsync();
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
            } catch (RuntimeException throwable) {
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
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not load local stations", throwable);
                postError(callback, throwable);
            }
        });
    }

    public void loadListeningHistoryStations(@NonNull StationRepository.LoadCallback callback) {
        ioExecutor.execute(() -> {
            try {
                cleanupExpiredListeningHistory();
                List<Station> stations = StationMapper.toListeningHistoryStations(listeningHistoryStationDao.getAll());
                postStationsLoaded(callback, stations);
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not load listening history stations", throwable);
                postError(callback, throwable);
            }
        });
    }

    public void loadListeningHistoryEntries(@NonNull ListeningHistoryCallback callback) {
        ioExecutor.execute(() -> {
            try {
                List<ListeningHistoryEntry> stations = refreshListeningHistoryFromDatabase(true);
                postListeningHistoryLoaded(callback, stations);
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not load listening history", throwable);
                postListeningHistoryError(callback, throwable);
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
                database.runInTransaction(() -> persistFavoriteState(station, favorite));
                refreshFavoriteStationsFromDatabase(true);
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not update favorite station state", throwable);
                refreshFavoriteStationsAsync();
            }
        });
    }

    public void saveLocalStation(@NonNull Station station, @Nullable WriteCallback callback) {
        ioExecutor.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                database.runInTransaction(() -> {
                    persistLocalStation(station, now);
                    updateFavoriteSnapshotForLocalStation(station, now);
                    updateListeningHistorySnapshotForLocalStation(station);
                });
                refreshFavoriteStationsFromDatabase(true);
                refreshListeningHistoryFromDatabase(true);
                postWriteSuccess(callback);
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not save local station", throwable);
                postWriteError(callback, throwable);
            }
        });
    }

    public void deleteLocalStation(@NonNull String stationId, @Nullable WriteCallback callback) {
        ioExecutor.execute(() -> {
            try {
                database.runInTransaction(() -> deleteLocalStationFromTables(stationId));
                refreshFavoriteStationsFromDatabase(true);
                refreshListeningHistoryFromDatabase(true);
                postWriteSuccess(callback);
            } catch (RuntimeException throwable) {
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
            } catch (RuntimeException throwable) {
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
                database.runInTransaction(() -> persistFavoriteOrder(reorderedFavorites));
                refreshFavoriteStationsFromDatabase(true);
                postWriteSuccess(callback);
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not reorder favorite stations", throwable);
                refreshFavoriteStationsAsync();
                postWriteError(callback, throwable);
            }
        });
    }

    public void clearLocalStations(@Nullable WriteCallback callback) {
        ioExecutor.execute(() -> {
            try {
                database.runInTransaction(() -> {
                    localStationDao.clearAll();
                    favoriteStationDao.deleteLocalStations();
                    listeningHistoryStationDao.deleteLocalStations();
                    listeningHistoryTrackDao.deleteLocalStations();
                });
                refreshFavoriteStationsFromDatabase(true);
                refreshListeningHistoryFromDatabase(true);
                postWriteSuccess(callback);
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not clear local stations", throwable);
                postWriteError(callback, throwable);
            }
        });
    }

    public void clearListeningHistory(@Nullable WriteCallback callback) {
        ioExecutor.execute(() -> {
            try {
                database.runInTransaction(() -> {
                    listeningHistoryStationDao.clearAll();
                    listeningHistoryTrackDao.clearAll();
                });
                boolean historyChanged = clearListeningHistoryCache();
                if (historyChanged) {
                    notifyListeningHistoryListeners();
                }
                postWriteSuccess(callback);
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not clear listening history", throwable);
                postWriteError(callback, throwable);
            }
        });
    }

    public void recordListeningHistoryStation(@NonNull Station station) {
        ioExecutor.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                database.runInTransaction(() -> {
                    cleanupExpiredListeningHistoryTables(now);
                    listeningHistoryStationDao.upsert(StationMapper.toListeningHistoryStationEntity(station, now));
                });
                refreshListeningHistoryFromDatabase(true);
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not record listening history station", throwable);
            }
        });
    }

    public void recordListeningHistoryTrack(@NonNull Station station,
                                           @NonNull ListeningHistoryTrack track) {
        ioExecutor.execute(() -> {
            try {
                boolean inserted = database.runInTransaction(
                        () -> persistListeningHistoryTrack(station, track)
                );
                if (!inserted) {
                    return;
                }
                refreshListeningHistoryFromDatabase(true);
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not record listening history track", throwable);
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

    public boolean hasLoadedListeningHistory() {
        return listeningHistoryLoaded;
    }

    @NonNull
    public List<ListeningHistoryEntry> getListeningHistorySnapshot() {
        synchronized (listeningHistoryLock) {
            return new ArrayList<>(listeningHistoryCache);
        }
    }

    public void addListeningHistoryListener(@NonNull ListeningHistoryListener listener) {
        listeningHistoryListeners.add(listener);
        if (listeningHistoryLoaded) {
            postToMain(listener::onListeningHistoryChanged);
        }
    }

    public void removeListeningHistoryListener(@NonNull ListeningHistoryListener listener) {
        listeningHistoryListeners.remove(listener);
    }

    private void refreshFavoriteStationsAsync() {
        ioExecutor.execute(() -> {
            try {
                refreshFavoriteStationsFromDatabase(true);
            } catch (RuntimeException throwable) {
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

    private void updateListeningHistorySnapshotForLocalStation(@NonNull Station station) {
        ListeningHistoryStationEntity historyEntity = listeningHistoryStationDao.findById(station.getId());
        if (historyEntity == null) {
            return;
        }
        listeningHistoryStationDao.upsert(StationMapper.toListeningHistoryStationEntity(
                station,
                historyEntity.lastPlayedAt
        ));
    }

    private void deleteLocalStationFromTables(@NonNull String stationId) {
        localStationDao.deleteById(stationId);
        favoriteStationDao.deleteById(stationId);
        listeningHistoryStationDao.deleteById(stationId);
        listeningHistoryTrackDao.deleteByStationId(stationId);
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

    private void cleanupExpiredListeningHistoryAsync() {
        ioExecutor.execute(() -> {
            try {
                cleanupExpiredListeningHistory();
            } catch (RuntimeException throwable) {
                Log.w(TAG, "Could not clean up listening history", throwable);
            }
        });
    }

    private void cleanupExpiredListeningHistory() {
        cleanupExpiredListeningHistory(System.currentTimeMillis());
    }

    private void cleanupExpiredListeningHistory(long now) {
        database.runInTransaction(() -> cleanupExpiredListeningHistoryTables(now));
    }

    private void cleanupExpiredListeningHistoryTables(long now) {
        listeningHistoryStationDao.deleteOlderThan(now - LISTENING_HISTORY_STATION_RETENTION_MILLIS);
        listeningHistoryTrackDao.deleteOlderThan(now - LISTENING_HISTORY_TRACK_RETENTION_MILLIS);
        pruneOrphanedHistoryTracks();
    }

    private void notifyFavoritesListeners() {
        for (FavoritesListener listener : favoritesListeners) {
            postToMain(listener::onFavoritesChanged);
        }
    }

    private void notifyListeningHistoryListeners() {
        for (ListeningHistoryListener listener : listeningHistoryListeners) {
            postToMain(listener::onListeningHistoryChanged);
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

    private void postListeningHistoryLoaded(@NonNull ListeningHistoryCallback callback,
                                            @NonNull List<ListeningHistoryEntry> stations) {
        postToMain(() -> callback.onListeningHistoryLoaded(stations));
    }

    private void postListeningHistoryError(@NonNull ListeningHistoryCallback callback,
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

    @NonNull
    private List<ListeningHistoryEntry> refreshListeningHistoryFromDatabase(boolean notifyListeners) {
        cleanupExpiredListeningHistory();
        List<ListeningHistoryEntry> history = buildListeningHistoryEntries(listeningHistoryStationDao.getAll());
        boolean changed = applyListeningHistorySnapshot(history);
        if (notifyListeners && changed) {
            notifyListeningHistoryListeners();
        }
        return history;
    }

    @NonNull
    private List<ListeningHistoryEntry> buildListeningHistoryEntries(
            @NonNull List<ListeningHistoryStationEntity> entities) {
        Map<String, List<ListeningHistoryTrack>> tracksByStationId = loadTracksByStationId();
        List<ListeningHistoryEntry> stations = new ArrayList<>(entities.size());
        for (ListeningHistoryStationEntity entity : entities) {
            stations.add(new ListeningHistoryEntry(
                    StationMapper.toStation(entity),
                    entity.lastPlayedAt,
                    tracksByStationId.getOrDefault(entity.id, Collections.emptyList())
            ));
        }
        return stations;
    }

    private boolean applyListeningHistorySnapshot(@NonNull List<ListeningHistoryEntry> entries) {
        synchronized (listeningHistoryLock) {
            listeningHistoryLoaded = true;
            if (listeningHistoryCache.equals(entries)) {
                return false;
            }

            listeningHistoryCache.clear();
            listeningHistoryCache.addAll(entries);
            return true;
        }
    }

    private boolean clearListeningHistoryCache() {
        synchronized (listeningHistoryLock) {
            boolean changed = !listeningHistoryCache.isEmpty();
            listeningHistoryLoaded = true;
            listeningHistoryCache.clear();
            return changed;
        }
    }

    private boolean persistListeningHistoryTrack(@NonNull Station station,
                                                @NonNull ListeningHistoryTrack track) {
        if (track.buildDisplayText() == null) {
            return false;
        }

        ListeningHistoryTrackEntity latestTrackEntity =
                listeningHistoryTrackDao.findLatestByStationId(station.getId());
        ListeningHistoryTrack latestTrack = latestTrackEntity == null
                ? null
                : StationMapper.toListeningHistoryTrack(latestTrackEntity);
        if (track.hasSameTrackInfo(latestTrack)) {
            return false;
        }

        listeningHistoryTrackDao.insert(StationMapper.toListeningHistoryTrackEntity(station.getId(), track));
        listeningHistoryTrackDao.trimToLatest(station.getId(), MAX_HISTORY_TRACKS_PER_STATION);
        return true;
    }

    @NonNull
    private Map<String, List<ListeningHistoryTrack>> loadTracksByStationId() {
        Map<String, List<ListeningHistoryTrack>> tracksByStationId = new LinkedHashMap<>();
        for (ListeningHistoryTrackEntity entity : listeningHistoryTrackDao.getAll()) {
            List<ListeningHistoryTrack> tracks = tracksByStationId.get(entity.stationId);
            if (tracks == null) {
                tracks = new ArrayList<>();
                tracksByStationId.put(entity.stationId, tracks);
            }
            tracks.add(StationMapper.toListeningHistoryTrack(entity));
        }
        return tracksByStationId;
    }

    private void pruneOrphanedHistoryTracks() {
        List<String> activeStationIds = listeningHistoryStationDao.getAllStationIds();
        if (activeStationIds.isEmpty()) {
            listeningHistoryTrackDao.clearAll();
            return;
        }
        listeningHistoryTrackDao.deleteByStationIdNotIn(activeStationIds);
    }
}
