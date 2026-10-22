package com.exodidio.mimi.browse

/**
 * One song as the browse tree sees it.
 *
 * Not the library's own row: a car and another app are shown a name, a cover and a
 * length, and none of the file's own details, so the shape the tree is given is its own
 * rather than the shape the cache happens to have.
 */
data class BrowseSong(
    /** The media store uri, which is also how the player knows the song. */
    val mediaId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    /** The album's own id, which is what the device's cover index is keyed by. */
    val albumId: Long,
    /** Where the song sits on its record, so an album can be listed in running order. */
    val trackNumber: Int,
    /** How long the song is, in milliseconds. */
    val durationMs: Long,
    /** The cover the device indexed for the album, or null when it indexed none. */
    val artworkUri: String?,
)
