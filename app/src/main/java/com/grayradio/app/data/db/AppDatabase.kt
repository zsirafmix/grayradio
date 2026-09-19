package com.grayradio.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.grayradio.app.data.dao.StationDao
import com.grayradio.app.data.entity.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Station::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE stations ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE stations ADD COLUMN countryCode TEXT")
                db.execSQL("ALTER TABLE stations ADD COLUMN tags TEXT")
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "grayradio.db")
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = get(context)
                            if (database.stationDao().count() == 0) {
                                database.stationDao().insertAll(SeedData.stations)
                            }
                        }
                    }
                })
                .build()
        }
    }
}
