package com.matijakljajic.freeairradio.data.local.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.matijakljajic.freeairradio.data.local.entity.RecentlyPlayedStationEntity;

import java.util.List;

@Dao
public interface RecentlyPlayedDao {

    @NonNull
    @Query("SELECT * FROM recently_played_stations ORDER BY last_played_at DESC")
    List<RecentlyPlayedStationEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(@NonNull RecentlyPlayedStationEntity entity);

    @Query("DELETE FROM recently_played_stations WHERE last_played_at < :cutoffTimestamp")
    void deleteOlderThan(long cutoffTimestamp);
}
