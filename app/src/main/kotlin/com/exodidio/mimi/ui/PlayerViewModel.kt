package com.exodidio.mimi.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.listeningElapsedMs
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

    /**
     * Whether shuffle survives starting a list, mirrored from settings.
     *
     * Read on a plain field rather than suspending inside [play], because tapping a song
     * has to start it now: a database read on the way to the player would be a delay
     * between the tap and the sound.
     */
    @Volatile
    private var keepShuffleMode = true

    private var lastSavedMediaId: String? = null
    private var lastSavedBucket: Long = -1L

    /** The song listening history is currently being kept for, and whether it counts yet. */
    private var trackedMediaId: String? = null
    private var heard = false

    /** The song listening time is being kept for, and how much of it is not yet written. */
    private var listeningMediaId: String? = null
    private var listeningSinceElapsedMs: Long? = null
    private var listeningWasPlaying = false
    private var pendingListeningMs = 0L

    init {
        viewModelScope.launch {
            runCatching { store.settings.collect { keepShuffleMode = it.keepShuffleMode } }
        }
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

    /**
     * Play the whole list from [index], so next/previous walk the row the user tapped.
     *
     * Starting a new list is the moment "Keep shuffle mode" is about: with it off, the
     * shuffle state is cleared so the list plays in the order it was handed over.
     */
    fun play(tracks: List<Track>, index: Int) {
        if (!keepShuffleMode) engine.setShuffle(false)
        engine.playQueue(tracks, index)
    }

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
        // Deliberately not [play]: turning shuffle on and then asking for the list is the
        // one case where the keep-shuffle setting must not switch it straight back off.
        engine.playQueue(tracks, tracks.indices.random())
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

    fun setSpeed(speed: Float) = engine.setSpeed(speed)

    /** How far the music is shifted up, in semitones. */
    fun setPitch(semitones: Int) = engine.setPitch(semitones)

    /**
     * How loud the music plays.
     *
     * The level is the device's own, so it is remembered by the phone rather than by
     * this app: it is the same level the music plays at everywhere else.
     */
    fun setVolume(volume: Float) = engine.setVolume(volume)

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
     *
     * The same collector keeps listening time. Each reading of the monotonic clock adds the
     * stretch since the previous reading only while the player was actually playing, so a
     * pause and a later resume do not count the silence between them. The total is written
     * whenever the song changes, playback pauses, or a short stretch has built up, which is
     * what keeps a process that is killed from losing much of it. A skip writes whatever
     * was heard before it as well, because the time was still spent listening.
     */
    private fun trackHistory() {
        viewModelScope.launch {
            engine.state.collect { snapshot ->
                val now = SystemClock.elapsedRealtime()
                val current = snapshot.current

                if (current == null) {
                    pendingListeningMs += listeningElapsedMs(
                        wasPlaying = listeningWasPlaying,
                        sinceElapsedMs = listeningSinceElapsedMs,
                        nowElapsedMs = now,
                    )
                    flushListening()
                    trackedMediaId = null
                    heard = false
                    listeningMediaId = null
                    listeningSinceElapsedMs = null
                    listeningWasPlaying = false
                    return@collect
                }

                // The elapsed stretch belongs to the song that was playing before this
                // reading, so it is added before a change of song can move the label.
                pendingListeningMs += listeningElapsedMs(
                    wasPlaying = listeningWasPlaying,
                    sinceElapsedMs = listeningSinceElapsedMs,
                    nowElapsedMs = now,
                )
                if (current.mediaId != listeningMediaId) {
                    flushListening()
                    listeningMediaId = current.mediaId
                }
                listeningSinceElapsedMs = now
                listeningWasPlaying = snapshot.isPlaying

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

                if (!heard) {
                    val duration = snapshot.durationMs
                    val threshold =
                        if (duration > 0) minOf(PLAY_THRESHOLD_MS, duration / 2) else PLAY_THRESHOLD_MS
                    if (snapshot.positionMs >= threshold) {
                        heard = true
                        current.mediaId.toTrackId()?.let { id ->
                            runCatching { library.recordPlay(id) }
                        }
                    }
                }

                if (!snapshot.isPlaying || pendingListeningMs >= LISTEN_FLUSH_MS) {
                    flushListening()
                }
            }
        }
    }

    /**
     * Writes the listening time built up for the song it belongs to.
     *
     * The amount is taken before the write so a slow database call cannot be counted twice
     * by a second flush arriving while this one is still running.
     */
    private suspend fun flushListening() {
        val mediaId = listeningMediaId ?: return
        val elapsed = pendingListeningMs
        if (elapsed <= 0L) return
        pendingListeningMs = 0L

        val trackId = mediaId.toTrackId() ?: return
        runCatching { library.recordListening(trackId, elapsed, System.currentTimeMillis()) }
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

        /**
         * How much unwritten listening is allowed to build up before it reaches the
         * database. Short enough that a killed process loses little, long enough that a
         * song is not a write every time the player ticks.
         */
        const val LISTEN_FLUSH_MS = 10_000L
    }
}
