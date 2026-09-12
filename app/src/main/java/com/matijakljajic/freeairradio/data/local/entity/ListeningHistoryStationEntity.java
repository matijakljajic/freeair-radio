package com.matijakljajic.freeairradio.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "listening_history_stations")
public class ListeningHistoryStationEntity {

    @PrimaryKey
    @NonNull
    public final String id;
    @Embedded
    @NonNull
    public final StationSnapshotFields station;
    @ColumnInfo(name = "last_played_at")
    public final long lastPlayedAt;

    public ListeningHistoryStationEntity(@NonNull String id,
                                       @NonNull StationSnapshotFields station,
                                       long lastPlayedAt) {
        this.id = id;
        this.station = station;
        this.lastPlayedAt = lastPlayedAt;
    }
}
