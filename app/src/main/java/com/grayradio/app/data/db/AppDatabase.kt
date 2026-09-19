package com.grayradio.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.grayradio.app.data.dao.StationDao
import com.grayradio.app.data.entity.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Station::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "grayradio.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed after DB is ready via a separate coroutine
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
