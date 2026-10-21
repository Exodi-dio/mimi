package com.exodidio.mimi.playback

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
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
 *
 * Audio focus is handled here rather than by the player so the "Lower volume on focal
 * loss" setting has somewhere to act: the player can either stop for a notification or
 * ignore it, and neither is ducking. The focus level multiplies with the sleep timer's
 * fade instead of replacing it, so a timer running out while a notification ducks still
 * ends at silence rather than jumping back to full volume.
 */
class MimiPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** Shuts the effect down with the player; it holds an audio session otherwise. */
    private var equalizer: TrackEqualizer? = null

    /** Alive only as long as the service, and only for watching the settings. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val audioManager by lazy { getSystemService(AudioManager::class.java) }

    private var focusRequest: AudioFocusRequest? = null
    private var focusHeld = false
    private var focusVolume = 1f
    private var fadeVolume = 1f

    /** True while a focus loss paused the music, so a gain may put it back. */
    private var pausedForFocus = false

    /** The newest settings, read by the focus and headset callbacks. */
    @Volatile
    private var currentSettings = Settings()

    /**
     * Starts playback when a wired or bluetooth headset arrives.
     *
     * Both are protected broadcasts, so no permission is added. A headset that connects
     * while nothing is loaded simply does nothing: there is no queue to start.
     */
    private val autoPlayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!currentSettings.autoPlay) return
            val connected = when (intent?.action) {
                Intent.ACTION_HEADSET_PLUG ->
                    // A headset plug reports 1 for plugged and 0 for pulled out; the
                    // platform never published a named constant for the 1.
                    intent.getIntExtra(Intent.EXTRA_STATE, -1) == 1

                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ->
                    intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) ==
                        BluetoothProfile.STATE_CONNECTED

                else -> false
            }
            if (!connected) return

            val player = mediaSession?.player ?: return
            if (player.mediaItemCount > 0 && !player.isPlaying && !player.playWhenReady) {
                player.play()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Two attribute objects on purpose: the player speaks Media3's type, while the
        // focus request is a platform one. They describe the same stream, and building each
        // from its own builder is what keeps that from being a cast between the two.
        val platformAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val playerAttributes = Media3AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            // Focus is requested by this service so it can duck instead of stopping.
            .setAudioAttributes(playerAttributes, /* handleAudioFocus = */ false)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()

        // The equaliser binds to an audio session, and the player only makes one once it has
        // something to play, so it is applied on every change of playing state as well as on
        // every change of settings. Applying is idempotent, so saying it twice costs nothing.
        val equalizer = TrackEqualizer()
        this.equalizer = equalizer

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(platformAttributes)
            // Ducking is a volume change, not a pause, so the platform is told not to pause
            // the stream while another app makes a short sound.
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { change -> onFocusChange(player, change) }
            .build()
        focusRequest = request

        player.addListener(
            object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (playWhenReady) requestFocus()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    equalizer.apply(
                        sessionId = player.audioSessionId,
                        enabled = currentSettings.equalizerEnabled,
                        curve = currentSettings.equalizerProfile,
                    )
                    if (isPlaying) requestFocus()
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
        sleepTimer.onFade = { level ->
            fadeVolume = level
            applyVolume(player)
        }

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

        // These settings steer the player itself, and the player lives here rather than in
        // the app's ViewModel, so they are applied where it is. Collecting rather than
        // reading once means changing one takes effect without the app having to reach
        // across and tell the service.
        scope.launch {
            (application as MimiApplication).settings.settings.collect { settings ->
                currentSettings = settings
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

        val headsetFilter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            autoPlayReceiver,
            headsetFilter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    /** Asks for focus once, while there is something playing or about to play. */
    private fun requestFocus() {
        if (focusHeld) return
        val request = focusRequest ?: return
        if (audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            focusHeld = true
        }
    }

    /**
     * Answers a change of focus with the behaviour the setting names.
     *
     * A notification is a transient loss, so it ducks when the setting is on and pauses
     * when it is off. A real loss — another app starting its own playback — pauses either
     * way and remembers that it did, so the music can come back when focus does.
     */
    private fun onFocusChange(player: ExoPlayer, change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                focusVolume = 1f
                applyVolume(player)
                if (pausedForFocus) {
                    pausedForFocus = false
                    player.play()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                focusHeld = false
                pausedForFocus = player.isPlaying
                player.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (currentSettings.lowerVolumeOnFocusLoss) {
                focusVolume = DUCKED_VOLUME
                applyVolume(player)
            } else {
                pausedForFocus = player.isPlaying
                player.pause()
            }
        }
    }

    /** The player's volume is the fade and the focus level together, never either alone. */
    private fun applyVolume(player: ExoPlayer) {
        player.volume = (fadeVolume * focusVolume).coerceIn(0f, 1f)
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
        runCatching { unregisterReceiver(autoPlayReceiver) }
        focusRequest?.let { request -> audioManager.abandonAudioFocusRequest(request) }
        focusRequest = null
        equalizer?.release()
        equalizer = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        /** Quiet enough to hear a notification over, loud enough to know the song is there. */
        const val DUCKED_VOLUME = 0.22f
    }
}
