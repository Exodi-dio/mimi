package com.exodidio.mimi.lyrics

import android.util.LruCache

/**
 * Looks for lyrics the device does not have.
 *
 * The one place in the app that reaches the network, and only ever when a listener asks:
 * songs that already carry lyrics in their tags, or have a `.lrc` beside them, never come
 * here at all.
 *
 * What it learns is remembered for the life of the app, including the empty answers, so a
 * song the database does not have is not asked about every time it is played. The answer
 * is dropped when the activity goes, which is the right lifetime for something that can be
 * fetched again.
 */
class LyricsFinder(private val client: LrclibClient = LrclibClient()) {

    private val cache = LruCache<String, Lyrics>(CACHE_ENTRIES)

    /** The words for this song, or [Lyrics.EMPTY] when there are none to be had. */
    suspend fun find(question: LyricsQuery): Lyrics {
        cache.get(question.key)?.let { return it }

        val lyrics = runCatching { lookup(question) }.getOrDefault(Lyrics.EMPTY)
        cache.put(question.key, lyrics)
        return lyrics
    }

    /** Forgets every answer, so the next question is asked again. */
    fun forget() = cache.evictAll()

    private suspend fun lookup(question: LyricsQuery): Lyrics {
        val answers = client.lookup(question).ifEmpty { client.search(question) }
        if (answers.isEmpty()) return Lyrics.EMPTY

        // Words that follow the music are worth more than words that do not, so a timed
        // answer is preferred whenever one of these songs has it.
        val timed = answers.firstNotNullOfOrNull { it["syncedLyrics"]?.takeIf(String::isNotBlank) }
        val plain = answers.firstNotNullOfOrNull { it["plainLyrics"]?.takeIf(String::isNotBlank) }
        val raw = timed ?: plain ?: return Lyrics.EMPTY

        return LyricsParser.parse(raw).takeIf { !it.isEmpty } ?: Lyrics.EMPTY
    }

    private companion object {
        const val CACHE_ENTRIES = 32
    }
}
