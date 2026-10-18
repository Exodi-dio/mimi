package com.exodidio.mimi.playback

import com.exodidio.mimi.library.Track
import kotlinx.coroutines.flow.StateFlow

/** What happens when the queue runs out. */
enum class RepeatMode {
    OFF,
    ALL,
    ONE;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }
}

/** The track currently loaded in the player, in the shape the UI needs it. */
data class NowPlaying(
    val mediaId: String,
    val title: String,
    val artist: String?,
    val album: String?,
)

/** One immutable snapshot of everything the player UI draws. */
data class PlaybackState(
    val connected: Boolean = false,
    val current: NowPlaying? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
)

/**
 * The only thing the app talks to for playback. Media3 sits behind this today; an
 * FFmpeg-backed engine can slot in later without any of the UI noticing.
 */
interface PlaybackEngine {
    val state: StateFlow<PlaybackState>

    suspend fun connect()
    fun release()

    fun playQueue(tracks: List<Track>, startIndex: Int)

    /**
     * Loads a queue but leaves it paused, already sitting at [startPositionMs].
     *
     * Used to put the previous session back on screen at launch: the user should see
     * what they were listening to and choose to hit play, not be blasted with audio
     * the moment the app opens.
     */
    fun prepareQueue(tracks: List<Track>, startIndex: Int, startPositionMs: Long)

    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun setShuffle(enabled: Boolean)
    fun setRepeat(mode: RepeatMode)
}
