package com.exodidio.mimi.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One playable audio file, as cached from MediaStore.
 *
 * [id] is the MediaStore `_ID`, which is stable for a given file and is used as
 * the primary key so repeated scans simply refresh rows in place.
 */
@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    @ColumnInfo(name = "album_id") val albumId: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String?,
    @ColumnInfo(name = "track_number") val trackNumber: Int,
)
