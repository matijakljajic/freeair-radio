package com.matijakljajic.freeairradio.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_stations")
public class LocalStationEntity {

    @PrimaryKey
    @NonNull
    public final String id;
    @Embedded
    @NonNull
    public final StationSnapshotFields station;
    @ColumnInfo(name = "created_at")
    public final long createdAt;
    @ColumnInfo(name = "updated_at")
    public final long updatedAt;

    public LocalStationEntity(@NonNull String id,
                              @NonNull StationSnapshotFields station,
                              long createdAt,
                              long updatedAt) {
        this.id = id;
        this.station = station;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
