package com.exodidio.mimi.lyrics

import android.util.LruCache
import com.exodidio.mimi.library.Track

/**
 * Looks for lyrics the device does not have.
 *
 * The one place in the app that reaches the network, and only ever when a listener asks:
 * songs that already carry lyrics in their tags, or have a `.lrc` beside them, never come
 * near this.
 *
 * Two questions are asked in order. The exact match first, because a hit there is the song
 * that was asked for rather than one with a similar name. Then the loose search, because a
 * tag that says "1979" where the database says "1979 - Remastered" is common enough that
 * insisting on an exact match would find almost nothing.
 *
 * A timed version is preferred over an untimed one. Words that follow the music are worth
 * more than words that do not, and the database frequently has both.
 */
class LyricsFinder(private val client: LrclibClient = LrclibClient()) {

    private val cache = LruCache<String, Lyrics>(CACHE_ENTRIES)

    /**
     * The lyrics for [track], or [Lyrics.EMPTY] when the database has none.
     *
     * Cached by song, including the empty answer: a song the database does not have should
     * not be asked about again every time its screen is opened.
     */
    suspend fun find(track: Track): Lyrics {
        val key = track.id.toString()
        cache.get(key)?.let { return it }

        val found = runCatching { lookup(track) }.getOrDefault(Lyrics.EMPTY)
        cache.put(key, found)
        return found
    }

    /** Forgets what was found, so the next ask goes out again. */
    fun forget() = cache.evictAll()

    private suspend fun lookup(track: Track): Lyrics {
        val candidates = client.lookup(track).ifEmpty { client.search(track) }
        if (candidates.isEmpty()) return Lyrics.EMPTY

        val timed = candidates.firstOrNull { !it["syncedLyrics"].isNullOrBlank() }
        val plain = candidates.firstOrNull { !it["plainLyrics"].isNullOrBlank() }
        val raw = timed?.get("syncedLyrics")?.takeIf { it.isNotBlank() }
            ?: plain?.get("plainLyrics")?.takeIf { it.isNotBlank() }
            ?: return Lyrics.EMPTY

        return LyricsParser.parse(raw)
    }

    private companion object {
        const val CACHE_ENTRIES = 32
    }
}
