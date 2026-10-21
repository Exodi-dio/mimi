package com.exodidio.mimi.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stops the music after a while.
 *
 * It is kept beside the player rather than on the screen because the point of a sleep
 * timer is to keep listening with the phone face down: a countdown owned by the screen
 * would die the moment Android reclaimed the activity, which is exactly when it is
 * wanted. Sharing the application gives it a lifetime the screen cannot cut short, and
 * the foreground service keeps that alive while anything is playing.
 *
 * It can wait for a length of time or for the end of the song that is playing. The second
 * is the one people reach for at night: a countdown picked from a list of round numbers
 * cuts the last song in half, and being woken by half a song is worse than not setting a
 * timer at all.
 */
class SleepTimer(private val scope: CoroutineScope) {

    /** What is left of the timer, or null when it is not running. */
    private val _remaining = MutableStateFlow<Long?>(null)
    val remaining: StateFlow<Long?> = _remaining.asStateFlow()

    /** True when the music should stop at the end of the song rather than after a count. */
    private val _stopAfterSong = MutableStateFlow(false)
    val stopAfterSong: StateFlow<Boolean> = _stopAfterSong.asStateFlow()

    private var job: Job? = null

    /**
     * Run once the timer reaches zero.
     *
     * The player puts its own pause here: this class counts, and the owner decides what
     * stopping means.
     */
    var onElapsed: (() -> Unit)? = null

    /**
     * How loud the player should be while the last stretch of the countdown runs.
     *
     * Always called with 1 when the timer is cancelled or reaches zero, so the volume is
     * put back whether the timer ran out, was stopped by hand, or was replaced by another
     * length. The owner decides what to do with it; this class only says how far along the
     * fade is.
     */
    var onFade: ((level: Float) -> Unit)? = null

    /** Whether to fade out over the last stretch. Set from settings; off unless asked. */
    var fade: Boolean = false

    fun start(durationMs: Long) {
        cancel()
        if (durationMs <= 0L) return

        _remaining.value = durationMs
        job = scope.launch {
            var left = durationMs
            while (left > 0L) {
                delay(TICK_MS)
                left -= TICK_MS
                _remaining.value = left.coerceAtLeast(0L)

                // Only the last stretch ramps down, so a half hour timer is not quiet for
                // half an hour. Dropping to zero on the same second the pause lands is
                // what makes the ending sound deliberate rather than like a dead battery.
                if (fade && left < SLEEP_FADE_MS) onFade?.invoke(fadeLevel(left))
            }

            _remaining.value = null
            job = null
            onFade?.invoke(1f)
            onElapsed?.invoke()
        }
    }

    /** Stops the music when the song that is playing finishes. */
    fun stopAfterSong() {
        cancel()
        _stopAfterSong.value = true
    }

    /**
     * True once, when a song ends and the timer was waiting for it.
     *
     * Read and cleared in one step so a player that reports the end of a song more than
     * once cannot pause the song after next as well.
     */
    fun consumeStopAfterSong(): Boolean {
        if (!_stopAfterSong.value) return false
        _stopAfterSong.value = false
        return true
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remaining.value = null
        _stopAfterSong.value = false
        onFade?.invoke(1f)
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}

/** How long the fade takes. */
const val SLEEP_FADE_MS = 15_000L

/**
 * How loud the player should be with [remainingMs] of a [fadeMs] fade left.
 *
 * Linear rather than logarithmic, which is not what a fader in a mixing desk does, but on
 * a phone the last second of a track is already quiet and a linear ramp is the shape that
 * sounds like someone turning the volume down rather than like the sound being taken away.
 *
 * Pure, so the ramp can be checked without a running timer.
 */
fun fadeLevel(remainingMs: Long, fadeMs: Long = SLEEP_FADE_MS): Float {
    if (fadeMs <= 0L) return 0f
    if (remainingMs >= fadeMs) return 1f
    if (remainingMs <= 0L) return 0f
    return remainingMs.toFloat() / fadeMs.toFloat()
}
