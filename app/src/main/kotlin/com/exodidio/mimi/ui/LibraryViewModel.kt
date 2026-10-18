package com.exodidio.mimi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.library.AlbumSummary
import com.exodidio.mimi.library.ArtistSummary
import com.exodidio.mimi.library.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
