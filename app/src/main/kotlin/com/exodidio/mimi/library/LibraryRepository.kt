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

    companion object {
        const val KEY_LAST_SCAN = "last_scan_at"

        /** How long a cached library is considered good enough to skip a rescan. */
        const val FRESHNESS_WINDOW_MS = 15 * 60 * 1000L
    }
}
