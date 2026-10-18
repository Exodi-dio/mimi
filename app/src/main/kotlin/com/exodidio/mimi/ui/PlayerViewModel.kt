package com.exodidio.mimi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.playback.Media3PlaybackEngine
import com.exodidio.mimi.playback.PlaybackEngine
import com.exodidio.mimi.playback.PlaybackState
import com.exodidio.mimi.playback.RepeatMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val library = (application as MimiApplication).library
    private val engine: PlaybackEngine = Media3PlaybackEngine(application, viewModelScope)

    val state: StateFlow<PlaybackState> = engine.state

    /** Restoring must happen at most once, and only if nothing is playing already. */
    private var restored = false

    private var lastSavedMediaId: String? = null
    private var lastSavedBucket: Long = -1L

    init {
        viewModelScope.launch {
            runCatching { engine.connect() }
            runCatching { restoreSession() }
        }
        persistSession()
    }

    /** Play the whole list from [index], so next/previous walk the row the user tapped. */
    fun play(tracks: List<Track>, index: Int) = engine.playQueue(tracks, index)

    fun togglePlayPause() = engine.togglePlayPause()

    fun next() = engine.next()

    fun previous() = engine.previous()

    fun seekTo(positionMs: Long) = engine.seekTo(positionMs)

    fun toggleShuffle() = engine.setShuffle(!engine.state.value.shuffleEnabled)

    fun cycleRepeat() = engine.setRepeat(engine.state.value.repeatMode.next())

    override fun onCleared() {
        engine.release()
    }

    /**
     * Puts the previous session back on screen, paused at the saved position.
     *
     * The session may outlive this ViewModel — the playback service keeps running — so
     * anything already loaded wins over what was written to the database.
     */
    private suspend fun restoreSession() {
        if (restored) return
        restored = true
        if (engine.state.value.current != null) return

        val saved = library.loadPlayback() ?: return
        val tracks = library.tracks.first()
        val index = tracks.indexOfFirst { it.contentUri == saved.mediaId }
        if (index < 0) return

        engine.setShuffle(saved.shuffleEnabled)
        engine.setRepeat(RepeatMode.entries.getOrNull(saved.repeatOrdinal) ?: RepeatMode.OFF)
        engine.prepareQueue(tracks, index, saved.positionMs)
    }

    /**
     * Writes the session back as it plays.
     *
     * The player reports a new position every tick, which is far too often to write to
     * disk, so writes are collapsed to one per [SAVE_INTERVAL_MS] bucket — plus an
     * immediate write whenever the track itself changes.
     */
    private fun persistSession() {
        viewModelScope.launch {
            engine.state.collect { snapshot ->
                val current = snapshot.current ?: return@collect
                val bucket = snapshot.positionMs / SAVE_INTERVAL_MS
                if (current.mediaId == lastSavedMediaId && bucket == lastSavedBucket) return@collect

                lastSavedMediaId = current.mediaId
                lastSavedBucket = bucket
                runCatching {
                    library.savePlayback(
                        mediaId = current.mediaId,
                        positionMs = snapshot.positionMs,
                        shuffleEnabled = snapshot.shuffleEnabled,
                        repeatOrdinal = snapshot.repeatMode.ordinal,
                    )
                }
            }
        }
    }

    private companion object {
        const val SAVE_INTERVAL_MS = 5_000L
    }
}
