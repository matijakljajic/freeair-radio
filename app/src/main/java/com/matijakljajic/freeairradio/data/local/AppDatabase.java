package com.matijakljajic.freeairradio.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "freeairradio.db";
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE favorite_stations ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE favorite_stations SET display_order = added_at");
        }
    };

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
                            .addMigrations(MIGRATION_1_2)
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
