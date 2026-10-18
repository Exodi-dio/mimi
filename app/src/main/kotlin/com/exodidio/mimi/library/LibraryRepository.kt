package com.exodidio.mimi.library

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/**
 * The library as the rest of the app sees it.
 *
 * Reads always come from the Room cache, so the list is on screen immediately at
 * launch. [refresh] re-reads MediaStore, and by default skips the work entirely
 * if the cache was already refreshed recently — the point being that opening the
 * app must never mean waiting on a scan.
 */
class LibraryRepository(
    private val context: Context,
    private val database: MimiDatabase,
) {
    private val trackDao = database.trackDao()
    private val metaDao = database.metaDao()

    val tracks: Flow<List<Track>> = trackDao.observeAll()

    /** Albums derived from the cached tracks; no table of its own to keep in sync. */
    val albums: Flow<List<AlbumSummary>> = trackDao.observeAlbums()

    /** Artists derived from the cached tracks; no table of its own to keep in sync. */
    val artists: Flow<List<ArtistSummary>> = trackDao.observeArtists()

    fun tracksForAlbum(albumId: Long): Flow<List<Track>> = trackDao.observeAlbumTracks(albumId)

    fun tracksForArtist(artist: String): Flow<List<Track>> = trackDao.observeArtistTracks(artist)

    fun albumsForArtist(artist: String): Flow<List<AlbumSummary>> = trackDao.observeArtistAlbums(artist)

    /** Titles, artists and albums containing [query]. Blank matches nothing, not everything. */
    fun search(query: String): Flow<List<Track>> = trackDao.search(query)

    suspend fun lastScanAt(): Long = metaDao.get(KEY_LAST_SCAN)?.toLongOrNull() ?: 0L

    /**
     * Refreshes the cache from MediaStore.
     *
     * @param force ignore the freshness window and scan regardless.
     * @return number of tracks found, or -1 when the scan was skipped.
     */
    suspend fun refresh(force: Boolean = false): Int {
        val now = System.currentTimeMillis()
        if (!force && now - lastScanAt() < FRESHNESS_WINDOW_MS) return -1

        val scanned = MediaStoreScanner.scan(context)
        database.withTransaction {
            trackDao.deleteAll()
            trackDao.insertAll(scanned)
            metaDao.put(MetaEntry(KEY_LAST_SCAN, now.toString()))
        }
        return scanned.size
    }

    /**
     * Remembers the current track and where in it the user was.
     *
     * Written through the same meta table as the scan timestamp, so it survives a
     * library rescan — the rows in `tracks` are disposable, this is not.
     */
    suspend fun savePlayback(
        mediaId: String,
        positionMs: Long,
        shuffleEnabled: Boolean,
        repeatOrdinal: Int,
    ) {
        database.withTransaction {
            metaDao.put(MetaEntry(KEY_LAST_MEDIA_ID, mediaId))
            metaDao.put(MetaEntry(KEY_LAST_POSITION, positionMs.toString()))
            metaDao.put(MetaEntry(KEY_SHUFFLE, if (shuffleEnabled) "1" else "0"))
            metaDao.put(MetaEntry(KEY_REPEAT, repeatOrdinal.toString()))
        }
    }

    /** Null until something has been played at least once. */
    suspend fun loadPlayback(): SavedPlayback? {
        val mediaId = metaDao.get(KEY_LAST_MEDIA_ID)?.takeIf { it.isNotBlank() } ?: return null
        return SavedPlayback(
            mediaId = mediaId,
            positionMs = metaDao.get(KEY_LAST_POSITION)?.toLongOrNull() ?: 0L,
            shuffleEnabled = metaDao.get(KEY_SHUFFLE) == "1",
            repeatOrdinal = metaDao.get(KEY_REPEAT)?.toIntOrNull() ?: 0,
        )
    }

    companion object {
        const val KEY_LAST_SCAN = "last_scan_at"

        /** How long a cached library is considered good enough to skip a rescan. */
        const val FRESHNESS_WINDOW_MS = 15 * 60 * 1000L

        const val KEY_LAST_MEDIA_ID = "playback_media_id"
        const val KEY_LAST_POSITION = "playback_position_ms"
        const val KEY_SHUFFLE = "playback_shuffle"
        const val KEY_REPEAT = "playback_repeat"
    }
}
