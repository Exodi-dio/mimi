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
 */
class SleepTimer(private val scope: CoroutineScope) {

    /** What is left of the timer, or null when it is not running. */
    private val _remaining = MutableStateFlow<Long?>(null)
    val remaining: StateFlow<Long?> = _remaining.asStateFlow()

    private var job: Job? = null

    /**
     * Run once the timer reaches zero.
     *
     * The player puts its own pause here: this class counts, and the owner decides what
     * stopping means.
     */
    var onElapsed: (() -> Unit)? = null

    fun start(durationMs: Long) {
        if (durationMs <= 0L) {
            cancel()
            return
        }

        job?.cancel()
        _remaining.value = durationMs
        job = scope.launch {
            var left = durationMs
            while (left > 0L) {
                delay(TICK_MS)
                left -= TICK_MS
                _remaining.value = left.coerceAtLeast(0L)
            }
            _remaining.value = null
            job = null
            onElapsed?.invoke()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remaining.value = null
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}
