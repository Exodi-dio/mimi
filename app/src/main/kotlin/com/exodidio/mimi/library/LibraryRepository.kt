package com.exodidio.mimi.library

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId

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
    private val favoriteDao = database.favoriteDao()
    private val playlistDao = database.playlistDao()
    private val playStatDao = database.playStatDao()

    private val listenDayDao = database.listenDayDao()

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

    /**
     * What has been searched for lately, most recent first.
     *
     * Watched like anything else, so a search remembered while the box is open shows up in
     * the list behind it without the screen having to ask again.
     */
    val recentSearches: Flow<List<String>> = metaDao
        .observeWithPrefix(RECENT_SEARCH_PREFIX)
        .map(::recentSearchesFrom)

    /**
     * Puts [text] at the front of the recent searches.
     *
     * The whole list is rewritten each time: it is a handful of rows, and clearing first
     * means the list can get shorter without leaving the old tail behind to reappear later.
     */
    suspend fun rememberSearch(text: String) = database.withTransaction {
        val current = recentSearchesFrom(metaDao.getAllWithPrefix(RECENT_SEARCH_PREFIX))
        val next = current.remembering(text)
        if (next == current) return@withTransaction

        metaDao.deleteWithPrefix(RECENT_SEARCH_PREFIX)
        metaDao.putAll(recentSearchRows(next))
    }

    /** Forgets everything that has been searched for. */
    suspend fun forgetSearches() {
        metaDao.deleteWithPrefix(RECENT_SEARCH_PREFIX)
    }

    /** The newest arrivals, for a shelf rather than a screen of its own. */
    fun recentlyAdded(limit: Int): Flow<List<Track>> = trackDao.observeRecentlyAdded(limit)

    val genres: Flow<List<GenreSummary>> = trackDao.observeGenres()

    val composers: Flow<List<ComposerSummary>> = trackDao.observeComposers()

    val years: Flow<List<YearSummary>> = trackDao.observeYears()

    val folders: Flow<List<FolderSummary>> = trackDao.observeFolders()

    fun tracksInGenre(genre: String): Flow<List<Track>> = trackDao.observeGenreTracks(genre)

    /**
     * The artists in the cache, with a track counted under every credit on it.
     *
     * The stored column holds the tag as written, so a duet is one row there. Splitting it
     * into names is what stops `A & B feat. C` from becoming one artist called that, and
     * it is done here rather than in SQL because the separators are a setting and the
     * pattern would have to be built into the statement anyway.
     *
     * The work is a pass over rows already in memory, so changing the separators re-groups
     * the list without asking MediaStore for anything.
     */
    fun observeArtists(separators: List<String>): Flow<List<ArtistSummary>> =
        trackDao.observeAll().map { tracks ->
            tracks.groupByCredit({ it.artist }, separators)
                .map { (name, credited) ->
                    ArtistSummary(
                        artist = name,
                        trackCount = credited.size,
                        // Distinct albums, not credits: two songs from one record still
                        // make one album on the artist's page.
                        albumCount = credited.map { it.albumId }.distinct().size,
                    )
                }
                .sortedBy { it.artist.lowercase() }
        }

    /** A track belongs to an artist when any of its credits names that artist. */
    fun observeArtistTracks(artist: String, separators: List<String>): Flow<List<Track>> =
        trackDao.observeAll().map { tracks ->
            tracks.filter { it.artist.isCreditFor(artist, separators) }
                .sortedForAlbumOrder()
        }

    /**
     * The albums an artist appears on.
     *
     * Derived from the split credits as well, because an album shared with another artist
     * belongs on both pages rather than only on the one whose name happened to be in the
     * tag first.
     */
    fun observeArtistAlbums(artist: String, separators: List<String>): Flow<List<AlbumSummary>> =
        observeArtistTracks(artist, separators).map { tracks ->
            tracks.filter { !it.album.isNullOrBlank() }
                .groupBy { it.albumId }
                .map { (albumId, albumTracks) ->
                    AlbumSummary(
                        albumId = albumId,
                        album = albumTracks.first().album.orEmpty(),
                        artist = albumTracks.mapNotNull { it.artist }.minOrNull(),
                        trackCount = albumTracks.size,
                        artworkUri = albumTracks.map { it.contentUri }.minOrNull(),
                    )
                }
                .sortedBy { it.album.lowercase() }
        }

    /** The genres in the cache, with a track counted under every genre it is filed under. */
    fun observeGenres(separators: List<String>): Flow<List<GenreSummary>> =
        trackDao.observeAll().map { tracks ->
            tracks.groupByCredit({ it.genre }, separators)
                .map { (name, filed) ->
                    GenreSummary(
                        genre = name,
                        trackCount = filed.size,
                        artworkUri = filed.map { it.contentUri }.minOrNull(),
                    )
                }
                .sortedBy { it.genre.lowercase() }
        }

    /** A track is filed under a genre when any of its genres names it. */
    fun observeGenreTracks(genre: String, separators: List<String>): Flow<List<Track>> =
        trackDao.observeAll().map { tracks ->
            tracks.filter { it.genre.isCreditFor(genre, separators) }
                .sortedForGenreOrder()
        }

    fun tracksByComposer(composer: String): Flow<List<Track>> = trackDao.observeComposerTracks(composer)

    fun tracksInYear(year: Int): Flow<List<Track>> = trackDao.observeYearTracks(year)

    fun tracksInFolder(folder: String): Flow<List<Track>> = trackDao.observeFolderTracks(folder)

    /** Ids of loved songs, for marking rows without loading the songs themselves. */
    val favoriteIds: Flow<List<Long>> = favoriteDao.observeIds()

    val lovedTracks: Flow<List<Track>> = favoriteDao.observeLoved()

    val playlists: Flow<List<PlaylistSummary>> = playlistDao.observePlaylists()

    fun tracksInPlaylist(playlistId: Long): Flow<List<Track>> = playlistDao.observePlaylistTracks(playlistId)

    suspend fun setLoved(trackId: Long, loved: Boolean) {
        if (loved) {
            favoriteDao.add(Favorite(trackId = trackId, addedAt = System.currentTimeMillis()))
        } else {
            favoriteDao.remove(trackId)
        }
    }

    /** Named after the user's input, trimmed. Returns the new playlist's id. */
    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(
            Playlist(name = name.trim(), createdAt = System.currentTimeMillis())
        )

    suspend fun renamePlaylist(playlistId: Long, name: String) =
        playlistDao.rename(playlistId, name.trim())

    suspend fun deletePlaylist(playlistId: Long) {
        database.withTransaction {
            playlistDao.deleteEntries(playlistId)
            playlistDao.deletePlaylist(playlistId)
        }
    }

    /**
     * Appends a song to a playlist. Doing nothing when it is already there keeps a song
     * from appearing twice in the same list.
     */
    suspend fun addToPlaylist(playlistId: Long, trackId: Long) {
        database.withTransaction {
            if (playlistDao.entryCount(playlistId, trackId) > 0) return@withTransaction
            playlistDao.insertEntry(
                PlaylistEntry(
                    playlistId = playlistId,
                    trackId = trackId,
                    position = playlistDao.nextPosition(playlistId),
                )
            )
        }
    }

    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) =
        playlistDao.removeEntry(playlistId, trackId)

    /**
     * Moves a song to a new place in its playlist.
     *
     * Every position from the destination onwards is rewritten rather than only the two
     * that swapped, because the gaps left by earlier removals make a running order that is
     * merely sorted into place drift apart over time. One transaction, so the list is never
     * read half-moved.
     */
    suspend fun moveInPlaylist(playlistId: Long, trackId: Long, toIndex: Int) {
        database.withTransaction {
            val order = playlistDao.entryTrackIds(playlistId).toMutableList()
            val from = order.indexOf(trackId)
            if (from < 0) return@withTransaction

            val to = toIndex.coerceIn(0, order.lastIndex)
            if (from == to) return@withTransaction

            order.removeAt(from)
            order.add(to, trackId)
            order.forEachIndexed { index, id -> playlistDao.setPosition(playlistId, id, index) }
        }
    }

    /**
     * Listening history, keyed by track.
     *
     * Handed out as a map rather than a list because every caller wants to ask "how many
     * times has *this* song been played", and a list would turn each of those questions
     * back into a scan.
     */
    val playStats: Flow<Map<Long, PlayStat>> = playStatDao.observeAll()
        .map { stats -> stats.associateBy { it.trackId } }

    /**
     * Listening time per song per local day.
     *
     * Handed over as rows rather than as a prepared chart because every period the picker
     * offers is a fold over the same small list, and doing that in one pure place keeps the
     * screen free of date arithmetic.
     */
    val listenDays: Flow<List<ListenDay>> = listenDayDao.observeAll()

    fun recentlyPlayed(limit: Int): Flow<List<Track>> = playStatDao.observeRecentlyPlayed(limit)

    fun mostPlayed(limit: Int): Flow<List<Track>> = playStatDao.observeMostPlayed(limit)

    /**
     * One listen, counted.
     *
     * The insert comes first so a song that has never been played still has a row to
     * update; `IGNORE` on conflict means an existing row — and the history in it — is left
     * exactly as it was.
     *
     * The daily row gets the play in the same transaction: a play is the moment an attempt
     * ended with the song heard, and the chart needs that on the day it happened. Listening
     * time is written separately, because a skip still counts whatever was heard before it.
     */
    suspend fun recordPlay(trackId: Long) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            playStatDao.insertIfAbsent(PlayStat(trackId = trackId))
            playStatDao.recordPlay(trackId, now)
            listenDayDao.addListening(
                trackId = trackId,
                day = localDayOf(now, ZoneId.systemDefault()),
                listenedMs = 0L,
                plays = 1,
            )
        }
    }

    /** One skip, counted. A song skipped before it was ever played gets a row too. */
    suspend fun recordSkip(trackId: Long) {
        database.withTransaction {
            playStatDao.insertIfAbsent(PlayStat(trackId = trackId))
            playStatDao.recordSkip(trackId)
        }
    }

    /**
     * Adds [listenedMs] of real listening to the song's row for the day [at] fell on.
     *
     * The duration comes from a monotonic clock at the player, so it is what was actually
     * heard rather than the difference between two wall clock readings. The day is decided
     * here, once, so every writer files listening under the same local date.
     */
    suspend fun recordListening(trackId: Long, listenedMs: Long, at: Long) {
        if (listenedMs <= 0L) return
        listenDayDao.addListening(
            trackId = trackId,
            day = localDayOf(at, ZoneId.systemDefault()),
            listenedMs = listenedMs,
            plays = 0,
        )
    }

    suspend fun lastScanAt(): Long = metaDao.get(KEY_LAST_SCAN)?.toLongOrNull() ?: 0L

    /**
     * Refreshes the cache from MediaStore.
     *
     * The ids already cached are read before the rebuild so the result can say how many
     * rows went away. A row can disappear because the file did, but also because it moved
     * into an excluded folder or below the shortest-song bound; from the cache's side
     * those are the same thing, and the count is what makes the rebuild visible instead of
     * silent.
     *
     * @param force ignore the freshness window and scan regardless.
     */
    suspend fun refresh(force: Boolean = false): ScanResult {
        val now = System.currentTimeMillis()
        if (!force && now - lastScanAt() < FRESHNESS_WINDOW_MS) {
            return ScanResult(found = 0, dropped = 0, skipped = true)
        }

        val previousIds = trackDao.ids()
        val chosen = Settings.from(metaDao.getAllWithPrefix(Settings.PREFIX))
        val scanned = MediaStoreScanner.scan(
            context = context,
            minDurationMs = chosen.minSongLength.millis,
            excludedFolders = chosen.excludedFolders,
        )
        val foundIds = scanned.mapTo(HashSet()) { it.id }
        val dropped = previousIds.count { it !in foundIds }

        database.withTransaction {
            trackDao.deleteAll()
            trackDao.insertAll(scanned)
            // History for files that are gone, dropped in the same breath as the files
            // themselves: the track list has just been rebuilt, so this is the one moment
            // where "no longer on the device" is known rather than guessed.
            playStatDao.prune()
            listenDayDao.prune()
            metaDao.put(MetaEntry(KEY_LAST_SCAN, now.toString()))
        }
        return ScanResult(found = scanned.size, dropped = dropped, skipped = false)
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

/**
 * Groups tracks under each of the credits [selector] names, keeping first-seen order.
 *
 * Case is folded for the key so a library that is inconsistent about capitalisation does
 * not grow two artist rows, while the spelling shown is the first one encountered. Each
 * track is deduplicated within itself by [splitCredits], so a tag naming the same artist
 * twice still counts once.
 */
private fun List<Track>.groupByCredit(
    selector: (Track) -> String?,
    separators: List<String>,
): List<Pair<String, List<Track>>> {
    val spellings = LinkedHashMap<String, String>()
    val groups = HashMap<String, MutableList<Track>>()

    forEach { track ->
        splitCredits(selector(track), separators).forEach { credit ->
            val key = credit.lowercase()
            spellings.getOrPut(key) { credit }
            groups.getOrPut(key) { mutableListOf() }.add(track)
        }
    }

    return spellings.map { (key, spelling) -> spelling to groups.getValue(key).toList() }
}

/** Whether [name] is one of the credits on a value, ignoring case and stray spacing. */
private fun String?.isCreditFor(name: String, separators: List<String>): Boolean =
    splitCredits(this, separators).any { it.equals(name.trim(), ignoreCase = true) }

/** Album running order, which is what an artist page wants inside each record. */
private fun List<Track>.sortedForAlbumOrder(): List<Track> = sortedWith(
    compareBy<Track> { it.album.orEmpty().lowercase() }
        .thenBy { it.trackNumber }
        .thenBy { it.title.lowercase() }
)

/** Artist, then album, then running order: the shape the genre screen has always had. */
private fun List<Track>.sortedForGenreOrder(): List<Track> = sortedWith(
    compareBy<Track> { it.artist.orEmpty().lowercase() }
        .thenBy { it.album.orEmpty().lowercase() }
        .thenBy { it.trackNumber }
        .thenBy { it.title.lowercase() }
)
