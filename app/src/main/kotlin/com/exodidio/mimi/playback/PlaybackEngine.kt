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

/**
 * A track as the player holds it.
 *
 * The same shape serves the track playing now and every entry in the queue, because
 * those are one thing seen from two angles: [PlaybackState.current] is simply the entry
 * in [PlaybackState.queue] that the player is sitting on.
 */
data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String?,
    val album: String?,
)

/** One immutable snapshot of everything the player UI draws. */
data class PlaybackState(
    val connected: Boolean = false,
    val current: QueueItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    /** How fast the music runs, as a multiple of normal speed. */
    val speed: Float = 1f,
    /** Everything loaded, in play order. [current] is this list at [currentIndex]. */
    val queue: List<QueueItem> = emptyList(),
    /** Where [current] sits in [queue], or -1 when nothing is loaded. */
    val currentIndex: Int = -1,
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

    /** How fast to play, as a multiple of normal speed. */
    fun setSpeed(speed: Float)

    /** Puts [track] straight after the one playing, without interrupting it. */
    fun playNext(track: Track)

    /** Appends [track] to the end of the queue. */
    fun addToQueue(track: Track)

    /** Jumps to a queue entry and starts playing it. */
    fun jumpToQueueItem(index: Int)

    /** Moves a queue entry within the queue, which is how what is coming up is reordered. */
    fun moveInQueue(fromIndex: Int, toIndex: Int)

    /** Drops a queue entry. Removing the one playing moves playback along to the next. */
    fun removeFromQueue(index: Int)
}
