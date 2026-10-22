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
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.Callback
import androidx.media3.session.MediaSession
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.browse.BrowseSong
import com.exodidio.mimi.browse.LibraryIndex
import com.exodidio.mimi.browse.toMediaItem
import com.exodidio.mimi.library.Settings
import com.exodidio.mimi.widgets.NowPlayingStore
import com.exodidio.mimi.widgets.WidgetSong
import com.exodidio.mimi.widgets.refreshNowPlayingWidget
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
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
 * Owns the real player. Being a [MediaLibraryService] is what buys us background
 * playback, the media notification, lock-screen controls and headset buttons, and it is
 * also what lets a car or another app walk the library: the browse tree hangs off the
 * very session the app itself plays through, so there is one player and one
 * notification whichever of them the music came from.
 *
 * Audio focus is handled here rather than by the player so the "Lower volume on focal
 * loss" setting has somewhere to act: the player can either stop for a notification or
 * ignore it, and neither is ducking. The focus level multiplies with the sleep timer's
 * fade instead of replacing it, so a timer running out while a notification ducks still
 * ends at silence rather than jumping back to full volume.
 *
 * The level of the music is worked out in one place, and everything that has a say in it
 * multiplies together: the sleep timer's fade, the ducking, the ramp of a crossfade, the
 * ramp of a play or pause, and the level the file's own tags ask for. None of them can
 * undo another, and the one that is quieter at the moment is the one that is heard.
 *
 * A play and a pause are slowed down here rather than in the player, because a pause the
 * player has already carried out is a pause whose last syllable has already been cut off.
 * The session talks to a player that takes the level down before it pauses, and brings it
 * up after it plays, so a controller, a notification or a headset all get the same fade.
 */
class MimiPlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null

    /**
     * The player the session wraps.
     *
     * Kept beside the session because the fade has to reach the real player to begin and
     * to stop playing, while everything a controller asks for goes through the wrapper.
     */
    private var rawPlayer: ExoPlayer? = null

    /**
     * How loud the play and pause ramp says the music is, from nothing to full.
     *
     * One unless a ramp is running, which is the same as not touching the level at all.
     */
    private var playPauseVolume = 1f

    /** The level the ramp in flight is heading for, so a reversal knows which way it went. */
    private var playPauseTarget = 1f

    /** The ramp in flight, so that a second tap can call it off. */
    private var playPauseRamp: Job? = null

    /**
     * True when a play has been started but the music has not begun to sound yet.
     *
     * The ramp up belongs where the sound starts, not where the command was given: a
     * file that takes a moment to buffer would otherwise finish its fade in silence and
     * arrive at full volume with a click, which is the one thing the fade is for.
     */
    private var resumeRampPending = false

    /**
     * The library as a browser sees it.
     *
     * Held in memory rather than read from the database on every question, because a car
     * walks the same shelves over and over.
     */
    private val libraryIndex by lazy { LibraryIndex(this) }

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
        rawPlayer = player

        // The session is handed a player that fades on the way in and out of a pause.
        // Everything else it asks for is forwarded untouched.
        mediaSession = MediaLibrarySession.Builder(this, FadedPlayer(player), LibraryCallback())
            .build()

        // The library is watched rather than read on demand: a browser asks for the
        // shelves from a binder, and the answer cannot wait for a database read.
        libraryIndex.start(scope)

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
                    if (isPlaying) {
                        requestFocus()
                        startResumeRamp()
                    }
                    publishNowPlaying()
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

                    // The widget shows what is playing, so it has to be told when the
                    // song changes rather than when the launcher next asks.
                    publishNowPlaying()
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
     * The sleep timer's fade, the ducking under a notification, the silence of a gap,
     * the ramp of a play or pause, the ReplayGain of the file and the ramp of a crossfade
     * all multiply together, so none of them can undo another. The result is never louder
     * than the loudest.
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
     *
     * The trim of the bass boost is taken off here, which is where a preamp belongs: the
     * player's volume sits in front of the effects, so the boost has less to work with
     * and the chain cannot be pushed into clipping.
     */
    private fun baseVolume(scale: Float): Float =
        (fadeVolume * focusVolume * gapVolume * playPauseVolume * scale * bassTrim())
            .coerceIn(0f, 1f)

    /**
     * The level the bass boost leaves behind, as a fraction of the level it started with.
     *
     * One when the boost is off, so nothing is taken off in that case.
     */
    private fun bassTrim(): Float = bassBoostTrimScale(currentSettings.bassBoostStrength)

    /**
     * Starts playing, bringing the level up rather than letting it arrive at full.
     *
     * The player is started after the level has been taken to nothing, so the first
     * sample is already quiet: setting the volume afterwards would let a click through.
     * A fade of zero, or a level that already belongs to someone else, keeps the old
     * instant behaviour rather than starting a ramp that would fight it.
     */
    private fun requestPlay() {
        val player = rawPlayer ?: return

        if (player.playWhenReady) {
            // A pause that is still ramping down is called back here. The sound never
            // stopped, so the level is brought back up instead of the pause landing.
            if (playPauseRamp?.isActive == true && playPauseTarget < 1f) {
                if (levelBelongsToAnotherRamp()) {
                    endPlayPauseRamp()
                } else {
                    rampTo(1f, currentSettings.fadeOnPlayPauseMs, null)
                }
            }
            return
        }

        val duration = currentSettings.fadeOnPlayPauseMs
        if (duration <= 0 || levelBelongsToAnotherRamp()) {
            endPlayPauseRamp()
            player.play()
            return
        }

        playPauseVolume = 0f
        applyVolumes()
        // The ramp itself starts when the player reports that it is playing, so a file
        // that buffers for a moment still fades in rather than fading before it is heard.
        resumeRampPending = true
        player.play()
        if (player.isPlaying) startResumeRamp()
    }

    /**
     * Starts the ramp up, if a play is waiting for the music to begin.
     *
     * Called when the player reports that it is playing, and harmless otherwise, so a
     * buffer that stalls and comes back cannot start a second fade.
     */
    private fun startResumeRamp() {
        if (!resumeRampPending) return
        resumeRampPending = false
        if (levelBelongsToAnotherRamp()) {
            playPauseVolume = 1f
            applyVolumes()
            return
        }
        rampTo(1f, currentSettings.fadeOnPlayPauseMs, null)
    }

    /**
     * Pauses, taking the level down first so the last syllable is not cut in half.
     *
     * The player is only paused once the ramp has reached nothing, which is why the
     * transport keeps saying it is playing for the length of the fade. A fade of zero,
     * or a level already being taken down by the sleep timer, the ducking or a
     * crossfade, pauses at once rather than adding a second ramp on top of the first.
     */
    private fun requestPause() {
        val player = rawPlayer ?: return

        if (!player.playWhenReady) {
            endPlayPauseRamp()
            player.pause()
            return
        }

        // A pause while the two tracks overlap ends the overlap: left running, its
        // handover would start the next track by itself a moment after the pause.
        if (crossfadeJob?.isActive == true) {
            stopCrossfade()
            endPlayPauseRamp()
            player.pause()
            return
        }

        // A second pause while the first is already taking the level down finishes the
        // job at once, so tapping twice cannot stretch the fade or leave it half way.
        if (playPauseRamp?.isActive == true && playPauseTarget < 1f) {
            endPlayPauseRamp()
            player.pause()
            return
        }

        val duration = currentSettings.fadeOnPlayPauseMs
        if (duration <= 0 || levelBelongsToAnotherRamp() || playPauseVolume <= 0f) {
            endPlayPauseRamp()
            player.pause()
            return
        }

        rampTo(0f, duration) {
            // The level goes back to one once the player is stopped, so the next play
            // starts from its own ramp rather than from whatever this one reached.
            player.pause()
            playPauseTarget = 1f
            playPauseVolume = 1f
            applyVolumes()
        }
    }

    /**
     * True while the sleep timer, the ducking or a crossfade owns the level.
     *
     * Those three already take the music down on their own, and a play or pause ramp
     * starting beside them would be two answers to one question: the volume would be
     * pulled two ways at once, and whichever finished first would win by accident.
     */
    private fun levelBelongsToAnotherRamp(): Boolean =
        fadeVolume < 1f || focusVolume < 1f || crossfadeJob?.isActive == true

    /**
     * Moves the play and pause level to [target] over [durationMs].
     *
     * The ramp starts from wherever the level is, not from the end it would have reached,
     * so tapping play during a fade out turns the sound round from where it is. The work
     * is done in small steps because the player has no ramp of its own: each step puts
     * one more factor into the level that [applyVolumes] already multiplies.
     */
    private fun rampTo(target: Float, durationMs: Int, then: (() -> Unit)?) {
        playPauseRamp?.cancel()
        playPauseTarget = target
        val ramp = VolumeRamp(durationMs, playPauseVolume, target)
        playPauseRamp = scope.launch {
            val begun = SystemClock.elapsedRealtime()
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - begun
                playPauseVolume = ramp.levelAt(elapsed)
                applyVolumes()
                if (ramp.finished(elapsed)) break
                delay(PLAY_PAUSE_STEP_MS)
            }
            then?.invoke()
        }
    }

    /**
     * Calls a ramp off and puts the level back to one.
     *
     * A ramp that is cancelled on its way to a pause leaves the level part way down, and
     * a player that is then paused by hand would be left at that level for the next play.
     * Putting it back here is what stops a rapid tap from keeping half the volume.
     */
    private fun endPlayPauseRamp() {
        playPauseRamp?.cancel()
        playPauseRamp = null
        playPauseTarget = 1f
        playPauseVolume = 1f
        resumeRampPending = false
        applyVolumes()
    }

    /**
     * The player the session hands to controllers.
     *
     * A pause is not refused here, it is only late: the level is taken down first and the
     * player is paused when it reaches nothing. Everything else, from a seek to a queue
     * change, is forwarded as it always was, because only the two transport commands have
     * a sound to smooth.
     */
    private inner class FadedPlayer(player: ExoPlayer) : ForwardingPlayer(player) {
        override fun play() = requestPlay()

        override fun pause() = requestPause()

        override fun setPlayWhenReady(playWhenReady: Boolean) {
            if (playWhenReady) requestPlay() else requestPause()
        }
    }

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
    private fun refreshReplayGain(player: Player) {
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
    private fun applyReplayGainLevels(player: Player) {
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
    private fun startCrossfade(player: Player, index: Int, length: Int) {
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
    private suspend fun handOver(player: Player, index: Int, incoming: ExoPlayer) {
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    /**
     * Writes down what is playing, and tells the widget about it.
     *
     * A widget is drawn by the system and cannot ask the player anything, so the song is
     * written down as it changes and the widget is drawn from what was written. That is
     * what lets the widget be right when the app is not running at all.
     */
    private fun publishNowPlaying() {
        val player = mediaSession?.player ?: return
        val item = player.currentMediaItem
        val metadata = item?.mediaMetadata

        NowPlayingStore(this).save(
            WidgetSong(
                mediaId = item?.mediaId.orEmpty(),
                title = metadata?.title?.toString()?.takeIf { it.isNotBlank() } ?: "Nothing playing",
                artist = metadata?.artist?.toString(),
                album = metadata?.albumTitle?.toString(),
                durationMs = player.duration,
                playing = player.isPlaying,
            ),
        )
        refreshNowPlayingWidget(this)
    }

    /**
     * The library as a browser is shown it.
     *
     * The tree is answered from the songs the service already holds, so a car walking
     * the shelves costs nothing but a walk. A browser that asks for something to be
     * played is answered with the song it named: the app knows what it wants to play and
     * says so itself, while a browser only knows the one song it tapped and is given
     * the library as the queue, so that next has somewhere to go.
     */
    @OptIn(UnstableApi::class)
    private inner class LibraryCallback : Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
            LibraryResult.ofItem(libraryIndex.browse().root().toMediaItem(), params),
        )

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val entries = libraryIndex.browse().childrenOf(parentId)
            // The page is worked out in longs, because a browser asking for everything
            // asks for a page the size of the whole library.
            val from = (page.toLong() * pageSize).coerceIn(0L, entries.size.toLong()).toInt()
            val items = entries.drop(from).take(pageSize.coerceAtLeast(0)).map { it.toMediaItem() }
            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = libraryIndex.browse().entry(mediaId)?.toMediaItem()
            return if (item == null) {
                Futures.immediateFuture(
                    LibraryResult.ofError<MediaItem>(LibraryResult.RESULT_ERROR_BAD_VALUE),
                )
            } else {
                Futures.immediateFuture(LibraryResult.ofItem(item, null))
            }
        }

        /**
         * Turns what a browser asked for into something the player can start.
         *
         * A browser that names a song hands back an item with nothing in it but the id,
         * because that is all it was given, so the song is looked up in the library and
         * answered with the file behind it.
         */
        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            if (mediaItems.none { it.localConfiguration?.uri == null }) {
                return Futures.immediateFuture(mediaItems)
            }

            val future = SettableFuture.create<List<MediaItem>>()
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { future.set(resolveItems(mediaItems)) }
                } catch (throwable: Throwable) {
                    future.setException(throwable)
                }
            }
            return future
        }

        /**
         * Gives a browser the library as the queue.
         *
         * The app says for itself, so it is left alone: it knows which list it meant to
         * play. A browser only knows the one song it tapped, and is given the library so
         * that the next button walks the songs rather than stopping at the end of one.
         */
        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            if (controller.packageName == packageName) {
                if (mediaItems.none { it.localConfiguration?.uri == null }) {
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            mediaItems,
                            startIndex,
                            startPositionMs,
                        ),
                    )
                }
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            future.set(
                                MediaSession.MediaItemsWithStartPosition(
                                    resolveItems(mediaItems),
                                    startIndex,
                                    startPositionMs,
                                ),
                            )
                        }
                    } catch (throwable: Throwable) {
                        future.setException(throwable)
                    }
                }
                return future
            }

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        if (libraryIndex.size == 0) libraryIndex.refresh()
                        val songs: List<BrowseSong> = libraryIndex.songs()
                        val wanted = mediaItems.firstOrNull()?.mediaId
                        val at = songs.indexOfFirst { it.mediaId == wanted }
                        future.set(
                            MediaSession.MediaItemsWithStartPosition(
                                songs.map { it.toMediaItem() },
                                if (at >= 0) at else 0,
                                0L,
                            ),
                        )
                    }
                } catch (throwable: Throwable) {
                    future.setException(throwable)
                }
            }
            return future
        }

        /**
         * The items a browser asked for, with the songs behind them filled in.
         *
         * An item that already knows where its audio is is left exactly as it is, so the
         * app's own list goes through untouched.
         */
        private suspend fun resolveItems(mediaItems: List<MediaItem>): List<MediaItem> {
            if (libraryIndex.size == 0) libraryIndex.refresh()
            return mediaItems.map { item ->
                if (item.localConfiguration?.uri != null) {
                    item
                } else {
                    libraryIndex.song(item.mediaId)?.toMediaItem() ?: item
                }
            }
        }
    }

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

        /**
         * How often a play or pause ramp moves.
         *
         * Short enough that a fade of 250 ms is not two audible steps, long enough that
         * a fade of a second does not wake the main thread fifty times over.
         */
        const val PLAY_PAUSE_STEP_MS = 20L

        /** How long the track coming up is brought back up after the handover. */
        const val CROSSFADE_FADE_IN_MS = 150L

        /** How many steps that bringing back up is done in. */
        const val CROSSFADE_FADE_IN_STEPS = 5
    }
}
