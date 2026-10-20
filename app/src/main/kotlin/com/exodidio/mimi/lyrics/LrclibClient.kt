package com.exodidio.mimi.lyrics

import com.exodidio.mimi.library.Track
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asks lrclib.net for lyrics.
 *
 * A public database, no account and no key, which is the whole reason it is the one service
 * this app talks to. Its API is deliberately small: `/api/get` is an exact match on artist,
 * title, album and length, and `/api/search` is a looser query for when the exact match
 * misses — a song whose tags disagree with the database by a second of length, for
 * instance, which is most of them.
 *
 * Built on [HttpURLConnection] rather than a client library because this is two GET
 * requests. Pulling a networking stack into an app that had none, to make two calls, would
 * be a poor trade in both size and dependencies.
 *
 * Every failure is a null or an empty list. A song without lyrics is a small annoyance; an
 * app that stops playing because a request timed out is not.
 */
class LrclibClient {

    /**
     * The exact match: same artist, same title, same album, same length.
     *
     * Returns everything the answer contained, so the caller can prefer a timed version
     * over an untimed one.
     */
    suspend fun lookup(track: Track): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val title = track.title.trim()
        if (title.isEmpty()) return@withContext emptyList()

        val parameters = buildList {
            add("track_name" to title)
            track.artist?.takeIf { it.isNotBlank() }?.let { add("artist_name" to it.trim()) }
            track.album?.takeIf { it.isNotBlank() }?.let { add("album_name" to it.trim()) }
            // The signature includes the length, and the database's copy of it is only as
            // good as the tags it was submitted from, so it is offered but not insisted on.
            if (track.durationMs > 0) add("duration" to (track.durationMs / 1_000).toString())
        }

        runCatching {
            LyricsJson.objects(request(BASE + "/api/get?" + query(parameters)))
        }.getOrDefault(emptyList())
    }

    /** A looser query, for when the exact match found nothing. */
    suspend fun search(track: Track): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val title = track.title.trim()
        if (title.isEmpty()) return@withContext emptyList()

        val parameters = buildList {
            add("track_name" to title)
            track.artist?.takeIf { it.isNotBlank() }?.let { add("artist_name" to it.trim()) }
        }

        runCatching {
            LyricsJson.objects(request(BASE + "/api/search?" + query(parameters)))
        }.getOrDefault(emptyList())
    }

    private fun query(parameters: List<Pair<String, String>>): String =
        parameters.joinToString("&") { (name, value) ->
            name + "=" + URLEncoder.encode(value, "UTF-8")
        }

    private fun request(address: String): String? {
        var opened: HttpURLConnection? = null
        return try {
            val connection = URL(address).openConnection() as HttpURLConnection
            opened = connection
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            // Somebody should be able to see who is asking. It names the app, not the
            // listener, and is the only thing sent that they did not choose to send.
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")

            // 404 is the database saying it does not have this song, which is an answer.
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { opened?.disconnect() }
        }
    }

    private companion object {
        const val BASE = "https://lrclib.net"
        const val TIMEOUT_MS = 8_000
        const val USER_AGENT = "MIMI/1.0 (Android; local music player)"
    }
}
