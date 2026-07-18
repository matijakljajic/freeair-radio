package com.matijakljajic.freeairradio.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_stations")
public class LocalStationEntity {

    @PrimaryKey
    @NonNull
    public final String id;
    @NonNull
    public final String name;
    @ColumnInfo(name = "stream_url")
    @NonNull
    public final String streamUrl;
    @Nullable
    @ColumnInfo(name = "resolved_stream_url")
    public final String resolvedStreamUrl;
    @Nullable
    public final String homepage;
    @Nullable
    public final String favicon;
    @NonNull
    public final String country;
    @NonNull
    @ColumnInfo(name = "country_code")
    public final String countryCode;
    @NonNull
    public final String language;
    @NonNull
    public final String tags;
    @NonNull
    public final String codec;
    public final int bitrate;
    @Nullable
    public final Boolean hls;
    @NonNull
    public final String origin;
    @ColumnInfo(name = "created_at")
    public final long createdAt;
    @ColumnInfo(name = "updated_at")
    public final long updatedAt;

    public LocalStationEntity(@NonNull String id,
                              @NonNull String name,
                              @NonNull String streamUrl,
                              @Nullable String resolvedStreamUrl,
                              @Nullable String homepage,
                              @Nullable String favicon,
                              @NonNull String country,
                              @NonNull String countryCode,
                              @NonNull String language,
                              @NonNull String tags,
                              @NonNull String codec,
                              int bitrate,
                              @Nullable Boolean hls,
                              @NonNull String origin,
                              long createdAt,
                              long updatedAt) {
        this.id = id;
        this.name = name;
        this.streamUrl = streamUrl;
        this.resolvedStreamUrl = resolvedStreamUrl;
        this.homepage = homepage;
        this.favicon = favicon;
        this.country = country;
        this.countryCode = countryCode;
        this.language = language;
        this.tags = tags;
        this.codec = codec;
        this.bitrate = bitrate;
        this.hls = hls;
        this.origin = origin;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
