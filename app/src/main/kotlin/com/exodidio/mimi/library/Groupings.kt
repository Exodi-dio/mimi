package com.exodidio.mimi.library

import androidx.room.ColumnInfo

/**
 * One album, summarised from the tracks that belong to it.
 *
 * There is no `albums` table. Albums and artists are *views* over the track cache, so
 * a rescan can throw the whole track list away and rebuild it without ever leaving a
 * stale album row behind.
 */
data class AlbumSummary(
    @ColumnInfo(name = "album_id") val albumId: Long,
    val album: String,
    val artist: String?,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    /** A track in this album, used as the source of the cover art. */
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
)

/** One artist, summarised from the tracks credited to them. */
data class ArtistSummary(
    val artist: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "album_count") val albumCount: Int,
)
