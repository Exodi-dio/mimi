package com.exodidio.mimi.library

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Notices when audio files appear or disappear on the device.
 *
 * Watching the media store rather than the clock is what makes a phone that was just
 * plugged into a computer find its new music without being told. It is also the cheap way
 * to do it: nothing runs while nothing changes, so a device that is never touched costs
 * nothing at all.
 *
 * Notifications arrive as a burst — a copy of thirty albums is thirty or more of them —
 * so they are collapsed to one callback once the noise stops. A rescan is far more
 * expensive than waiting a few seconds for it.
 */
class LibraryChangeObserver(
    private val context: Context,
    private val onChange: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var pending: Job? = null
    private var registered = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            schedule()
        }
    }

    fun start() {
        if (registered) return
        registered = true
        // A provider that refuses to register is not worth crashing over: the library is
        // still scanned when the app opens, this only adds to that.
        runCatching {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                /* notifyForDescendants = */ true,
                observer,
            )
        }
    }

    fun stop() {
        if (!registered) return
        registered = false
        runCatching { context.contentResolver.unregisterContentObserver(observer) }
        pending?.cancel()
        pending = null
    }

    fun close() {
        stop()
        scope.cancel()
    }

    private fun schedule() {
        pending?.cancel()
        pending = scope.launch {
            delay(DEBOUNCE_MS)
            onChange()
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 4_000L
    }
}
