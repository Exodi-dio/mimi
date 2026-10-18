package com.exodidio.mimi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.playback.Media3PlaybackEngine
import com.exodidio.mimi.playback.PlaybackEngine
import com.exodidio.mimi.playback.PlaybackState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val engine: PlaybackEngine = Media3PlaybackEngine(application, viewModelScope)

    val state: StateFlow<PlaybackState> = engine.state

    init {
        viewModelScope.launch { runCatching { engine.connect() } }
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
}
