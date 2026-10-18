package com.exodidio.mimi.ui

/**
 * Formats a duration for display. Kept as a plain function (and unit tested) so
 * it can be reused by the list, the player and the queue later.
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')

    return if (hours > 0) "$hours:$mm:$ss" else "$minutes:$ss"
}
