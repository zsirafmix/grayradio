package com.grayradio.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

data class RemoteStation(
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val bitrate: Int = 0,
    val tags: String? = null,
)

data class RemoteCountry(
    val name: String,
    val code: String,
    val stationCount: Int,
)

data class GenreOption(
    val tag: String,
    val labelHu: String,
)

/**
 * Radio Browser API client (no API key).
 * Primary mirror: https://de1.api.radio-browser.info
 */
class RadioBrowserApi(
    private val client: OkHttpClient = defaultClient(),
) {
    suspend fun searchByName(name: String, limit: Int = 30): List<RemoteStation> =
        withContext(Dispatchers.IO) {
            val q = java.net.URLEncoder.encode(name.trim(), Charsets.UTF_8.name())
            val url =
                "$BASE/json/stations/search?name=$q&limit=$limit&hidebroken=true&order=clickcount&reverse=true"
            parseStations(httpGet(url))
        }

    suspend fun searchHungarian(limit: Int = 40): List<RemoteStation> =
        searchByCountry("HU", tag = null, limit = limit)

    suspend fun searchByCountry(
        countryCode: String,
        tag: String? = null,
        limit: Int = 60,
    ): List<RemoteStation> = withContext(Dispatchers.IO) {
        val code = countryCode.trim().uppercase()
        val tagPart = tag?.trim()?.takeIf { it.isNotEmpty() }?.let {
            "&tag=${java.net.URLEncoder.encode(it, Charsets.UTF_8.name())}"
        }.orEmpty()
        val url =
            "$BASE/json/stations/search?countrycode=$code$tagPart&limit=$limit&hidebroken=true&order=clickcount&reverse=true"
        parseStations(httpGet(url))
    }

    suspend fun searchByTag(tag: String, limit: Int = 60): List<RemoteStation> =
        withContext(Dispatchers.IO) {
            val q = java.net.URLEncoder.encode(tag.trim(), Charsets.UTF_8.name())
            val url =
                "$BASE/json/stations/search?tag=$q&limit=$limit&hidebroken=true&order=clickcount&reverse=true"
            parseStations(httpGet(url))
        }

    suspend fun fetchCountries(minStations: Int = 5): List<RemoteCountry> =
        withContext(Dispatchers.IO) {
            val json = httpGet("$BASE/json/countries") ?: return@withContext emptyList()
            val arr = JSONArray(json)
            val out = ArrayList<RemoteCountry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val code = o.optString("iso_3166_1").uppercase()
                val count = o.optInt("stationcount")
                if (code.length != 2 || count < minStations) continue
                out += RemoteCountry(
                    name = o.optString("name").ifBlank { code },
                    code = code,
                    stationCount = count,
                )
            }
            out.sortedWith(
                compareByDescending<RemoteCountry> { it.code == "HU" }
                    .thenByDescending { it.stationCount }
                    .thenBy { it.name },
            )
        }

    private fun httpGet(url: String): String? {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.string()
            }
        }.getOrNull()
    }

    private fun parseStations(json: String?): List<RemoteStation> {
        if (json.isNullOrBlank()) return emptyList()
        val arr = JSONArray(json)
        val out = ArrayList<RemoteStation>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val stream = o.optString("url_resolved").ifBlank { o.optString("url") }
            if (stream.isBlank() || !(stream.startsWith("http://") || stream.startsWith("https://"))) {
                continue
            }
            out += RemoteStation(
                name = o.optString("name").ifBlank { "Ismeretlen rádió" }.trim(),
                streamUrl = stream,
                logoUrl = o.optString("favicon").takeIf { it.startsWith("http") },
                country = o.optString("country").takeIf { it.isNotBlank() },
                countryCode = o.optString("countrycode").takeIf { it.isNotBlank() }?.uppercase(),
                bitrate = o.optInt("bitrate"),
                tags = o.optString("tags").takeIf { it.isNotBlank() },
            )
        }
        return out.distinctBy { it.streamUrl }
    }

    companion object {
        const val BASE = "https://de1.api.radio-browser.info"
        const val USER_AGENT = "ZsirafGrayRadio/1.1"

        val CURATED_GENRES: List<GenreOption> = listOf(
            GenreOption("rock", "Rock"),
            GenreOption("pop", "Pop"),
            GenreOption("news", "Hírek"),
            GenreOption("dance", "Dance"),
            GenreOption("jazz", "Jazz"),
            GenreOption("classical", "Klasszikus"),
            GenreOption("electronic", "Elektronikus"),
            GenreOption("hiphop", "Hip-hop"),
            GenreOption("metal", "Metal"),
            GenreOption("folk", "Folk"),
            GenreOption("talk", "Beszélgetős"),
            GenreOption("country", "Country"),
            GenreOption("blues", "Blues"),
            GenreOption("reggae", "Reggae"),
        )

        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
    }
}
