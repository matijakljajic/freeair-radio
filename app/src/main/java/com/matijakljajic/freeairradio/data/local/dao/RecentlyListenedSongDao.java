package com.matijakljajic.freeairradio.data.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.matijakljajic.freeairradio.data.local.entity.RecentlyListenedSongEntity;

import java.util.List;

@Dao
public interface RecentlyListenedSongDao {

    @NonNull
    @Query("SELECT * FROM recently_listened_songs ORDER BY heard_at DESC")
    List<RecentlyListenedSongEntity> getAll();

    @Nullable
    @Query("SELECT * FROM recently_listened_songs WHERE station_id = :stationId ORDER BY heard_at DESC LIMIT 1")
    RecentlyListenedSongEntity findLatestByStationId(@NonNull String stationId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(@NonNull RecentlyListenedSongEntity entity);

    @Query("DELETE FROM recently_listened_songs WHERE station_id = :stationId")
    void deleteByStationId(@NonNull String stationId);

    @Query("DELETE FROM recently_listened_songs WHERE station_id NOT IN (:stationIds)")
    void deleteByStationIdNotIn(@NonNull List<String> stationIds);

    @Query("DELETE FROM recently_listened_songs WHERE heard_at < :cutoffTimestamp")
    void deleteOlderThan(long cutoffTimestamp);

    @Query("DELETE FROM recently_listened_songs WHERE station_id = :stationId AND id NOT IN (SELECT id FROM recently_listened_songs WHERE station_id = :stationId ORDER BY heard_at DESC LIMIT :keepCount)")
    void trimToLatest(@NonNull String stationId, int keepCount);

    @Query("DELETE FROM recently_listened_songs")
    void clearAll();
}
