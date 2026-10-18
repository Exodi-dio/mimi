package com.exodidio.mimi.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.exodidio.mimi.MimiApplication
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

        // These two settings steer the player itself, and the player lives here rather than
        // in the app's ViewModel, so they are applied where it is. Collecting rather than
        // reading once means changing either takes effect on the next song at the latest,
        // without the app having to reach across and tell the service.
        scope.launch {
            (application as MimiApplication).settings.settings.collect { settings ->
                player.setHandleAudioBecomingNoisy(settings.pauseOnDisconnect)
                player.setSkipSilenceEnabled(settings.skipSilence)
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
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
