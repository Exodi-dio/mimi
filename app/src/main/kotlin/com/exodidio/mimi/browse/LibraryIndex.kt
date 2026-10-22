package com.exodidio.mimi.browse

import android.content.Context
import com.exodidio.mimi.MimiApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The library as the session sees it.
 *
 * The browse tree and a browser's play requests both need the songs, and both are
 * answered from memory: a car walks the same shelves over and over, and the cache is
 * already in the database, so it is read once and kept until the library changes.
 */
class LibraryIndex(context: Context) {

    private val app = context.applicationContext as MimiApplication

    /** The songs as the tree sees them. */
    @Volatile
    private var songs: List<BrowseSong> = emptyList()

    /** The separators the artist credits are split at. */
    @Volatile
    private var separators: List<String> = emptyList()

    /** The tree over what is known. */
    fun browse(): LibraryBrowse = LibraryBrowse(songs, separators)

    /** Every song the library holds, in the order the tree lists them. */
    fun songs(): List<BrowseSong> = songs

    /** The song with [mediaId], or null when the library does not hold it. */
    fun song(mediaId: String): BrowseSong? = songs.firstOrNull { it.mediaId == mediaId }

    /** How many songs are held. */
    val size: Int get() = songs.size

    /**
     * Watches the library and the settings.
     *
     * The tree is asked for from a binder, so it cannot wait for a database read while
     * it is being answered. Watching the cache instead means the songs are already
     * there by the time anything asks for them.
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            app.library.tracks.collect { rows -> songs = rows.map { it.toBrowseSong() } }
        }
        scope.launch {
            app.settings.settings.collect { chosen -> separators = chosen.artistSplit }
        }
    }

    /**
     * Reads the library again, once.
     *
     * Used when something asks for the tree before the watch has delivered anything,
     * which is what happens when the service has only just started.
     */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        val rows = app.library.tracks.first()
        val chosen = app.settings.settings.first()
        separators = chosen.artistSplit
        songs = rows.map { it.toBrowseSong() }
    }
}
