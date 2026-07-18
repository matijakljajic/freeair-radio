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
    @Query("SELECT * FROM favorite_stations ORDER BY display_order ASC, added_at ASC")
    List<FavoriteStationEntity> getAll();

    @Nullable
    @Query("SELECT * FROM favorite_stations WHERE id = :stationId LIMIT 1")
    FavoriteStationEntity findById(@NonNull String stationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(@NonNull FavoriteStationEntity entity);

    @Query("DELETE FROM favorite_stations WHERE id = :stationId")
    void deleteById(@NonNull String stationId);

    @Query("SELECT COALESCE(MAX(display_order), -1) + 1 FROM favorite_stations")
    long getNextDisplayOrder();

    @Query("UPDATE favorite_stations SET display_order = :displayOrder, updated_at = :updatedAt WHERE id = :stationId")
    void updateOrder(@NonNull String stationId, long displayOrder, long updatedAt);

    @Query("DELETE FROM favorite_stations")
    void clearAll();
}
