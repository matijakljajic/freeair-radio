package com.matijakljajic.freeairradio.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "listening_history_tracks",
        indices = {
                @Index(value = {"station_id", "heard_at"})
        }
)
public class ListeningHistoryTrackEntity {

    @PrimaryKey(autoGenerate = true)
    public final long id;
    @NonNull
    @ColumnInfo(name = "station_id")
    public final String stationId;
    @Nullable
    public final String artist;
    @Nullable
    public final String title;
    @ColumnInfo(name = "heard_at")
    public final long heardAt;

    public ListeningHistoryTrackEntity(long id,
                                      @NonNull String stationId,
                                      @Nullable String artist,
                                      @Nullable String title,
                                      long heardAt) {
        this.id = id;
        this.stationId = stationId;
        this.artist = artist;
        this.title = title;
        this.heardAt = heardAt;
    }
}
