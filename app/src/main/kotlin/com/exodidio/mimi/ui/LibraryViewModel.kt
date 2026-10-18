package com.exodidio.mimi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.library.Track
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
