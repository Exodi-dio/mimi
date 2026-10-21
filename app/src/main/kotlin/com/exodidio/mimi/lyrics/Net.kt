package com.exodidio.mimi.lyrics

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One GET request, as text, or nothing at all.
 *
 * Both lyrics databases are asked the same way: a URL with a query string, a short wait,
 * and an answer. Neither needs a key, a session or a retry, and the only thing either can
 * do with a failure is show no words — so this answers with null rather than throwing, and
 * the callers never have to think about it.
 *
 * The wait is deliberately short. A lyrics lookup is a small favour to the listener, and a
 * request hanging in the background for a minute is worse than admitting it found nothing.
 */
internal suspend fun fetchText(
    address: String,
    timeoutMs: Int = DEFAULT_HTTP_TIMEOUT_MS,
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", HTTP_USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            // A 404 from a lyrics database is an answer — "I do not have this song" — and
            // is treated the same as an empty list rather than as a fault.
            if (connection.responseCode !in 200..299) {
                null
            } else {
                connection.inputStream.bufferedReader().use { it.readText() }
            }
        } finally {
            runCatching { connection.disconnect() }
        }
    }.getOrNull()
}

/** Parameters as a URL query string, with both sides escaped. */
internal fun queryString(parameters: List<Pair<String, String>>): String =
    parameters.joinToString("&") { (key, value) ->
        URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
    }

internal const val DEFAULT_HTTP_TIMEOUT_MS = 30_000

internal const val HTTP_USER_AGENT = "MIMI/1.0 (Android; local music player)"
