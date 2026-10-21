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
import android.net.Uri
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the real player. Being a [MediaSessionService] is what buys us background
 * playback, the media notification, lock-screen controls and headset buttons.
 *
 * Audio focus is handled here rather than by the player so the "Lower volume on focal
 * loss" setting has somewhere to act: the player can either stop for a notification or
 * ignore it, and neither is ducking. The focus level multiplies with the sleep timer's
 * fade instead of replacing it, so a timer running out while a notification ducks still
 * ends at silence rather than jumping back to full volume.
 *
 * The level of the music is worked out in one place, and everything that has a say in it
 * multiplies together: the sleep timer's fade, the ducking, the ReplayGain of the file
 * and the ramp of a crossfade. None of them can undo another, and the one that is
 * quieter at the moment is the one that is heard.
 */
class MimiPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** Shuts the effect down with the player; it holds an audio session otherwise. */
    private var equalizer: TrackEqualizer? = null

    /**
     * The effects for the track that comes up during a crossfade.
     *
     * The overlap is played by a player of its own, and that player has an audio session
     * of its own, so the chain is built twice.
     */
    private var crossfadeEqualizer: TrackEqualizer? = null

    /** The player that plays the track coming up while the one playing fades down. */
    private var crossfadePlayer: ExoPlayer? = null

    /** Alive only as long as the service, and only for watching the settings. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val audioManager by lazy { getSystemService(AudioManager::class.java) }

    private var focusRequest: AudioFocusRequest? = null
    private var focusHeld = false
    private var focusVolume = 1f
    private var fadeVolume = 1f

    /**
     * The level the ReplayGain tags of the track playing ask for.
     *
     * One, when the file carries no tags or the mode is switched off, which is the same
     * as not touching the level at all.
     */
    private var replayGainVolume = 1f

    /** The same, for the track that is coming up during a crossfade. */
    private var incomingReplayGainVolume = 1f

    /**
     * The ReplayGain level of every track whose tags have been read, by its uri.
     *
     * The tags are read off the disk, so they are read once and kept: a crossfade needs
     * the level of the track that has not started yet.
     */
    private val replayGainScales = mutableMapOf<String, Float>()

    /** How far the two tracks of an overlap have travelled through it. */
    private var crossfadeOutgoing = 1f
    private var crossfadeIncoming = 0f

    /** The overlap itself, so that it can be called off. */
    private var crossfadeJob: Job? = null

    /** True while the session's player is being handed over to the track coming up. */
    private var handoverPending = false

    /** True while a silence is being left between two tracks. */
    private var gapVolume = 1f

    /** True while a focus loss paused the music, so a gain may put it back. */
    private var pausedForFocus = false

    /** The newest settings, read by the focus and headset callbacks. */
    @Volatile
    private var currentSettings = Settings()

    /** The attributes the players are built with, so both speak the same language. */
    private lateinit var playerAttributes: Media3AudioAttributes

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
                    // A headset plug reports 1 for plugged and 0 for pulled out under the
                    // extra named "state"; the platform never published a constant for it.
                    intent.getIntExtra(HEADSET_STATE, -1) == 1

                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ->
                    intent.getIntExtra(BLUETOOTH_PROFILE_STATE, -1) ==
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

        val attributes = Media3AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        this.playerAttributes = attributes

        val player = ExoPlayer.Builder(this)
            // Focus is requested by this service so it can duck instead of stopping.
            .setAudioAttributes(attributes, /* handleAudioFocus = */ false)
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
                        bassBoost = currentSettings.bassBoostStrength,
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
            applyVolumes()
        }

        // "End of track" is decided here because the player is the only thing that knows a
        // song has finished. The next song starts as the change is reported, so it is
        // paused at once rather than allowed to play on for a moment.
        player.addListener(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Only a song that ran to its end counts. Skipping by hand moves to the
                    // next song too, and stopping there would make the timer feel like a
                    // fault rather than a promise. A crossfade hands the songs over by
                    // hand, but the song really did end, so it counts as well.
                    val ended = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                        reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT ||
                        (handoverPending && reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
                    handoverPending = false
                    if (ended && sleepTimer.consumeStopAfterSong()) player.pause()

                    // The new track is played at the level its own tags ask for.
                    refreshReplayGain(player)

                    // A listener who has turned the gapless switch off wants back the
                    // silence a device would otherwise close.
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                        !currentSettings.gaplessPlayback
                    ) {
                        leaveGap()
                    }
                }
            },
        )

        // These settings steer the player itself, and the player lives here rather than in
        // the app's ViewModel, so they are applied where it is. Collecting rather than
        // reading once means changing one takes effect without the app having to reach
        // across and tell the service.
        scope.launch {
            (application as MimiApplication).settings.settings.collect { settings ->
                val previous = currentSettings
                currentSettings = settings
                player.setHandleAudioBecomingNoisy(settings.pauseOnDisconnect)
                player.setSkipSilenceEnabled(settings.skipSilence)
                sleepTimer.fade = settings.fadeOutSleepTimer
                equalizer.apply(
                    sessionId = player.audioSessionId,
                    enabled = settings.equalizerEnabled,
                    curve = settings.equalizerProfile,
                    bassBoost = settings.bassBoostStrength,
                )
                crossfadeEqualizer?.apply(
                    sessionId = crossfadePlayer?.audioSessionId ?: 0,
                    enabled = settings.equalizerEnabled,
                    curve = settings.equalizerProfile,
                    bassBoost = settings.bassBoostStrength,
                )

                // An overlap that is running while the switch is turned off is called off.
                if (settings.crossfadeMs <= 0) stopCrossfade()

                // The mode or the preamp changed, so the level is worked out again.
                if (previous.replayGainMode != settings.replayGainMode ||
                    previous.replayGainPreampDb != settings.replayGainPreampDb
                ) {
                    refreshReplayGain(player)
                }
            }
        }

        // The overlap is begun by watching how much of the track is left, which is
        // something only the player knows.
        scope.launch {
            while (isActive) {
                delay(CROSSFADE_TICK_MS)
                updateCrossfade()
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
                applyVolumes()
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
                applyVolumes()
            } else {
                pausedForFocus = player.isPlaying
                player.pause()
            }
        }
    }

    /**
     * The level of every player, from every source that has a say in it.
     *
     * The sleep timer's fade, the ducking under a notification, the ReplayGain of the
     * file and the ramp of a crossfade all multiply together, so none of them can undo
     * another. The result is never louder than the loudest.
     */
    private fun applyVolumes() {
        val player = mediaSession?.player ?: return
        player.volume = (baseVolume(replayGainVolume) * crossfadeOutgoing).coerceIn(0f, 1f)
        crossfadePlayer?.let { incoming ->
            incoming.volume =
                (baseVolume(incomingReplayGainVolume) * crossfadeIncoming).coerceIn(0f, 1f)
        }
    }

    /**
     * The level the music plays at before the ramp of a crossfade is applied.
     */
    private fun baseVolume(scale: Float): Float =
        (fadeVolume * focusVolume * gapVolume * scale).coerceIn(0f, 1f)

    /**
     * The ReplayGain level of a track, when its tags have been read.
     */
    private fun replayGainScaleOf(uri: String?): Float =
        uri?.let { replayGainScales[it] } ?: 1f

    /**
     * Reads the ReplayGain tags of the track playing and of the one after it.
     *
     * The tags are read off the disk, so they are read on a thread of their own and the
     * level is only changed once they are in. The track coming up is read as well,
     * because a crossfade needs its level before the overlap begins.
     */
    private fun refreshReplayGain(player: ExoPlayer) {
        val mode = ReplayGainMode.fromSetting(currentSettings.replayGainMode)
        if (mode == ReplayGainMode.NONE) {
            replayGainVolume = 1f
            incomingReplayGainVolume = 1f
            applyVolumes()
            return
        }

        val index = player.currentMediaItemIndex
        if (index < 0 || index >= player.mediaItemCount) return

        val preamp = currentSettings.replayGainPreampDb
        val current = player.getMediaItemAt(index).localConfiguration?.uri
        val next = if (index + 1 < player.mediaItemCount) {
            player.getMediaItemAt(index + 1).localConfiguration?.uri
        } else {
            null
        }

        scope.launch {
            // What is already known is used at once, so a track that has been read
            // before keeps its level while the tags are read again.
            applyReplayGainLevels(player)

            val scales = HashMap<String, Float>()
            current?.let { uri -> scales[uri.toString()] = readReplayGain(uri, mode, preamp) }
            next?.let { uri -> scales[uri.toString()] = readReplayGain(uri, mode, preamp) }

            replayGainScales.putAll(scales)
            applyReplayGainLevels(player)
        }
    }

    /**
     * The level of the track playing and of the one coming up, from what is known.
     */
    private fun applyReplayGainLevels(player: ExoPlayer) {
        replayGainVolume = replayGainScaleOf(player.currentMediaItem?.localConfiguration?.uri?.toString())
        incomingReplayGainVolume = replayGainScaleOf(
            crossfadePlayer?.currentMediaItem?.localConfiguration?.uri?.toString(),
        )
        applyVolumes()
    }

    /**
     * The level one file asks for, read from the tags it carries.
     */
    private suspend fun readReplayGain(uri: Uri, mode: ReplayGainMode, preamp: Float): Float =
        withContext(Dispatchers.IO) {
            val tags = ReplayGainReader.read(this@MimiPlaybackService, uri)
            replayGainScale(tags, mode, preamp)
        }

    /**
     * Leaves the silence that a gapless device would close.
     *
     * The silence is made by taking the level away for a moment rather than by stopping
     * the music, so the notification does not flicker and the focus is not disturbed. It
     * goes through the same level the fade and the ducking use, so it cannot undo them.
     */
    private fun leaveGap() {
        scope.launch {
            gapVolume = 0f
            applyVolumes()
            delay(GAPLESS_GAP_MS)
            gapVolume = 1f
            applyVolumes()
        }
    }

    /**
     * Starts the overlap when the track that is playing is nearly at its end.
     *
     * Nothing happens when the crossfade is switched off, when there is no track after
     * this one, or when the sleep timer's fade or the ducking is doing its own work: the
     * level belongs to them then, and the overlap waits rather than arguing.
     */
    private fun updateCrossfade() {
        val player = mediaSession?.player ?: return
        val length = currentSettings.crossfadeMs
        if (length <= 0) {
            stopCrossfade()
            return
        }
        if (crossfadeJob?.isActive == true) return
        if (!player.isPlaying) return

        // The fade and the ducking own the level while they are at work.
        if (fadeVolume < 1f || focusVolume < 1f) return

        val index = player.currentMediaItemIndex
        if (index < 0 || index + 1 >= player.mediaItemCount) return

        val duration = player.duration
        if (duration <= 0L) return

        val remaining = duration - player.currentPosition
        if (remaining > length + CROSSFADE_MARGIN_MS) return
        // A track shorter than the overlap would be cut off in the middle of the fade,
        // so it is left to end on its own.
        if (remaining < length) return

        startCrossfade(player, index, length)
    }

    /**
     * Brings the track that is ending down and the one coming up over the top of it.
     *
     * The two are played at the same time, so the overlap needs a player of its own: one
     * player plays one item at a time, and the session's player is the one the
     * notification and the lock screen follow.
     */
    private fun startCrossfade(player: ExoPlayer, index: Int, length: Int) {
        val item = player.getMediaItemAt(index + 1)
        val incoming = crossfadePlayer ?: buildCrossfadePlayer().also { crossfadePlayer = it }

        incoming.setMediaItem(item)
        incoming.prepare()
        incomingReplayGainVolume = replayGainScaleOf(item.localConfiguration?.uri?.toString())
        crossfadeOutgoing = 1f
        crossfadeIncoming = 0f
        incoming.volume = 0f
        incoming.play()

        crossfadeJob = scope.launch {
            val steps = (length / CROSSFADE_STEP_MS).coerceIn(8L, 400L).toInt()
            for (step in 1..steps) {
                val levels = crossfadeLevels(step / steps.toFloat())
                crossfadeOutgoing = levels.first
                crossfadeIncoming = levels.second
                applyVolumes()
                delay(CROSSFADE_STEP_MS)
            }
            handOver(player, index, incoming)
        }
    }

    /**
     * Hands the track coming up over to the session's player.
     *
     * The session's player takes the track at the position the other one has reached, so
     * the music does not start again from the top, and it is brought back up over a
     * moment so that the handover is not a click.
     */
    private suspend fun handOver(player: ExoPlayer, index: Int, incoming: ExoPlayer) {
        val position = incoming.currentPosition
        incoming.pause()
        crossfadeOutgoing = 0f
        crossfadeIncoming = 0f
        applyVolumes()

        handoverPending = true
        player.seekTo(index + 1, position)
        player.play()

        for (step in 1..CROSSFADE_FADE_IN_STEPS) {
            crossfadeOutgoing = step / CROSSFADE_FADE_IN_STEPS.toFloat()
            applyVolumes()
            delay(CROSSFADE_FADE_IN_MS / CROSSFADE_FADE_IN_STEPS)
        }
        crossfadeOutgoing = 1f
        applyVolumes()
    }

    /**
     * Calls the overlap off, leaving the track that is playing where it is.
     */
    private fun stopCrossfade() {
        crossfadeJob?.cancel()
        crossfadeJob = null
        crossfadeOutgoing = 1f
        crossfadeIncoming = 0f
        crossfadePlayer?.pause()
        applyVolumes()
    }

    /**
     * The player that plays the track coming up.
     *
     * It is a player of its own because a player plays one item at a time, and an overlap
     * needs two at once. It is not part of the media session: the session keeps the track
     * that is playing, which is what the notification and the lock screen show.
     */
    private fun buildCrossfadePlayer(): ExoPlayer {
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(playerAttributes, /* handleAudioFocus = */ false)
            .build()

        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    // The effects belong to an audio session, and this player has one of
                    // its own.
                    crossfadeEqualizer?.apply(
                        sessionId = player.audioSessionId,
                        enabled = currentSettings.equalizerEnabled,
                        curve = currentSettings.equalizerProfile,
                        bassBoost = currentSettings.bassBoostStrength,
                    )
                }
            },
        )

        val effects = TrackEqualizer()
        crossfadeEqualizer = effects
        return player
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
        crossfadeEqualizer?.release()
        crossfadeEqualizer = null
        crossfadePlayer?.release()
        crossfadePlayer = null
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

        /**
         * The state extra carried by a profile connection broadcast.
         *
         * The platform names this in `BluetoothProfile`, but the constant is not part of
         * the public SDK this build compiles against. The wire value is stable and is the
         * one the system actually sends.
         */
        const val BLUETOOTH_PROFILE_STATE = "android.bluetooth.profile.extra.STATE"

        /** The extra a wired-headset plug carries, named but not exported by `Intent`. */
        const val HEADSET_STATE = "state"

        /** How often the overlap is looked for. */
        const val CROSSFADE_TICK_MS = 100L

        /** How often the level moves while the two tracks are on top of each other. */
        const val CROSSFADE_STEP_MS = 50L

        /** How long the track coming up is brought back up after the handover. */
        const val CROSSFADE_FADE_IN_MS = 150L

        /** How many steps that bringing back up is done in. */
        const val CROSSFADE_FADE_IN_STEPS = 5
    }
}
