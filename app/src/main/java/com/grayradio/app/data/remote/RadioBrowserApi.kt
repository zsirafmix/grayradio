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
    val bitrate: Int = 0,
    val tags: String? = null,
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
            parse(httpGet(url))
        }

    suspend fun searchHungarian(limit: Int = 40): List<RemoteStation> =
        withContext(Dispatchers.IO) {
            val url =
                "$BASE/json/stations/search?countrycode=HU&limit=$limit&hidebroken=true&order=clickcount&reverse=true"
            parse(httpGet(url))
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

    private fun parse(json: String?): List<RemoteStation> {
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
                bitrate = o.optInt("bitrate"),
                tags = o.optString("tags").takeIf { it.isNotBlank() },
            )
        }
        return out.distinctBy { it.streamUrl }
    }

    companion object {
        const val BASE = "https://de1.api.radio-browser.info"
        const val USER_AGENT = "GrayRadio/1.0"

        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
    }
}
