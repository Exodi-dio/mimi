package com.exodidio.mimi.library

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the device's audio collection out of MediaStore.
 *
 * This is the only place that knows about MediaStore; everything else works with
 * [Track]. The query is deliberately narrow (music only, real duration) because
 * a wide MediaStore query is what makes library scans feel slow.
 */
object MediaStoreScanner {

    private const val UNKNOWN = "<unknown>"

    private val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.MediaColumns.DURATION,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.Audio.Media.TRACK,
    )

    private const val selection =
        "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.MediaColumns.DURATION} > 0"

    suspend fun scan(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val tracks = ArrayList<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        context.contentResolver
            .query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.MediaColumns.TITLE} COLLATE NOCASE ASC",
            )
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                val modifiedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    tracks += Track(
                        id = id,
                        title = cursor.stringOrNull(titleColumn) ?: "Unknown title",
                        artist = cursor.stringOrNull(artistColumn),
                        album = cursor.stringOrNull(albumColumn),
                        albumId = if (albumIdColumn >= 0) cursor.getLong(albumIdColumn) else 0L,
                        durationMs = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L,
                        contentUri = ContentUris.withAppendedId(collection, id).toString(),
                        dateModified = if (modifiedColumn >= 0) cursor.getLong(modifiedColumn) else 0L,
                        sizeBytes = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L,
                        mimeType = cursor.stringOrNull(mimeColumn),
                        trackNumber = if (trackColumn >= 0) cursor.getInt(trackColumn) else 0,
                    )
                }
            }

        tracks
    }

    private fun android.database.Cursor.stringOrNull(index: Int): String? {
        if (index < 0) return null
        val value = getString(index) ?: return null
        val trimmed = value.trim()
        return trimmed.ifEmpty { null }?.takeUnless { it.equals(UNKNOWN, ignoreCase = true) }
    }
}
