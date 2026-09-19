package com.grayradio.app.data.repository

import com.grayradio.app.data.dao.StationDao
import com.grayradio.app.data.db.SeedData
import com.grayradio.app.data.entity.Station
import com.grayradio.app.data.remote.RadioBrowserApi
import com.grayradio.app.data.remote.RemoteStation
import kotlinx.coroutines.flow.Flow

class StationRepository(
    private val dao: StationDao,
    private val api: RadioBrowserApi = RadioBrowserApi(),
) {
    fun observeStations(): Flow<List<Station>> = dao.observeAll()

    suspend fun ensureSeeded() {
        if (dao.count() == 0) {
            dao.insertAll(SeedData.stations)
        }
    }

    suspend fun addManual(name: String, streamUrl: String, logoUrl: String? = null): Result<Station> {
        val trimmedName = name.trim()
        val trimmedUrl = streamUrl.trim()
        if (trimmedName.isEmpty()) return Result.failure(IllegalArgumentException("name"))
        if (trimmedUrl.isEmpty()) return Result.failure(IllegalArgumentException("url"))
        if (!(trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://"))) {
            return Result.failure(IllegalArgumentException("invalid_url"))
        }
        dao.findByUrl(trimmedUrl)?.let {
            return Result.failure(IllegalStateException("already_added"))
        }
        val station = Station(name = trimmedName, streamUrl = trimmedUrl, logoUrl = logoUrl)
        val id = dao.insert(station)
        return Result.success(station.copy(id = id))
    }

    suspend fun addRemote(remote: RemoteStation): Result<Station> =
        addManual(remote.name, remote.streamUrl, remote.logoUrl)

    suspend fun update(station: Station): Result<Unit> {
        val trimmedName = station.name.trim()
        val trimmedUrl = station.streamUrl.trim()
        if (trimmedName.isEmpty()) return Result.failure(IllegalArgumentException("name"))
        if (!(trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://"))) {
            return Result.failure(IllegalArgumentException("invalid_url"))
        }
        dao.update(station.copy(name = trimmedName, streamUrl = trimmedUrl))
        return Result.success(Unit)
    }

    suspend fun delete(station: Station) {
        dao.delete(station)
    }

    suspend fun searchByName(query: String): List<RemoteStation> = api.searchByName(query)

    suspend fun searchHungarian(): List<RemoteStation> = api.searchHungarian()
}
