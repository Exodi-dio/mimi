package com.exodidio.mimi.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One playable audio file, as cached from MediaStore.
 *
 * [id] is the MediaStore `_ID`, which is stable for a given file and is used as
 * the primary key so repeated scans simply refresh rows in place.
 *
 * Every field added after the first release is nullable. That is not laziness: a column
 * added by `ALTER TABLE` arrives with no default, and Room compares what the migration
 * produced against what the entity promises. Flooring a hundred rows to zero and then
 * claiming the column has a default is how a migration turns into a crash on a phone the
 * developer never held. Nullable columns cannot disagree, and "no year" is honestly
 * different from "year zero" anyway.
 */
@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String?,
    /** Who the record is by, which on a compilation is not who the track is by. */
    @ColumnInfo(name = "album_artist") val albumArtist: String? = null,
    val album: String?,
    @ColumnInfo(name = "album_id") val albumId: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    /** Seconds since the epoch, as MediaStore reports it. Null until a scan has seen it. */
    @ColumnInfo(name = "date_added") val dateAdded: Long? = null,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String?,
    @ColumnInfo(name = "track_number") val trackNumber: Int,
    val year: Int? = null,
    val genre: String? = null,
    val composer: String? = null,
    /** The file's own name, including its extension. */
    @ColumnInfo(name = "file_name") val fileName: String? = null,
    /** The relative folder the file sits in, e.g. `Music/Albums/Blue`. */
    val folder: String? = null,
)
