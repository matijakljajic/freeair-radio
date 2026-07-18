package com.matijakljajic.freeairradio.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.matijakljajic.freeairradio.data.local.dao.FavoriteStationDao;
import com.matijakljajic.freeairradio.data.local.dao.LocalStationDao;
import com.matijakljajic.freeairradio.data.local.dao.RecentlyPlayedDao;
import com.matijakljajic.freeairradio.data.local.entity.FavoriteStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.LocalStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.RecentlyPlayedStationEntity;

@Database(
        entities = {
                FavoriteStationEntity.class,
                LocalStationEntity.class,
                RecentlyPlayedStationEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "freeairradio.db";

    private static volatile AppDatabase instance;

    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .fallbackToDestructiveMigration(false)
                            .build();
                }
            }
        }
        return instance;
    }

    @NonNull
    public abstract FavoriteStationDao favoriteStationDao();

    @NonNull
    public abstract LocalStationDao localStationDao();

    @NonNull
    public abstract RecentlyPlayedDao recentlyPlayedDao();
}
