package com.exodidio.mimi.widgets

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.exodidio.mimi.MainActivity
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.albumArtUriFor
import com.exodidio.mimi.playback.MimiPlaybackService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** Play the song, or pause it when it is playing. */
const val ACTION_TOGGLE = "com.exodidio.mimi.widget.TOGGLE"

/** Skip to the song after the one that is playing. */
const val ACTION_NEXT = "com.exodidio.mimi.widget.NEXT"

/** Play the song named by the extra. */
const val ACTION_PLAY = "com.exodidio.mimi.widget.PLAY"

/** The song to play, named by its media store uri. */
const val EXTRA_MEDIA_ID = "com.exodidio.mimi.widget.MEDIA_ID"

/**
 * The buttons on the widgets.
 *
 * A button on a widget acts at once rather than opening the app, because a listener
 * tapping next on a home screen means next now. The work is done through the media
 * session, so what comes out is the same music the app would play and the app's own
 * player sees it.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Playing a song means reading the cache, which is a disk read, so the receiver
        // is held open until the work is done.
        val pending = goAsync()
        widgetScope.launch {
            try {
                act(context, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun act(context: Context, intent: Intent) {
        val controller = connect(context) ?: return
        try {
            when (intent.action) {
                ACTION_TOGGLE -> toggle(context, controller)
                ACTION_NEXT -> controller.seekToNextMediaItem()
                ACTION_PLAY -> play(context, controller, intent.getStringExtra(EXTRA_MEDIA_ID))
            }
        } finally {
            controller.release()
        }
    }

    /**
     * Plays the music, or pauses it when it is playing.
     *
     * A widget can be used when the app was never opened, in which case there is nothing
     * loaded: the session the library kept is put back instead, which is what the app
     * itself does when it opens.
     */
    private suspend fun toggle(context: Context, controller: MediaController) {
        if (controller.mediaItemCount > 0) {
            if (controller.isPlaying) controller.pause() else controller.play()
            return
        }

        val saved = library(context).loadPlayback() ?: return
        val tracks = library(context).tracks.first()
        val index = tracks.indexOfFirst { it.contentUri == saved.mediaId }
        if (index < 0) return

        controller.setMediaItems(tracks.map { it.toPlayerItem() }, index, saved.positionMs)
        controller.prepare()
        controller.play()
    }

    /**
     * Plays one song, with the rest of the library behind it.
     *
     * The whole library is the queue rather than the one song, so that the next button
     * walks the songs instead of stopping at the end of one.
     */
    private suspend fun play(context: Context, controller: MediaController, mediaId: String?) {
        val wanted = mediaId?.takeIf { it.isNotBlank() } ?: return
        val tracks = library(context).tracks.first()
        val index = tracks.indexOfFirst { it.contentUri == wanted }
        if (index < 0) return

        controller.setMediaItems(tracks.map { it.toPlayerItem() }, index, 0L)
        controller.prepare()
        controller.play()
    }

    /**
     * The session the buttons act through.
     *
     * The controller is what the app itself plays through, so a button on a widget and
     * the app's own player are one player and cannot fight over the music.
     */
    private suspend fun connect(context: Context): MediaController? {
        val token = SessionToken(context, ComponentName(context, MimiPlaybackService::class.java))
        return try {
            suspendCancellableCoroutine<MediaController> { continuation ->
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
        } catch (_: Throwable) {
            null
        }
    }

    /** The library the songs are read from. */
    private fun library(context: Context) =
        (context.applicationContext as MimiApplication).library
}

/**
 * The player's own name for a song.
 *
 * The uri is in the item as well as being its id, so the session can start it without
 * another round trip, and the name, the artist and the cover are in the metadata so
 * that the notification says what is playing.
 */
private fun Track.toPlayerItem(): MediaItem = MediaItem.Builder()
    .setMediaId(contentUri)
    .setUri(contentUri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUriFor(albumId)?.let { Uri.parse(it) })
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build(),
    )
    .build()

/**
 * The intent a widget's button sends.
 *
 * The receiver is not exported, because a pending intent is sent as the app that made
 * it, so nobody else has to be able to reach it.
 */
fun widgetActionIntent(context: Context, action: String): PendingIntent {
    val intent = Intent(context, WidgetActionReceiver::class.java).setAction(action)
    return PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * The intent that opens the app on the player.
 *
 * The app is asked to open on the player rather than on its home screen because the
 * player is what the widget is showing: the song that is playing.
 */
fun openPlayerIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
