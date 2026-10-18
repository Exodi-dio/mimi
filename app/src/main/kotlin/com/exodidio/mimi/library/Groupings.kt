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

/**
 * One genre, summarised from the tracks filed under it.
 *
 * Like albums and artists, this is a view. The difference is that the value came from
 * MediaStore's genre tables rather than from a column of the audio table, so a song with
 * no genre simply belongs to no row here.
 */
data class GenreSummary(
    val genre: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
)

/** One composer, summarised from the tracks credited to them. */
data class ComposerSummary(
    val composer: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
)

/** One release year, summarised from the tracks that carry it. */
data class YearSummary(
    val year: Int,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
)

/**
 * One folder on the device, summarised from the files inside it.
 *
 * [folder] is the path relative to the storage root, e.g. `Music/Albums/Blue`. The whole
 * path is kept rather than just the last segment because two folders called `Live` in
 * different places are not the same folder, and merging them would be a lie.
 */
data class FolderSummary(
    val folder: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
)
