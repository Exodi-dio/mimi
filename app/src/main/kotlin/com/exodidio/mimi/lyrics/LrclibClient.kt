package com.exodidio.mimi.lyrics

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asks lrclib.net for lyrics.
 *
 * A public database: no account, no key, nothing to configure. Two questions are asked of
 * it. `/api/get` is an exact match on artist, title, album and length, and a hit there is
 * the song that was asked about. `/api/search` is looser, for the ordinary case where the
 * tags disagree with the database -- a file reading "1979" against a record filed as
 * "1979 - Remastered" is common enough to be worth a second question.
 *
 * Only two GET requests, so [HttpURLConnection] rather than a networking stack: a
 * dependency that arrives with interceptors and a JSON parser would be a poor trade. Every
 * failure comes back empty. A song without lyrics is a small annoyance; an app that stops
 * playing because a request timed out is not.
 */
class LrclibClient {

    /** The exact match, or an empty list when the database does not have this song. */
    suspend fun lookup(question: LyricsQuery): List<Map<String, String>> =
        withContext(Dispatchers.IO) {
            val title = question.title.trim()
            if (title.isEmpty()) return@withContext emptyList()

            val parameters = buildList {
                add("track_name" to title)
                question.artist?.takeIf { it.isNotBlank() }?.let { add("artist_name" to it.trim()) }
                question.album?.takeIf { it.isNotBlank() }?.let { add("album_name" to it.trim()) }
                // The signature includes the length, and the database's copy of it is only
                // as good as the tags it was submitted from, so it is offered but not
                // insisted on.
                if (question.durationMs > 0) {
                    add("duration" to (question.durationMs / 1_000).toString())
                }
            }

            read(BASE + "/api/get?" + asQueryString(parameters))
        }

    /** A looser question, for when the exact one found nothing. */
    suspend fun search(question: LyricsQuery): List<Map<String, String>> =
        withContext(Dispatchers.IO) {
            val title = question.title.trim()
            if (title.isEmpty()) return@withContext emptyList()

            val parameters = buildList {
                add("track_name" to title)
                question.artist?.takeIf { it.isNotBlank() }?.let { add("artist_name" to it.trim()) }
            }

            read(BASE + "/api/search?" + asQueryString(parameters))
        }

    /** The parsed answer, or nothing at all when the request did not land. */
    private fun read(address: String): List<Map<String, String>> {
        val body = request(address) ?: return emptyList()
        return runCatching { LyricsJson.objects(body) }.getOrDefault(emptyList())
    }

    private fun asQueryString(parameters: List<Pair<String, String>>): String =
        parameters.joinToString("&") { (name, value) ->
            name + "=" + URLEncoder.encode(value, "UTF-8")
        }

    private fun request(address: String): String? {
        var opened: HttpURLConnection? = null
        return try {
            val connection = (URL(address).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            opened = connection

            // Not an error: the database saying it does not have this song is an answer.
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                connection.inputStream.bufferedReader().use { it.readText() }
            }
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
