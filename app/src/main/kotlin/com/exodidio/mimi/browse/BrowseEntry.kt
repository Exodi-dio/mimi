package com.exodidio.mimi.browse

/**
 * One thing in the browse tree: a shelf to open, or a song to play.
 *
 * A browser is shown a flat list at every level, so a shelf and a song have to be the
 * same shape. What tells them apart is whether the entry opens into more entries or
 * makes a sound.
 */
data class BrowseEntry(
    /** The id the tree is asked about: a shelf's own name, or a song's media uri. */
    val mediaId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val artworkUri: String?,
    /** True when the entry opens into more entries rather than making a sound. */
    val browsable: Boolean,
    /** True when the entry is a song the player can start. */
    val playable: Boolean,
    /** How long the song is, in milliseconds. Zero for anything that is not a song. */
    val durationMs: Long = 0L,
)
