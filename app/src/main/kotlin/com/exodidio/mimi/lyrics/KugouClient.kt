package com.exodidio.mimi.lyrics

import android.util.Base64
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Asks Kugou for lyrics.
 *
 * The second database, and the reason it is worth having is that it is not a database in
 * the way lrclib is: it holds the lyrics that accompany its own catalogue, which is where
 * a great deal of popular music in Chinese, and the romanisation work in this app is for
 * exactly that music.
 *
 * Two steps, because that is how it is built: a search that answers with candidates and a
 * per-candidate download that answers with the words, encoded. Everything here is best
 * effort — if either step fails the song simply has no answer from this source.
 */
class KugouClient {

    /** Every candidate it offers, with the words already fetched. */
    suspend fun search(question: LyricsQuery): List<LyricsCandidate> {
        if (question.title.isBlank()) return emptyList()

        val artist = question.artist?.trim().orEmpty()
        val keyword = if (artist.isEmpty()) question.title else "$artist - ${question.title}"
        val body = fetchText(
            ENDPOINT_SEARCH + "?" + queryString(
                listOf(
                    "ver" to "1",
                    "man" to "yes",
                    "client" to "mobi",
                    "keyword" to keyword,
                    "duration" to question.durationMs.coerceAtLeast(0).toString(),
                    "hash" to "",
                    "album_audio_id" to "",
                ),
            ),
        ) ?: return emptyList()

        val candidates = runCatching { arrayFieldIn(body, "candidates") }
            .getOrDefault(emptyList())
            .take(MAX_CANDIDATES)

        if (candidates.isEmpty()) return emptyList()

        // Each candidate is a separate download and none of them depends on another, so
        // they are fetched together rather than one after the other.
        return coroutineScope {
            candidates
                .map { candidate -> async { download(question, candidate) } }
                .awaitAll()
                .filterNotNull()
        }
    }

    private suspend fun download(
        question: LyricsQuery,
        candidate: Map<String, String>,
    ): LyricsCandidate? {
        val id = candidate["id"]?.takeIf { it.isNotBlank() } ?: return null
        val accessKey = candidate["accesskey"]?.takeIf { it.isNotBlank() } ?: return null

        val body = fetchText(
            ENDPOINT_DOWNLOAD + "?" + queryString(
                listOf(
                    "ver" to "1",
                    "client" to "pc",
                    "id" to id,
                    "accesskey" to accessKey,
                    "fmt" to "lrc",
                    "charset" to "utf8",
                ),
            ),
        ) ?: return null

        val encoded = runCatching { LyricsJson.objects(body).firstOrNull()?.get("content") }
            .getOrNull()
            ?: return null

        val words = decodeEntities(decode(encoded) ?: return null)
        if (words.isBlank()) return null

        return LyricsCandidate(
            provider = if (SYNCED.containsMatchIn(words)) PROVIDER_TIMED else PROVIDER_PLAIN,
            trackName = question.title,
            artistName = question.artist.orEmpty(),
            durationMs = candidate["duration"]?.toLongOrNull() ?: question.durationMs,
            content = words,
        )
    }

    /** The words arrive base64 encoded, and sometimes with line breaks folded into them. */
    private fun decode(encoded: String): String? = runCatching {
        String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
    }.getOrNull()

    /**
     * Some of the words come back with entities in them rather than characters.
     *
     * Left alone they would be shown as typed, so `&amp;` would read as `&amp;`. Anything
     * not recognised is left exactly as it was, which is the only safe answer for text
     * that might legitimately contain an ampersand.
     */
    private fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text

        return ENTITY.replace(text) { match ->
            when (val name = match.groupValues[1]) {
                "amp" -> "&"
                "apos" -> "'"
                "lt" -> "<"
                "gt" -> ">"
                "quot" -> "\""
                "nbsp" -> " "
                else -> numericCharacter(name) ?: match.value
            }
        }
    }

    private fun numericCharacter(name: String): String? {
        if (!name.startsWith("#")) return null

        val digits = name.drop(1)
        val code = if (digits.startsWith("x") || digits.startsWith("X")) {
            digits.drop(1).toIntOrNull(16)
        } else {
            digits.toIntOrNull()
        } ?: return null

        return code.takeIf { it in 1..0x10FFFF }?.let { Character.toChars(it).concatToString() }
    }

    private companion object {
        const val ENDPOINT_SEARCH = "http://krcs.kugou.com/search"
        const val ENDPOINT_DOWNLOAD = "https://lyrics.kugou.com/download"
        const val MAX_CANDIDATES = 5
        const val PROVIDER_TIMED = "kugou-synced"
        const val PROVIDER_PLAIN = "kugou-plain"

        val SYNCED = Regex("(?m)^\\[\\d{2}:\\d{2}\\.\\d+]")
        val ENTITY = Regex("&(#(?:x[0-9a-fA-F]+|\\d+)|amp|apos|lt|gt|quot|nbsp);")
    }
}
