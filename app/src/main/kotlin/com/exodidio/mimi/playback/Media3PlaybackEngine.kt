package com.exodidio.mimi.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.exodidio.mimi.library.Track
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
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
        publish(connected)
        startTicker()
    }

    override fun release() {
        ticker?.cancel()
        ticker = null
        controller?.removeListener(listener)
        controller?.release()
        controller = null
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
        val item = player.currentMediaItem
        val metadata = item?.mediaMetadata
        val duration = player.duration

        _state.value = PlaybackState(
            connected = true,
            current = item?.let {
                NowPlaying(
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
