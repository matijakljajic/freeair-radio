package com.matijakljajic.freeairradio.data.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.matijakljajic.freeairradio.data.local.entity.ListeningHistoryStationEntity;

import java.util.List;

@Dao
public interface ListeningHistoryStationDao {

    @NonNull
    @Query("SELECT * FROM listening_history_stations ORDER BY last_played_at DESC")
    List<ListeningHistoryStationEntity> getAll();

    @NonNull
    @Query("SELECT id FROM listening_history_stations")
    List<String> getAllStationIds();

    @Nullable
    @Query("SELECT * FROM listening_history_stations WHERE id = :stationId LIMIT 1")
    ListeningHistoryStationEntity findById(@NonNull String stationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(@NonNull ListeningHistoryStationEntity entity);

    @Query("DELETE FROM listening_history_stations WHERE id = :stationId")
    void deleteById(@NonNull String stationId);

    @Query("DELETE FROM listening_history_stations WHERE id LIKE 'LOCAL:%'")
    void deleteLocalStations();

    @Query("DELETE FROM listening_history_stations WHERE last_played_at < :cutoffTimestamp")
    void deleteOlderThan(long cutoffTimestamp);

    @Query("DELETE FROM listening_history_stations")
    void clearAll();
}
