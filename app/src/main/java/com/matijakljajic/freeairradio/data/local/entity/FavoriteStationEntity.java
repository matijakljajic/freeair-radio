package com.matijakljajic.freeairradio.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_stations")
public class FavoriteStationEntity {

    @PrimaryKey
    @NonNull
    public final String id;
    @Embedded
    @NonNull
    public final StationSnapshotFields station;
    @ColumnInfo(name = "display_order")
    public final long displayOrder;
    @ColumnInfo(name = "added_at")
    public final long addedAt;
    @ColumnInfo(name = "updated_at")
    public final long updatedAt;

    public FavoriteStationEntity(@NonNull String id,
                                 @NonNull StationSnapshotFields station,
                                 long displayOrder,
                                 long addedAt,
                                 long updatedAt) {
        this.id = id;
        this.station = station;
        this.displayOrder = displayOrder;
        this.addedAt = addedAt;
        this.updatedAt = updatedAt;
    }
}
