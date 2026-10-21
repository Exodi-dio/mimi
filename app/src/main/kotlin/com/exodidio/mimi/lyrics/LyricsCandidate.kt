package com.exodidio.mimi.lyrics

/**
 * One set of words offered for a song, before anybody has chosen it.
 *
 * [content] is kept exactly as the provider sent it — timed LRC or plain lines — because
 * [LyricsParser] already knows how to tell them apart and a second opinion here would only
 * be a second thing to keep in step.
 */
data class LyricsCandidate(
    val provider: String,
    val trackName: String,
    val artistName: String,
    val durationMs: Long,
    val content: String,
)
