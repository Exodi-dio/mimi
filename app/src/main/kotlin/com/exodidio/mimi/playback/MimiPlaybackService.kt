package com.exodidio.mimi.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.library.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns the real player. Being a [MediaSessionService] is what buys us background
 * playback, the media notification, lock-screen controls and headset buttons.
 */
class MimiPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** Shuts the effect down with the player; it holds an audio session otherwise. */
    private var equalizer: TrackEqualizer? = null

    /** Alive only as long as the service, and only for watching the settings. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .build()

        mediaSession = MediaSession.Builder(this, player).build()

        // The equaliser binds to an audio session, and the player only makes one once it has
        // something to play, so it is applied on every change of playing state as well as on
        // every change of settings. Applying is idempotent, so saying it twice costs nothing.
        val equalizer = TrackEqualizer()
        this.equalizer = equalizer

        var current = Settings()
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    equalizer.apply(
                        sessionId = player.audioSessionId,
                        enabled = current.equalizerEnabled,
                        curve = current.equalizerProfile,
                    )
                }
            },
        )

        // The sleep timer counts somewhere that outlives the screen; stopping is something
        // only the player can carry out, so it is handed the job once the player exists.
        val sleepTimer = (application as MimiApplication).sleepTimer
        sleepTimer.onElapsed = {
            if (player.isPlaying) player.pause()
        }

        // Fading is the player's own volume rather than the phone's, so the ramp cannot
        // leave the device quiet for everything else afterwards.
        sleepTimer.onFade = { level -> player.volume = level }

        // "End of track" is decided here because the player is the only thing that knows a
        // song has finished. The next song starts as the change is reported, so it is
        // paused at once rather than allowed to play on for a moment.
        player.addListener(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Only a song that ran to its end counts. Skipping by hand moves to the
                    // next song too, and stopping there would make the timer feel like a
                    // fault rather than a promise.
                    val ended = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                        reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                    if (ended && sleepTimer.consumeStopAfterSong()) player.pause()
                }
            },
        )

        // These two settings steer the player itself, and the player lives here rather than
        // in the app's ViewModel, so they are applied where it is. Collecting rather than
        // reading once means changing either takes effect on the next song at the latest,
        // without the app having to reach across and tell the service.
        scope.launch {
            (application as MimiApplication).settings.settings.collect { settings ->
                current = settings
                player.setHandleAudioBecomingNoisy(settings.pauseOnDisconnect)
                player.setSkipSilenceEnabled(settings.skipSilence)
                sleepTimer.fade = settings.fadeOutSleepTimer
                equalizer.apply(
                    sessionId = player.audioSessionId,
                    enabled = settings.equalizerEnabled,
                    curve = settings.equalizerProfile,
                )
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** Keep playing if the app is swiped away; only shut down when nothing is playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        equalizer?.release()
        equalizer = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
