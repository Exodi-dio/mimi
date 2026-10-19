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

/**
 * Formats a file size the way a person reads one: one decimal place, and the largest unit
 * that does not make the number less than one.
 *
 * Kept in decimal units, which is what storage sizes are quoted in, so a file the phone
 * calls 4.2 MB reads here as 4.2 MB rather than as 4.0 MiB.
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < 1_000L) return "$bytes B"

    val units = listOf("kB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1_000.0
    var unit = 0
    // 999.5 rather than 1000: a size that would round up into the next unit is better
    // shown as that unit than as "1000 kB".
    while (value >= 999.5 && unit < units.lastIndex) {
        value /= 1_000.0
        unit++
    }

    // One decimal place, except where that would print a needless ".0" on a whole
    // number — sizes are read at a glance, not measured.
    val rounded = Math.round(value * 10.0) / 10.0
    val text = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    return "$text ${units[unit]}"
}
