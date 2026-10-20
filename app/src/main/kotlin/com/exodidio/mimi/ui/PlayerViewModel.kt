package com.exodidio.mimi.ui

import android.app.Application
import android.net.Uri
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
    private val store = (application as MimiApplication).settings
    private val sleepTimer = (application as MimiApplication).sleepTimer
    private val engine: PlaybackEngine = Media3PlaybackEngine(application, viewModelScope)

    val state: StateFlow<PlaybackState> = engine.state

    /** Restoring must happen at most once, and only if nothing is playing already. */
    private var restored = false

    private var lastSavedMediaId: String? = null
    private var lastSavedBucket: Long = -1L

    /** The song listening history is currently being kept for, and whether it counts yet. */
    private var trackedMediaId: String? = null
    private var heard = false

    init {
        viewModelScope.launch {
            runCatching { engine.connect() }
            // The session is still written down even when it is not restored, so turning
            // this on later picks up from wherever the app was left.
            val chosen = runCatching { store.settings.first() }.getOrNull()
            if (chosen?.resumeSession != false) runCatching { restoreSession() }
        }
        persistSession()
        trackHistory()
    }

    /** Play the whole list from [index], so next/previous walk the row the user tapped. */
    fun play(tracks: List<Track>, index: Int) = engine.playQueue(tracks, index)

    fun togglePlayPause() = engine.togglePlayPause()

    fun next() = engine.next()

    fun previous() = engine.previous()

    fun seekTo(positionMs: Long) = engine.seekTo(positionMs)

    fun toggleShuffle() = engine.setShuffle(!engine.state.value.shuffleEnabled)

    fun cycleRepeat() = engine.setRepeat(engine.state.value.repeatMode.next())

    fun playNext(track: Track) = engine.playNext(track)

    fun addToQueue(track: Track) = engine.addToQueue(track)

    /**
     * Plays the whole library at random.
     *
     * Shuffle is turned on and playback starts at an arbitrary entry rather than a
     * shuffled copy of the list: the queue then holds every song in its real order, so the
     * up-next panel keeps showing what is genuinely loaded, and turning shuffle off
     * mid-listen lands somewhere sensible instead of on a reshuffled ghost.
     */
    fun shuffleAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        engine.setShuffle(true)
        play(tracks, tracks.indices.random())
    }

    fun jumpToQueueItem(index: Int) = engine.jumpToQueueItem(index)

    fun moveInQueue(fromIndex: Int, toIndex: Int) = engine.moveInQueue(fromIndex, toIndex)

    fun removeFromQueue(index: Int) = engine.removeFromQueue(index)

    fun clearUpcoming() = engine.clearUpcoming()

    /**
     * Keeps what is queued as a playlist.
     *
     * The queue holds media ids, and the songs themselves are looked up again rather than
     * remembered, so a playlist made from a queue is made of songs the library still has.
     * Anything that has since gone from the device is simply left out.
     */
    fun saveQueueAsPlaylist(name: String) {
        if (name.isBlank()) return
        val mediaIds = engine.state.value.queue.map { it.mediaId }
        if (mediaIds.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                val byUri = library.tracks.first().associateBy { it.contentUri }
                val ordered = mediaIds.mapNotNull { byUri[it] }
                if (ordered.isEmpty()) return@runCatching

                val playlistId = library.createPlaylist(name)
                ordered.forEach { library.addToPlaylist(playlistId, it.id) }
            }
        }
    }

    /**
     * Nudges playback by [deltaMs], kept inside the song.
     *
     * The step buttons are the difference between a player that is occasionally useful and
     * one that is not, for anything long enough that finding a spot by dragging is fiddly.
     */
    fun seekBy(deltaMs: Long) {
        val snapshot = engine.state.value
        val target = (snapshot.positionMs + deltaMs).coerceAtLeast(0L)
        engine.seekTo(if (snapshot.durationMs > 0L) target.coerceAtMost(snapshot.durationMs) else target)
    }

    fun setSpeed(speed: Float) = engine.setSpeed(speed)

    /** What is left of the sleep timer, or null when it is not running. */
    val sleepRemaining: StateFlow<Long?> = sleepTimer.remaining

    fun startSleepTimer(durationMs: Long) = sleepTimer.start(durationMs)

    fun cancelSleepTimer() = sleepTimer.cancel()

    /** True while the timer is waiting for the current song to finish. */
    val sleepAtEndOfTrack: StateFlow<Boolean> = sleepTimer.stopAfterSong

    fun stopAfterSong() = sleepTimer.stopAfterSong()

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

    /**
     * Keeps the listening history.
     *
     * A song counts as heard once it has run past [PLAY_THRESHOLD_MS] — or half its length,
     * if it is shorter than that. Reaching that point is the difference between playing a
     * song and passing over it, and it needs nothing from the UI to detect.
     *
     * Skips fall out of the same rule for free: the threshold is never more than half a
     * song, so a song that reaches its own end has always been heard. Any song that is
     * replaced while still unheard was therefore not finished — it was moved on from, by
     * the next button or by the listener picking something else.
     */
    private fun trackHistory() {
        viewModelScope.launch {
            engine.state.collect { snapshot ->
                val current = snapshot.current
                if (current == null) {
                    trackedMediaId = null
                    heard = false
                    return@collect
                }

                if (current.mediaId != trackedMediaId) {
                    val abandoned = trackedMediaId?.takeIf { !heard }
                    trackedMediaId = current.mediaId
                    heard = false
                    abandoned?.let { mediaId ->
                        mediaId.toTrackId()?.let { id ->
                            runCatching { library.recordSkip(id) }
                        }
                    }
                }

                if (heard) return@collect
                val duration = snapshot.durationMs
                val threshold =
                    if (duration > 0) minOf(PLAY_THRESHOLD_MS, duration / 2) else PLAY_THRESHOLD_MS
                if (snapshot.positionMs < threshold) return@collect

                heard = true
                current.mediaId.toTrackId()?.let { id ->
                    runCatching { library.recordPlay(id) }
                }
            }
        }
    }

    /**
     * The song behind a media id.
     *
     * The engine names every item by its content URI, which ends in the MediaStore id the
     * library keys its rows by, so history is written against the same id everything else
     * uses. Anything that does not parse simply goes uncounted.
     */
    private fun String.toTrackId(): Long? = runCatching {
        Uri.parse(this).lastPathSegment?.toLongOrNull()
    }.getOrNull()

    private companion object {
        const val SAVE_INTERVAL_MS = 5_000L

        /**
         * How much of a song has to be heard before it counts as played. Twenty seconds is
         * the convention the streaming players settled on, and it is what stops a song
         * skipped on the first bar from topping the most-played list.
         */
        const val PLAY_THRESHOLD_MS = 20_000L
    }
}
