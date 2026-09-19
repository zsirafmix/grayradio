package com.grayradio.app.data.repository

import com.grayradio.app.data.dao.StationDao
import com.grayradio.app.data.db.SeedData
import com.grayradio.app.data.entity.Station
import com.grayradio.app.data.remote.RadioBrowserApi
import com.grayradio.app.data.remote.RemoteCountry
import com.grayradio.app.data.remote.RemoteStation
import kotlinx.coroutines.flow.Flow

class StationRepository(
    private val dao: StationDao,
    private val api: RadioBrowserApi = RadioBrowserApi(),
) {
    fun observeStations(): Flow<List<Station>> = dao.observeAll()

    fun observeFavorites(): Flow<List<Station>> = dao.observeFavorites()

    suspend fun ensureSeeded() {
        if (dao.count() == 0) {
            dao.insertAll(SeedData.stations)
        }
    }

    suspend fun addManual(
        name: String,
        streamUrl: String,
        logoUrl: String? = null,
        countryCode: String? = null,
        tags: String? = null,
        isFavorite: Boolean = false,
    ): Result<Station> {
        val trimmedName = name.trim()
        val trimmedUrl = streamUrl.trim()
        if (trimmedName.isEmpty()) return Result.failure(IllegalArgumentException("name"))
        if (trimmedUrl.isEmpty()) return Result.failure(IllegalArgumentException("url"))
        if (!(trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://"))) {
            return Result.failure(IllegalArgumentException("invalid_url"))
        }
        dao.findByUrl(trimmedUrl)?.let { existing ->
            if (isFavorite && !existing.isFavorite) {
                val updated = existing.copy(isFavorite = true)
                dao.update(updated)
                return Result.success(updated)
            }
            return Result.failure(IllegalStateException("already_added"))
        }
        val station = Station(
            name = trimmedName,
            streamUrl = trimmedUrl,
            logoUrl = logoUrl,
            countryCode = countryCode,
            tags = tags,
            isFavorite = isFavorite,
        )
        val id = dao.insert(station)
        return Result.success(station.copy(id = id))
    }

    suspend fun addRemote(remote: RemoteStation, favorite: Boolean = false): Result<Station> =
        addManual(
            name = remote.name,
            streamUrl = remote.streamUrl,
            logoUrl = remote.logoUrl,
            countryCode = remote.countryCode,
            tags = remote.tags,
            isFavorite = favorite,
        )

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

    suspend fun toggleFavorite(station: Station): Station {
        val updated = station.copy(isFavorite = !station.isFavorite)
        dao.update(updated)
        return updated
    }

    suspend fun setFavoriteByUrl(remote: RemoteStation, favorite: Boolean): Result<Station> {
        val existing = dao.findByUrl(remote.streamUrl)
        return if (existing != null) {
            val updated = existing.copy(isFavorite = favorite)
            dao.update(updated)
            Result.success(updated)
        } else if (favorite) {
            addRemote(remote, favorite = true)
        } else {
            Result.failure(IllegalStateException("not_saved"))
        }
    }

    suspend fun findByUrl(url: String): Station? = dao.findByUrl(url)

    suspend fun searchByName(query: String): List<RemoteStation> = api.searchByName(query)

    suspend fun searchHungarian(): List<RemoteStation> = api.searchHungarian()

    suspend fun browseStations(countryCode: String, tag: String?): List<RemoteStation> =
        api.searchByCountry(countryCode, tag)

    suspend fun fetchCountries(): List<RemoteCountry> = api.fetchCountries()
}
