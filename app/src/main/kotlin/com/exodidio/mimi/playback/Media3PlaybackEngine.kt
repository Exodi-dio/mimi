package com.exodidio.mimi.playback

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.exodidio.mimi.library.Track
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Playback through Media3. We hold a [MediaController] rather than the player itself
 * so the actual playback lives in [MimiPlaybackService] and keeps running when the
 * app is not on screen.
 */
class Media3PlaybackEngine(
    private val context: Context,
    private val scope: CoroutineScope,
) : PlaybackEngine {

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var ticker: Job? = null

    /** The device's own mixer, which is what a volume slider moves. */
    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)

    /**
     * The queue is read from the session one item at a time, and every read crosses a
     * binder boundary. Doing that on the twice-a-second tick would be hundreds of calls
     * a second for a list that only changes when the timeline does, so it is cached and
     * rebuilt on the timeline-changed event instead.
     */
    private var queueDirty = true
    private var cachedQueue: List<QueueItem> = emptyList()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) queueDirty = true
            publish(player)
        }
    }

    override suspend fun connect() {
        if (controller != null) return

        val token = SessionToken(context, ComponentName(context, MimiPlaybackService::class.java))
        val connected = suspendCancellableCoroutine { continuation ->
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener(
                {
                    try {
                        continuation.resume(future.get())
                    } catch (throwable: Throwable) {
                        continuation.resumeWithException(throwable)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
            continuation.invokeOnCancellation { MediaController.releaseFuture(future) }
        }

        controller = connected
        connected.addListener(listener)
        queueDirty = true
        publish(connected)
        startTicker()
    }

    override fun release() {
        ticker?.cancel()
        ticker = null
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        cachedQueue = emptyList()
        queueDirty = true
        _state.value = PlaybackState()
    }

    override fun playQueue(tracks: List<Track>, startIndex: Int) {
        val player = controller ?: return
        if (tracks.isEmpty()) return

        val items = tracks.map { it.toMediaItem() }
        val index = startIndex.coerceIn(0, items.lastIndex)
        player.setMediaItems(items, index, 0L)
        player.prepare()
        player.play()
    }

    override fun prepareQueue(tracks: List<Track>, startIndex: Int, startPositionMs: Long) {
        val player = controller ?: return
        if (tracks.isEmpty()) return

        val items = tracks.map { it.toMediaItem() }
        val index = startIndex.coerceIn(0, items.lastIndex)
        // Same as playQueue, minus play(): preparing leaves the transport stopped but
        // the item loaded and seeked, so play() later resumes from the right place.
        player.setMediaItems(items, index, startPositionMs.coerceAtLeast(0L))
        player.prepare()
    }

    override fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    override fun next() {
        controller?.seekToNextMediaItem()
    }

    override fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    override fun setRepeat(mode: RepeatMode) {
        controller?.repeatMode = mode.toPlayerRepeatMode()
    }

    override fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed.coerceIn(MIN_SPEED, MAX_SPEED))
    }

    /**
     * How loud the music plays.
     *
     * The player inside the service owns its own volume for the sleep timer's fade and
     * for ducking under a notification, so the level a listener sets is moved on the
     * music stream instead. The two then multiply rather than overwrite each other: a
     * fade still ends in silence rather than in the level the listener chose, and the
     * level the listener chose survives the next notification.
     */
    override fun setVolume(volume: Float) {
        val manager = audioManager ?: return
        val level = volume.coerceIn(0f, 1f)
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        runCatching {
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, (level * max).roundToInt(), 0)
        }
    }

    /**
     * The music stream's level, as a fraction of the loudest it will go.
     *
     * Read from the device rather than remembered, so a change made with the volume
     * keys is reflected by the slider the next time the player is opened.
     */
    private fun streamVolume(): Float {
        val manager = audioManager ?: return 1f
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (manager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max)
            .coerceIn(0f, 1f)
    }

    override fun playNext(track: Track) {
        val player = controller ?: return
        // Straight after whatever is playing, so it survives the current track ending
        // rather than landing at the end of an album the user is not going to reach.
        val at = (player.currentMediaItemIndex + 1).coerceIn(0, player.mediaItemCount)
        player.addMediaItem(at, track.toMediaItem())
    }

    override fun addToQueue(track: Track) {
        controller?.addMediaItem(track.toMediaItem())
    }

    override fun jumpToQueueItem(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.seekTo(index, 0L)
        player.play()
    }

    override fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        val last = player.mediaItemCount - 1
        if (fromIndex !in 0..last || toIndex !in 0..last || fromIndex == toIndex) return
        player.moveMediaItem(fromIndex, toIndex)
    }

    override fun removeFromQueue(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.removeMediaItem(index)
    }

    override fun clearUpcoming() {
        val player = controller ?: return
        val current = player.currentMediaItemIndex
        if (current < 0) return

        val from = current + 1
        val count = player.mediaItemCount - from
        if (count > 0) player.removeMediaItems(from, from + count)
    }

    /**
     * Media3 only reports position changes to listeners when something else happens,
     * so the progress bar needs its own heartbeat.
     */
    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                controller?.let(::publish)
                delay(TICK_MS)
            }
        }
    }

    private fun publish(player: Player) {
        if (queueDirty) {
            cachedQueue = (0 until player.mediaItemCount).map { queueItemAt(player, it) }
            queueDirty = false
        }

        val item = player.currentMediaItem
        val metadata = item?.mediaMetadata
        val duration = player.duration

        _state.value = PlaybackState(
            connected = true,
            current = item?.let {
                QueueItem(
                    mediaId = it.mediaId,
                    title = metadata?.title?.toString()?.takeIf(String::isNotBlank) ?: UNKNOWN_TITLE,
                    artist = metadata?.artist?.toString(),
                    album = metadata?.albumTitle?.toString(),
                )
            },
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = if (duration > 0L) duration else 0L,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode.toRepeatMode(),
            speed = player.playbackParameters.speed.coerceIn(MIN_SPEED, MAX_SPEED),
            volume = streamVolume(),
            queue = cachedQueue,
            currentIndex = if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex,
        )
    }

    private fun queueItemAt(player: Player, index: Int): QueueItem {
        val item = player.getMediaItemAt(index)
        val metadata = item.mediaMetadata
        return QueueItem(
            mediaId = item.mediaId,
            title = metadata.title?.toString()?.takeIf(String::isNotBlank) ?: UNKNOWN_TITLE,
            artist = metadata.artist?.toString(),
            album = metadata.albumTitle?.toString(),
        )
    }

    private fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
        // The content URI doubles as the media id. It is unique per row and it is the
        // one field that reliably survives the trip through the media session.
        .setMediaId(contentUri)
        .setUri(contentUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build(),
        )
        .build()

    private companion object {
        const val TICK_MS = 500L
        const val UNKNOWN_TITLE = "Unknown title"

        /** Anything slower is unintelligible; anything faster stops being music. */
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 3f
    }
}

private fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
}

private fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    else -> RepeatMode.OFF
}
