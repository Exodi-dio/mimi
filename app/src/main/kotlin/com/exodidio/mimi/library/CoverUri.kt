package com.exodidio.mimi.library

/**
 * Where the device keeps the cover it found for an album.
 *
 * The album art index is keyed by the album's own id, which is what the library stores,
 * so a row knows where its cover is without opening the file. A song with no album has
 * no cover to look for, and zero is not an album id the index knows.
 */
fun albumArtUriFor(albumId: Long): String? =
    if (albumId == 0L) null else "$ALBUM_ART_BASE/$albumId"

/** The path the device indexes album art under. */
private const val ALBUM_ART_BASE = "content://media/external/audio/albumart"
