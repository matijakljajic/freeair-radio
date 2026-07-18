package com.matijakljajic.freeairradio.data.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.matijakljajic.freeairradio.data.local.entity.LocalStationEntity;

import java.util.List;

@Dao
public interface LocalStationDao {

    @NonNull
    @Query("SELECT * FROM local_stations ORDER BY updated_at DESC")
    List<LocalStationEntity> getAll();

    @Nullable
    @Query("SELECT * FROM local_stations WHERE id = :stationId LIMIT 1")
    LocalStationEntity findById(@NonNull String stationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(@NonNull LocalStationEntity entity);

    @Query("DELETE FROM local_stations WHERE id = :stationId")
    void deleteById(@NonNull String stationId);
}
