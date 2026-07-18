package com.matijakljajic.freeairradio.data.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.matijakljajic.freeairradio.data.local.entity.FavoriteStationEntity;

import java.util.List;

@Dao
public interface FavoriteStationDao {

    @NonNull
    @Query("SELECT * FROM favorite_stations ORDER BY added_at ASC")
    List<FavoriteStationEntity> getAll();

    @Nullable
    @Query("SELECT * FROM favorite_stations WHERE id = :stationId LIMIT 1")
    FavoriteStationEntity findById(@NonNull String stationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(@NonNull FavoriteStationEntity entity);

    @Query("DELETE FROM favorite_stations WHERE id = :stationId")
    void deleteById(@NonNull String stationId);

    @Query("DELETE FROM favorite_stations")
    void clearAll();
}
