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

    /** The columns that have been in MediaProvider since the beginning. */
    private val baseProjection = arrayOf(
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

    /**
     * What the library would ideally know, on top of the essentials.
     *
     * A projection naming one column the provider does not have fails the *whole* query,
     * not just that column, so these are asked for on top of [baseProjection] and the scan
     * falls back rather than coming back empty. A device that says no simply gets a
     * library without years and folders, which is worth having.
     */
    private val extendedProjection = baseProjection + arrayOf(
        MediaStore.Audio.Media.COMPOSER,
        MediaStore.Audio.Media.YEAR,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.RELATIVE_PATH,
        MediaStore.MediaColumns.DISPLAY_NAME,
    )

    /**
     * Which rows count as music.
     *
     * The lower bound comes from the settings, so it is a bound argument rather than text
     * pasted into the statement: the query keeps one shape and the provider can still
     * prepare it. An upper bound is added only when one is in force, because `DURATION < 0`
     * would otherwise have to be special-cased to mean "no limit".
     */
    private const val selection =
        "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.MediaColumns.DURATION} > ?"

    /**
     * A track as it comes off the cursor, before the side tables have had their say.
     *
     * Kept separate from [Track] because album artists and genres are not columns of the
     * audio table at all — they live in side tables that are read once per scan and then
     * joined in memory, which is far cheaper than one query per song.
     */
    private data class Row(
        val id: Long,
        val title: String,
        val artist: String?,
        val album: String?,
        val albumId: Long,
        val durationMs: Long,
        val dateModified: Long,
        val sizeBytes: Long,
        val mimeType: String?,
        val trackNumber: Int,
        val composer: String?,
        val year: Int?,
        val dateAdded: Long?,
        val folder: String?,
        val fileName: String?,
    )

    /**
     * Reads the device and returns what belongs in the library.
     *
     * Folders the listener has excluded are dropped here rather than later, so an excluded
     * folder costs a comparison per row instead of a whole song built and then thrown away.
     *
     * The corrections the app holds for songs whose files could not be written to are
     * applied here rather than where the lists are drawn, because the cache is the one
     * place every list reads from: a correction applied there is seen by the songs list,
     * by the albums, by the artists and by the search at once.
     */
    suspend fun scan(
        context: Context,
        minDurationMs: Long = 0L,
        maxDurationMs: Long = 0L,
        excludedFolders: Collection<String> = emptyList(),
        overrides: Map<Long, TagOverride> = emptyMap(),
    ): List<Track> =
        withContext(Dispatchers.IO) {
            val found = runCatching {
                queryRows(context, extendedProjection, minDurationMs, maxDurationMs)
            }.getOrElse {
                queryRows(context, baseProjection, minDurationMs, maxDurationMs)
            }
            val rows = if (excludedFolders.isEmpty()) {
                found
            } else {
                found.filterNot { isExcludedFolder(it.folder, excludedFolders) }
            }

        if (rows.isEmpty()) return@withContext emptyList()

        val albumArtists = albumArtists(context)
        val genres = genresByTrack(context)

        rows.map { row ->
            val track = Track(
                id = row.id,
                title = row.title,
                artist = row.artist,
                albumArtist = albumArtists[row.albumId],
                album = row.album,
                albumId = row.albumId,
                durationMs = row.durationMs,
                contentUri = ContentUris
                    .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, row.id)
                    .toString(),
                dateAdded = row.dateAdded,
                dateModified = row.dateModified,
                sizeBytes = row.sizeBytes,
                mimeType = row.mimeType,
                trackNumber = row.trackNumber,
                year = row.year,
                genre = genres[row.id],
                composer = row.composer,
                fileName = row.fileName,
                folder = row.folder,
            )
            track.withOverride(overrides[row.id])
        }
    }

    /**
     * Where the device says each song's file is, keyed by the song's id.
     *
     * A playlist file holds paths rather than the addresses the media provider gives, so
     * writing one out and reading one in both need to know the path of a song. The
     * provider is asked for it in its own query because the column is no longer part of
     * the scan: a device that will not give it leaves the playlists without paths rather
     * than making the whole scan fail.
     */
    @Suppress("DEPRECATION")
    suspend fun pathsById(context: Context): Map<Long, String> = withContext(Dispatchers.IO) {
        runCatching {
            val paths = HashMap<Long, String>()
            context.contentResolver
                .query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA),
                    null,
                    null,
                    null,
                )
                ?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val pathColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (pathColumn >= 0) {
                        while (cursor.moveToNext()) {
                            val path = cursor.getString(pathColumn)
                            if (!path.isNullOrBlank()) paths[cursor.getLong(idColumn)] = path
                        }
                    }
                }
            paths
        }.getOrDefault(emptyMap())
    }

    private fun queryRows(
        context: Context,
        projection: Array<String>,
        minDurationMs: Long,
        maxDurationMs: Long,
    ): List<Row> {
        val bounded = maxDurationMs > 0L
        val query = if (bounded) {
            "$selection AND ${MediaStore.MediaColumns.DURATION} <= ?"
        } else {
            selection
        }
        val arguments = if (bounded) {
            arrayOf(minDurationMs.toString(), maxDurationMs.toString())
        } else {
            arrayOf(minDurationMs.toString())
        }

        val rows = ArrayList<Row>()
        context.contentResolver
            .query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                query,
                arguments,
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
                val composerColumn = cursor.getColumnIndex(MediaStore.Audio.Media.COMPOSER)
                val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val addedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val pathColumn = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val year = if (yearColumn >= 0) cursor.getInt(yearColumn) else 0
                    rows += Row(
                        id = cursor.getLong(idColumn),
                        title = cursor.stringOrNull(titleColumn) ?: "Unknown title",
                        artist = cursor.stringOrNull(artistColumn),
                        album = cursor.stringOrNull(albumColumn),
                        albumId = if (albumIdColumn >= 0) cursor.getLong(albumIdColumn) else 0L,
                        durationMs = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L,
                        dateModified = if (modifiedColumn >= 0) cursor.getLong(modifiedColumn) else 0L,
                        sizeBytes = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L,
                        mimeType = cursor.stringOrNull(mimeColumn),
                        trackNumber = if (trackColumn >= 0) cursor.getInt(trackColumn) else 0,
                        composer = if (composerColumn >= 0) cursor.stringOrNull(composerColumn) else null,
                        year = year.takeIf { it > 0 },
                        dateAdded = if (addedColumn >= 0) cursor.getLong(addedColumn) else null,
                        folder = if (pathColumn >= 0) {
                            cursor.stringOrNull(pathColumn)?.trim('/')?.takeIf { it.isNotEmpty() }
                        } else {
                            null
                        },
                        fileName = if (nameColumn >= 0) cursor.stringOrNull(nameColumn) else null,
                    )
                }
            }
        return rows
    }

    /**
     * Album id to album artist, read from the albums table.
     *
     * The audio table only carries the track artist, which on a compilation is a different
     * person on every row. The albums table is the one place that knows the record's own
     * artist, so it is worth the second query.
     */
    private fun albumArtists(context: Context): Map<Long, String> = runCatching {
        val artists = HashMap<Long, String>()
        context.contentResolver
            .query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ARTIST),
                null,
                null,
                null,
            )
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
                while (cursor.moveToNext()) {
                    val name = cursor.stringOrNull(artistColumn)
                    if (name != null) artists[cursor.getLong(idColumn)] = name
                }
            }
        artists
    }.getOrDefault(emptyMap())

    /**
     * Track id to genre, from the genre side tables.
     *
     * MediaStore's own `genre` column on the audio table has been unreliable for years,
     * so the genre tables are the source that actually has data. This is one query for the
     * genre names and then one narrow query per genre — cheap next to the audio query
     * itself, and only ever done during a scan.
     *
     * A song carrying several genres keeps the first one. Browsing by a joined-up string
     * like "Rock, Pop" would invent a genre that matches nothing else in the library.
     */
    private fun genresByTrack(context: Context): Map<Long, String> = runCatching {
        val genres = HashMap<Long, String>()
        context.contentResolver
            .query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null,
                null,
                null,
            )
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Genres.NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.stringOrNull(nameColumn) ?: continue
                    val genreId = cursor.getLong(idColumn)
                    membersOfGenre(context, genreId).forEach { trackId ->
                        if (trackId !in genres) genres[trackId] = name
                    }
                }
            }
        genres
    }.getOrDefault(emptyMap())

    private fun membersOfGenre(context: Context, genreId: Long): List<Long> = runCatching {
        val ids = ArrayList<Long>()
        context.contentResolver
            .query(
                MediaStore.Audio.Genres.Members.getContentUri(
                    MediaStore.VOLUME_EXTERNAL,
                    genreId,
                ),
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null,
            )
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (cursor.moveToNext()) ids += cursor.getLong(idColumn)
            }
        ids
    }.getOrDefault(emptyList())

    private fun android.database.Cursor.stringOrNull(index: Int): String? {
        if (index < 0) return null
        val value = getString(index) ?: return null
        val trimmed = value.trim()
        return trimmed.ifEmpty { null }?.takeUnless { it.equals(UNKNOWN, ignoreCase = true) }
    }
}
