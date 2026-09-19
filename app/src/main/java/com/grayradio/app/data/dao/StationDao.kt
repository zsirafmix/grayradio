package com.grayradio.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.grayradio.app.data.entity.Station
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<Station>>

    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<Station>>

    @Query("SELECT COUNT(*) FROM stations")
    suspend fun count(): Int

    @Query("SELECT * FROM stations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Station?

    @Query("SELECT * FROM stations WHERE streamUrl = :url LIMIT 1")
    suspend fun findByUrl(url: String): Station?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(station: Station): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stations: List<Station>)

    @Update
    suspend fun update(station: Station)

    @Delete
    suspend fun delete(station: Station)

    @Query("DELETE FROM stations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE stations SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)
}
