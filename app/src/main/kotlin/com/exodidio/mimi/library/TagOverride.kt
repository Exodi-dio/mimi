package com.exodidio.mimi.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A correction the app holds for a song whose file could not be written to.
 *
 * Writing tags into a file needs the platform's permission, and the platform sometimes
 * says no. A song edited anyway must still show what the listener typed, so the app
 * keeps the corrected tags beside the library and shows those instead.
 *
 * The row holds the whole set of tags rather than only the fields that changed, because
 * a field the listener emptied has to stay empty: a row that only remembered the fields
 * that were filled in could not say that a song has no year any more.
 *
 * The table is its own rather than a column of `tracks`, because that table is wiped and
 * rebuilt by every scan and a correction is something the listener made.
 */
@Entity(tableName = "tag_overrides")
data class TagOverride(
    @PrimaryKey @ColumnInfo(name = "track_id") val trackId: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    @ColumnInfo(name = "album_artist") val albumArtist: String?,
    val year: Int?,
    @ColumnInfo(name = "track_number") val trackNumber: Int?,
    val genre: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * The song as the app shows it, with the correction applied.
 *
 * The correction replaces the tags outright rather than filling in the gaps, so a song
 * whose year was cleared shows no year rather than the year the file still carries.
 */
fun Track.withOverride(override: TagOverride?): Track {
    if (override == null) return this
    return copy(
        title = override.title,
        artist = override.artist,
        album = override.album,
        albumArtist = override.albumArtist,
        year = override.year,
        // A track with no number is the zeroth one, which is how the rest of the app
        // already reads a track it has no number for.
        trackNumber = override.trackNumber ?: 0,
        genre = override.genre,
    )
}

/** The correction that holds [values] for the song [trackId]. */
fun tagOverrideFor(trackId: Long, values: TagValues, at: Long): TagOverride = TagOverride(
    trackId = trackId,
    title = values.title,
    artist = values.artist,
    album = values.album,
    albumArtist = values.albumArtist,
    year = values.year,
    trackNumber = values.trackNumber,
    genre = values.genre,
    updatedAt = at,
)
