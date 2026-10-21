package com.exodidio.mimi.lyrics

import android.util.LruCache
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Looks for lyrics the device does not have.
 *
 * The one place in the app that reaches the network, and only ever when the listener asks:
 * songs that already carry lyrics in their tags, or have a `.lrc` beside them, never come
 * here.
 *
 * Both databases are asked at once rather than in turn. They are independent, neither is
 * reliably better, and waiting for one before asking the other would double the time a
 * listener spends looking at a spinner for no benefit.
 */
class LyricsFinder(
    private val lrclib: LrclibClient = LrclibClient(),
    private val kugou: KugouClient = KugouClient(),
) {

    private val cache = LruCache<String, Lyrics>(CACHE_ENTRIES)

    /** Everything the two databases offer for this song, in the order they are listed. */
    suspend fun search(question: LyricsQuery): List<LyricsCandidate> = coroutineScope {
        val fromLrclib = async { runCatching { lrclib.search(question) }.getOrDefault(emptyList()) }
        val fromKugou = async { runCatching { kugou.search(question) }.getOrDefault(emptyList()) }

        fromLrclib.await() + fromKugou.await()
    }

    /**
     * The best single answer, for a caller that has no way to offer a choice.
     *
     * What it learns is remembered for the life of the app, including the empty answers,
     * so a song neither database has is not asked about twice.
     */
    suspend fun find(question: LyricsQuery): Lyrics {
        cache.get(question.key)?.let { return it }

        val found = search(question).firstOrNull()
            ?.let { LyricsParser.parse(it.content) }
            ?: Lyrics.EMPTY

        cache.put(question.key, found)
        return found
    }

    /** Forgets everything remembered, including the songs that came back empty. */
    fun forget() = cache.evictAll()

    private companion object {
        const val CACHE_ENTRIES = 32
    }
}
