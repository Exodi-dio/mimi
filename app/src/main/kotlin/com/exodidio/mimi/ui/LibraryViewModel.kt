package com.exodidio.mimi.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.library.AlbumSummary
import com.exodidio.mimi.library.ArtistSummary
import com.exodidio.mimi.library.BackupFile
import com.exodidio.mimi.library.ComposerSummary
import com.exodidio.mimi.library.FolderSummary
import com.exodidio.mimi.library.GenreSummary
import com.exodidio.mimi.library.LibraryChangeObserver
import com.exodidio.mimi.library.ListenDay
import com.exodidio.mimi.library.M3uSong
import com.exodidio.mimi.library.PlayStat
import com.exodidio.mimi.library.PlaylistSummary
import com.exodidio.mimi.library.RestoreOutcome
import com.exodidio.mimi.library.Settings
import com.exodidio.mimi.library.TagValues
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.YearSummary
import com.exodidio.mimi.library.albumSummaries
import com.exodidio.mimi.library.artistSummaries
import com.exodidio.mimi.library.backupFileName
import com.exodidio.mimi.library.backupIntervalMillis
import com.exodidio.mimi.library.buildBackup
import com.exodidio.mimi.library.buildM3u
import com.exodidio.mimi.library.comparator
import com.exodidio.mimi.library.composerSummaries
import com.exodidio.mimi.library.documentDisplayName
import com.exodidio.mimi.library.folderSummaries
import com.exodidio.mimi.library.genreSummaries
import com.exodidio.mimi.library.hiddenNames
import com.exodidio.mimi.library.isExcludedFolder
import com.exodidio.mimi.library.isWithinDurationRange
import com.exodidio.mimi.library.matchM3uEntries
import com.exodidio.mimi.library.parseBackup
import com.exodidio.mimi.library.parseM3u
import com.exodidio.mimi.library.readDocumentText
import com.exodidio.mimi.library.safeFileName
import com.exodidio.mimi.library.tracksWithoutHidden
import com.exodidio.mimi.library.writeDocumentInTree
import com.exodidio.mimi.library.writeDocumentText
import com.exodidio.mimi.library.yearSummaries
import com.exodidio.mimi.widgets.refreshWidgets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val library = (application as MimiApplication).library

    private val settings = (application as MimiApplication).settings

    /**
     * The application itself, for the files this app reads and writes outside itself.
     *
     * The constructor's own copy is only in scope while the class is being set up, so a
     * method that needs the application later asks for it here instead.
     */
    private val app: Application = application

    /**
     * A list that has to be re-read when the artist separators change.
     *
     * Splitting names is a setting rather than a property of the tags, so the same cache
     * has to be grouped again the moment it changes. Everything else about a detail
     * screen, such as which artist was opened, stays as it was.
     */
    private fun <T> withArtistSeparators(read: (List<String>) -> Flow<T>): Flow<T> =
        settings.settings
            .map { it.artistSplit }
            .distinctUntilChanged()
            .flatMapLatest { separators -> read(separators) }

    /** The same, for the genre lists, which are split at their own separators. */
    private fun <T> withGenreSeparators(read: (List<String>) -> Flow<T>): Flow<T> =
        settings.settings
            .map { it.genreSplit }
            .distinctUntilChanged()
            .flatMapLatest { separators -> read(separators) }

    init {
        // Which folders are left out is the one setting the scanner itself has to know
        // about, so changing it rebuilds the library rather than waiting for the next
        // scan to notice. The first emission is what is already there, and is dropped so
        // opening the app does not scan twice.
        viewModelScope.launch {
            settings.settings
                .map { it.excludedFolders }
                .distinctUntilChanged()
                .drop(1)
                .collect { refresh(force = true) }
        }
    }

    /**
     * The library with hidden artists and albums already left out.
     *
     * Every list and count is derived from this one flow, so hiding a name is one filter
     * in one place rather than a rule each screen has to remember to apply. The length
     * range is applied here too, which is what makes moving a thumb take effect on the
     * list at once rather than at the end of the next scan.
     */
    private val visibleTracks: StateFlow<List<Track>> =
        combine(library.tracks, settings.settings) { all, chosen ->
            all.tracksWithoutHidden(chosen.hiddenNames())
                .filter { isWithinDurationRange(it.durationMs, chosen.minDurationMs, chosen.maxDurationMs) }
                .filterNot { isExcludedFolder(it.folder, chosen.excludedFolders) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Comes straight from the Room cache, so the list paints instantly on launch.
     *
     * The chosen order is applied here rather than in the query. The cache reads back one
     * way, and sorting it again is a pass over a list already in memory, where asking the
     * database for each order would mean a different statement per choice.
     */
    val tracks: StateFlow<List<Track>> = combine(visibleTracks, settings.settings) { all, chosen ->
        all.sortedWith(chosen.songSort.comparator(chosen.sortDescending))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val albums: StateFlow<List<AlbumSummary>> =
        combine(visibleTracks, settings.settings) { all, chosen ->
            all.albumSummaries().sortedWith(
                chosen.albumSort.comparator(chosen.sortDescending),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val artists: StateFlow<List<ArtistSummary>> =
        combine(visibleTracks, settings.settings) { all, chosen ->
            all.artistSummaries(chosen.artistSplit).sortedWith(
                chosen.artistSort.comparator(chosen.sortDescending),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Detail screens ask for their own slice; the query is keyed by the screen's argument. */
    fun tracksForAlbum(albumId: Long): Flow<List<Track>> =
        library.tracksForAlbum(albumId).visible()

    fun tracksForArtist(artist: String): Flow<List<Track>> =
        withArtistSeparators { library.observeArtistTracks(artist, it) }.visible()

    fun albumsForArtist(artist: String): Flow<List<AlbumSummary>> =
        tracksForArtist(artist).map { it.albumSummaries() }

    val genres: StateFlow<List<GenreSummary>> =
        combine(visibleTracks, settings.settings) { all, chosen ->
            all.genreSummaries(chosen.genreSplit)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val composers: StateFlow<List<ComposerSummary>> = visibleTracks
        .map { it.composerSummaries() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val years: StateFlow<List<YearSummary>> = visibleTracks
        .map { it.yearSummaries() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val folders: StateFlow<List<FolderSummary>> = visibleTracks
        .map { it.folderSummaries() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun tracksInGenre(genre: String): Flow<List<Track>> =
        withGenreSeparators { library.observeGenreTracks(genre, it) }.visible()

    fun tracksByComposer(composer: String): Flow<List<Track>> =
        library.tracksByComposer(composer).visible()

    fun tracksInYear(year: Int): Flow<List<Track>> = library.tracksInYear(year).visible()

    fun tracksInFolder(folder: String): Flow<List<Track>> = library.tracksInFolder(folder).visible()

    /** Leaves hidden songs and songs outside the length range out of a detail screen's list. */
    private fun Flow<List<Track>>.visible(): Flow<List<Track>> =
        combine(this, settings.settings) { list, chosen ->
            list.tracksWithoutHidden(chosen.hiddenNames())
                .filter { isWithinDurationRange(it.durationMs, chosen.minDurationMs, chosen.maxDurationMs) }
                .filterNot { isExcludedFolder(it.folder, chosen.excludedFolders) }
        }

    /** What the home screen shows: a handful each, newest and most played first. */
    val recentlyAdded: StateFlow<List<Track>> =
        library.recentlyAdded(HOME_SECTION_LIMIT).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val recentlyPlayed: StateFlow<List<Track>> =
        library.recentlyPlayed(HOME_SECTION_LIMIT).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val mostPlayed: StateFlow<List<Track>> =
        library.mostPlayed(HOME_SECTION_LIMIT).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** How often each song has been played and skipped, keyed by song id. */
    val playStats: StateFlow<Map<Long, PlayStat>> = library.playStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    /** How long each song was heard on each local day, as recorded. */
    val listenDays: StateFlow<List<ListenDay>> = library.listenDays.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _query = MutableStateFlow("")

    /** What the search box holds. Kept here so it survives a trip to another tab. */
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Matches for the current query.
     *
     * A blank query matches nothing rather than everything: an empty search box that
     * fills with the entire library reads as if the search is broken.
     */
    val searchResults: StateFlow<List<Track>> = _query
        .flatMapLatest { text ->
            if (text.isBlank()) flowOf(emptyList()) else library.search(text)
        }
        .combine(settings.settings) { results, chosen ->
            results.tracksWithoutHidden(chosen.hiddenNames())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun onQueryChange(text: String) {
        _query.value = text
    }

    /** Searches that are worth offering again. */
    val recentSearches: StateFlow<List<String>> = library.recentSearches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /**
     * Remembers what is in the search box.
     *
     * Called when a search bears fruit rather than on every keystroke: what is worth
     * offering back is what somebody went on to use, not the false starts on the way.
     */
    fun rememberSearch() {
        val text = _query.value
        viewModelScope.launch { library.rememberSearch(text) }
    }

    fun forgetSearches() {
        viewModelScope.launch { library.forgetSearches() }
    }

    /**
     * Loved songs as a set rather than a list, because every track row asks whether it is
     * loved and a list would turn that into a scan per row.
     */
    val favoriteIds: StateFlow<Set<Long>> = library.favoriteIds
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )

    val lovedTracks: StateFlow<List<Track>> =
        combine(library.lovedTracks, settings.settings) { all, chosen ->
            all.tracksWithoutHidden(chosen.hiddenNames())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val playlists: StateFlow<List<PlaylistSummary>> = library.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun tracksInPlaylist(playlistId: Long): Flow<List<Track>> =
        library.tracksInPlaylist(playlistId).visible()

    fun toggleLoved(trackId: Long) {
        val loved = trackId in favoriteIds.value
        viewModelScope.launch { library.setLoved(trackId, loved = !loved) }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { onCreated(library.createPlaylist(trimmed)) }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { library.renamePlaylist(playlistId, trimmed) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { library.deletePlaylist(playlistId) }
    }

    /**
     * Hands back the songs of one album chosen at random.
     *
     * The pick is made from the albums the screen is showing, so a hidden album cannot be
     * chosen, and the songs are read fresh rather than from a snapshot so a rescan between
     * the tap and the read cannot leave the player holding rows that no longer exist.
     */
    fun playRandomAlbum(onReady: (List<Track>) -> Unit) {
        val choices = albums.value
        if (choices.isEmpty()) return

        val chosen = choices.random()
        viewModelScope.launch {
            runCatching { tracksForAlbum(chosen.albumId).first() }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let(onReady)
        }
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { library.addToPlaylist(playlistId, trackId) }
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { library.removeFromPlaylist(playlistId, trackId) }
    }

    fun moveInPlaylist(playlistId: Long, trackId: Long, toIndex: Int) {
        viewModelScope.launch { library.moveInPlaylist(playlistId, trackId, toIndex) }
    }

    /**
     * Makes a playlist out of a playlist file the listener picked.
     *
     * The songs are matched against the library by the path the file names and, failing
     * that, by the name of the file, and the ones that are not here are counted rather
     * than quietly left out: a playlist that came back short should say so.
     */
    fun importPlaylist(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val text = readDocumentText(app, uri)
            if (text == null) {
                onDone("That file could not be read.")
                return@launch
            }

            val match = matchM3uEntries(parseM3u(text), library.libraryFiles())
            if (match.matched.isEmpty()) {
                onDone("None of the songs in that file are in your library.")
                return@launch
            }

            val name = documentDisplayName(app, uri)
                ?.substringBeforeLast('.')
                ?.takeIf { it.isNotBlank() }
                ?: "Imported playlist"
            library.importPlaylist(name, match.matched)

            val skipped = if (match.skipped == 0) {
                ""
            } else {
                " ${match.skipped} were not in your library."
            }
            onDone("Added ${match.matched.size} songs to \u201c$name\u201d.$skipped")
        }
    }

    /**
     * Writes a playlist out as a file in the folder the listener chose.
     *
     * The file holds the paths of the songs with their titles and lengths, which is what
     * another player reads. A device that will not say where its songs are cannot produce
     * such a file, and says so rather than writing a file of nothing.
     */
    fun exportPlaylist(playlistId: Long, name: String, folder: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val tracks = library.tracksInPlaylistNow(playlistId)
            val files = library.libraryFiles().associateBy { it.trackId }

            val songs = tracks.mapNotNull { track ->
                val path = files[track.id]?.path ?: return@mapNotNull null
                M3uSong(
                    path = path,
                    title = track.title,
                    artist = track.artist,
                    durationMs = track.durationMs,
                )
            }

            if (songs.isEmpty()) {
                onDone("The device would not say where the songs are, so no file was written.")
                return@launch
            }

            val fileName = "${safeFileName(name)}.m3u"
            val written = writeDocumentInTree(app, folder, fileName, buildM3u(songs))
            onDone(
                if (written != null) {
                    "Exported ${songs.size} songs to $fileName."
                } else {
                    "The playlist could not be written to that folder."
                }
            )
        }
    }

    /**
     * Writes a backup into the document the listener chose.
     *
     * The backup holds the playlists, the settings and the listening history, and none of
     * the music: the songs are files on the device and stay there.
     */
    fun createBackup(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val text = buildBackup(library.buildBackupData(System.currentTimeMillis()))
            val written = writeDocumentText(app, uri, text)
            onDone(
                if (written) {
                    "Backup saved."
                } else {
                    "The backup could not be written to that file."
                }
            )
        }
    }

    /**
     * Reads a backup file and merges it into the library.
     *
     * A file that cannot be trusted is refused with the reason, because a restore that
     * quietly did nothing looks exactly like one that worked.
     */
    fun restoreBackup(uri: Uri, onDone: (RestoreOutcome) -> Unit) {
        viewModelScope.launch {
            val text = readDocumentText(app, uri)
            if (text == null) {
                onDone(RestoreOutcome.Refused("That file could not be read."))
                return@launch
            }

            onDone(
                when (val file = parseBackup(text)) {
                    is BackupFile.Read -> RestoreOutcome.Done(library.restoreBackup(file.backup))
                    is BackupFile.UnsupportedVersion -> RestoreOutcome.Refused(
                        "That backup was made by a newer version of the app."
                    )
                    is BackupFile.Truncated -> RestoreOutcome.Refused(
                        "That backup is incomplete, so it was not restored."
                    )
                    is BackupFile.Malformed -> RestoreOutcome.Refused(file.reason)
                }
            )
        }
    }

    /**
     * Writes an automatic backup when the one that was due is due.
     *
     * The time of the last one is kept beside the settings, so opening the app twice in
     * a day does not write two backups. Nothing is written when no folder has been
     * chosen: a backup that lands in the app's own storage is not one the listener can
     * find again.
     */
    private fun writeAutomaticBackupWhenDue(chosen: Settings) {
        if (!chosen.automaticBackups) return
        val folder = chosen.backupFolder
        if (folder.isBlank()) return

        val now = System.currentTimeMillis()
        viewModelScope.launch {
            if (now - library.lastBackupAt() < backupIntervalMillis(chosen.backupFrequency)) {
                return@launch
            }
            val text = buildBackup(library.buildBackupData(now))
            val written = writeDocumentInTree(app, folder, backupFileName(now), text)
            if (written != null) library.noteBackupWritten(now)
        }
    }

    /**
     * Keeps the corrected tags of a song whose file could not be written to.
     */
    fun keepTagOverride(trackId: Long, values: TagValues) {
        viewModelScope.launch { library.keepTagOverride(trackId, values) }
    }

    /**
     * Forgets a correction, which is what happens once the file itself holds the tags.
     */
    fun clearTagOverride(trackId: Long) {
        viewModelScope.launch { library.clearTagOverride(trackId) }
    }

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    /** How many cached rows the last completed scan dropped, or null before one has run. */
    private val _lastScanDropped = MutableStateFlow<Int?>(null)
    val lastScanDropped: StateFlow<Int?> = _lastScanDropped.asStateFlow()

    /** Watches the media store, so music copied onto the phone turns up on its own. */
    private val changeObserver = LibraryChangeObserver(application) { refresh(force = true) }

    /**
     * What the current cache was built from: the two length bounds and the blacklist.
     *
     * All three decide which songs exist, so they are one value: widening either bound or
     * changing the blacklist means the cache is missing songs, and one rebuild answers all
     * of them. The folders are sorted so two equal sets cannot look like a change.
     */
    private data class ScannedFilter(
        val minMs: Long,
        val maxMs: Long,
        val excludedFolders: List<String>,
    )

    private var scannedFilter: ScannedFilter? = null

    init {
        // Scanning follows the settings rather than happening on a fixed schedule. The
        // first reading is a warm start, which only scans if the cache has gone stale;
        // after that, only a change to what songs are allowed needs a rebuild, because
        // that one changes which songs exist at all.
        viewModelScope.launch {
            settings.settings.collect { chosen ->
                val filter = ScannedFilter(
                    minMs = chosen.minDurationMs,
                    maxMs = chosen.maxDurationMs,
                    excludedFolders = chosen.excludedFolders.sorted(),
                )
                val previous = scannedFilter
                scannedFilter = filter
                when {
                    previous == null -> if (chosen.scanOnLaunch) refresh(force = false)
                    previous != filter -> refresh(force = true)
                }
                if (chosen.watchForChanges) changeObserver.start() else changeObserver.stop()
                // The automatic backup is looked at here rather than on a timer of its
                // own, because the app is only ever running while somebody is using it,
                // and this is the moment the app is open and the settings are known.
                writeAutomaticBackupWhenDue(chosen)
            }
        }
    }

    override fun onCleared() {
        changeObserver.close()
        super.onCleared()
    }

    fun refresh(force: Boolean = true) {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            try {
                val result = library.refresh(force = force)
                if (!result.skipped) {
                    _lastScanDropped.value = result.dropped
                    // The widgets show the library, so a rebuilt library has to reach them.
                    refreshWidgets(app)
                }
            } catch (_: SecurityException) {
                // Permission revoked mid-flight; the permission gate will re-ask.
            } finally {
                _scanning.value = false
            }
        }
    }

    private companion object {
        const val HOME_SECTION_LIMIT = 20
    }
}
