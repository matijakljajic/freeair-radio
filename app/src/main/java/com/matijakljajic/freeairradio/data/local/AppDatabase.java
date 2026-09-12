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
import com.matijakljajic.freeairradio.data.local.dao.ListeningHistoryTrackDao;
import com.matijakljajic.freeairradio.data.local.dao.ListeningHistoryStationDao;
import com.matijakljajic.freeairradio.data.local.entity.FavoriteStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.LocalStationEntity;
import com.matijakljajic.freeairradio.data.local.entity.ListeningHistoryTrackEntity;
import com.matijakljajic.freeairradio.data.local.entity.ListeningHistoryStationEntity;

@Database(
        entities = {
                FavoriteStationEntity.class,
                LocalStationEntity.class,
                ListeningHistoryStationEntity.class,
                ListeningHistoryTrackEntity.class
        },
        version = 4,
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
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `recently_listened_songs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `station_id` TEXT NOT NULL, `artist` TEXT, `title` TEXT, `heard_at` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_recently_listened_songs_station_id_heard_at` ON `recently_listened_songs` (`station_id`, `heard_at`)");
        }
    };
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE recently_played_stations RENAME TO listening_history_stations"
            );
            database.execSQL(
                    "ALTER TABLE recently_listened_songs RENAME TO listening_history_tracks"
            );
            database.execSQL(
                    "DROP INDEX IF EXISTS index_recently_listened_songs_station_id_heard_at"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_history_tracks_station_id_heard_at "
                            + "ON listening_history_tracks (station_id, heard_at)"
            );
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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
    public abstract ListeningHistoryStationDao listeningHistoryStationDao();

    @NonNull
    public abstract ListeningHistoryTrackDao listeningHistoryTrackDao();
}
