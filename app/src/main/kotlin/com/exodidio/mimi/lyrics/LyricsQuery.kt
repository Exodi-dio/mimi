package com.exodidio.mimi.lyrics

/**
 * Everything a lyrics database is asked about.
 *
 * Its own type rather than a library track, because the player knows what is playing as a
 * queue item and has no track to hand, and because a question about a song should be
 * answerable without opening the library. [key] is what an answer is remembered under, so
 * the same song asked twice is asked once.
 */
data class LyricsQuery(
    val key: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L,
)
