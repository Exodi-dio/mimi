package com.exodidio.mimi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.library.AlbumSummary
import com.exodidio.mimi.library.ArtistSummary
import com.exodidio.mimi.library.PlaylistSummary
import com.exodidio.mimi.library.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val library = (application as MimiApplication).library

    /**
     * Comes straight from the Room cache, so the list paints instantly on launch.
     */
    val tracks: StateFlow<List<Track>> = library.tracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val albums: StateFlow<List<AlbumSummary>> = library.albums.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val artists: StateFlow<List<ArtistSummary>> = library.artists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** Detail screens ask for their own slice; the query is keyed by the screen's argument. */
    fun tracksForAlbum(albumId: Long): Flow<List<Track>> = library.tracksForAlbum(albumId)

    fun tracksForArtist(artist: String): Flow<List<Track>> = library.tracksForArtist(artist)

    fun albumsForArtist(artist: String): Flow<List<AlbumSummary>> = library.albumsForArtist(artist)

    private val _query = MutableStateFlow("")

    /** What the search box holds. Kept here so it survives a trip to another tab. */
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Matches for the current query.
     *
     * A blank query matches nothing rather than everything: an empty search box that
     * fills with the entire library reads as if the search is broken.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Track>> = _query
        .flatMapLatest { text ->
            if (text.isBlank()) flowOf(emptyList()) else library.search(text)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun onQueryChange(text: String) {
        _query.value = text
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

    val lovedTracks: StateFlow<List<Track>> = library.lovedTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val playlists: StateFlow<List<PlaylistSummary>> = library.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun tracksInPlaylist(playlistId: Long): Flow<List<Track>> = library.tracksInPlaylist(playlistId)

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

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { library.addToPlaylist(playlistId, trackId) }
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { library.removeFromPlaylist(playlistId, trackId) }
    }

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    init {
        // Warm start: refresh only if the cache has gone stale.
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            try {
                library.refresh(force = force)
            } catch (_: SecurityException) {
                // Permission revoked mid-flight; the permission gate will re-ask.
            } finally {
                _scanning.value = false
            }
        }
    }
}
