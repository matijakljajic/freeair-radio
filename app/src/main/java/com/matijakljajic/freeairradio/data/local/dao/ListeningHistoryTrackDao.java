package com.matijakljajic.freeairradio.data.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.matijakljajic.freeairradio.data.local.entity.ListeningHistoryTrackEntity;

import java.util.List;

@Dao
public interface ListeningHistoryTrackDao {

    @NonNull
    @Query("SELECT * FROM listening_history_tracks ORDER BY heard_at DESC")
    List<ListeningHistoryTrackEntity> getAll();

    @Nullable
    @Query("SELECT * FROM listening_history_tracks WHERE station_id = :stationId ORDER BY heard_at DESC LIMIT 1")
    ListeningHistoryTrackEntity findLatestByStationId(@NonNull String stationId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(@NonNull ListeningHistoryTrackEntity entity);

    @Query("DELETE FROM listening_history_tracks WHERE station_id = :stationId")
    void deleteByStationId(@NonNull String stationId);

    @Query("DELETE FROM listening_history_tracks WHERE station_id LIKE 'LOCAL:%'")
    void deleteLocalStations();

    @Query("DELETE FROM listening_history_tracks WHERE station_id NOT IN (:stationIds)")
    void deleteByStationIdNotIn(@NonNull List<String> stationIds);

    @Query("DELETE FROM listening_history_tracks WHERE heard_at < :cutoffTimestamp")
    void deleteOlderThan(long cutoffTimestamp);

    @Query("DELETE FROM listening_history_tracks WHERE station_id = :stationId AND id NOT IN (SELECT id FROM listening_history_tracks WHERE station_id = :stationId ORDER BY heard_at DESC LIMIT :keepCount)")
    void trimToLatest(@NonNull String stationId, int keepCount);

    @Query("DELETE FROM listening_history_tracks")
    void clearAll();
}
