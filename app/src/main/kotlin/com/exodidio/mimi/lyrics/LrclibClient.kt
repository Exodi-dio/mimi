package com.exodidio.mimi.lyrics

/**
 * Asks lrclib.net about a song.
 *
 * A public database with no account and no key, which is why it is asked first: a hit here
 * is instant and costs the listener nothing to arrange.
 *
 * Two questions are asked of it. The first is an exact match on artist, title, album and
 * length. When that misses, a looser search by title and artist — because a tag reading
 * "1979" against a record filed as "1979 - Remastered" is the ordinary case, and a second
 * of silence at the start of a file is enough to make the lengths disagree.
 */
class LrclibClient {

    /** Every song it believes could be this one, the most exact first. */
    suspend fun search(question: LyricsQuery): List<LyricsCandidate> {
        if (question.title.isBlank()) return emptyList()

        val exact = answer(ENDPOINT_LOOKUP, lookupParameters(question))
        if (exact.isNotEmpty()) return exact

        return answer(ENDPOINT_SEARCH, searchParameters(question))
    }

    private suspend fun answer(
        endpoint: String,
        parameters: List<Pair<String, String>>,
    ): List<LyricsCandidate> {
        val address = BASE + endpoint + "?" + queryString(parameters)
        val body = fetchText(address) ?: return emptyList()
        return runCatching { LyricsJson.objects(body) }
            .getOrDefault(emptyList())
            .mapNotNull(::toCandidate)
    }

    private fun lookupParameters(question: LyricsQuery): List<Pair<String, String>> = buildList {
        add("track_name" to question.title)
        question.artist?.takeIf { it.isNotBlank() }?.let { add("artist_name" to it) }
        question.album?.takeIf { it.isNotBlank() }?.let { add("album_name" to it) }
        if (question.durationMs > 0) add("duration" to (question.durationMs / 1_000).toString())
    }

    private fun searchParameters(question: LyricsQuery): List<Pair<String, String>> = buildList {
        add("track_name" to question.title)
        question.artist?.takeIf { it.isNotBlank() }?.let { add("artist_name" to it) }
    }

    private fun toCandidate(entry: Map<String, String>): LyricsCandidate? {
        val timed = entry["syncedLyrics"]?.takeIf { it.isNotBlank() }
        val plain = entry["plainLyrics"]?.takeIf { it.isNotBlank() }
        val content = timed ?: plain ?: return null

        return LyricsCandidate(
            provider = if (timed != null) PROVIDER_TIMED else PROVIDER_PLAIN,
            trackName = entry["trackName"].orEmpty(),
            artistName = entry["artistName"].orEmpty(),
            // This database counts in seconds; the rest of the app counts in milliseconds.
            durationMs = ((entry["duration"]?.toDoubleOrNull() ?: 0.0) * 1_000.0).toLong(),
            content = content,
        )
    }

    private companion object {
        const val BASE = "https://lrclib.net"
        const val ENDPOINT_LOOKUP = "/api/get"
        const val ENDPOINT_SEARCH = "/api/search"
        const val PROVIDER_TIMED = "lrclib-synced"
        const val PROVIDER_PLAIN = "lrclib-plain"
    }
}
