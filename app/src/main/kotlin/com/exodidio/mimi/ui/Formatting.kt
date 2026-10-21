package com.exodidio.mimi.ui

import com.exodidio.mimi.library.Track
import kotlin.math.roundToInt

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

/**
 * Says how long a whole library would take to play, in the units a person would use.
 *
 * A total can run to days, and "73:12:44" is a number nobody converts in their head, so
 * hours are only kept until there are days, and minutes only until there are hours.
 * Anything under a minute reads as seconds, because "0 hours" is a worse answer than "0
 * minutes" to a library that has just been emptied.
 */
fun formatTotalDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "No music yet"

    val totalSeconds = durationMs / 1000
    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3_600
    val minutes = (totalSeconds % 3_600) / 60

    if (days > 0) return plural(days, "day") + if (hours > 0) " ${plural(hours, "hour")}" else ""
    if (hours > 0) return plural(hours, "hour") + if (minutes > 0) " ${plural(minutes, "minute")}" else ""
    if (minutes > 0) return plural(minutes, "minute")
    return plural(totalSeconds, "second")
}

private fun plural(count: Long, word: String): String =
    if (count == 1L) "1 $word" else "$count ${word}s"

/**
 * A running time in the short form a list header uses.
 *
 * The long form in [formatTotalDuration] is right for a statistics page where the number
 * is the point. A header has one line shared with a song count, so the units are shortened
 * and only the two largest are kept: "2 hr 41 min" says what a listener wants at a glance,
 * where "2 hours 41 minutes" pushes the count off the line.
 */
fun formatListDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0 min"

    val totalSeconds = durationMs / 1000
    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    if (days > 0) {
        val dayText = if (days == 1L) "1 day" else "$days days"
        return if (hours > 0) "$dayText $hours hr" else dayText
    }
    if (hours > 0) return if (minutes > 0) "$hours hr $minutes min" else "$hours hr"
    if (minutes > 0) return "$minutes min"
    return "$seconds sec"
}

/** How long a list of songs would take, used by every list header that totals one. */
fun totalDurationMs(tracks: List<Track>): Long =
    tracks.sumOf { it.durationMs.coerceAtLeast(0L) }

/**
 * A change against the previous period, as a signed, rounded percentage.
 *
 * The sign is the point of the number, so a fall carries a minus rather than only being
 * smaller than the rise it is compared with. A change that rounds to nothing reads as
 * "0%" instead of "+0%", because a plus in front of no movement claims a rise.
 */
fun formatSignedPercent(changePercent: Double): String {
    val rounded = changePercent.roundToInt()
    return when {
        rounded > 0 -> "+$rounded%"
        rounded < 0 -> "$rounded%"
        else -> "0%"
    }
}

/**
 * The one line every song list header uses: the count, then the running time.
 *
 * A list with nothing in it says only that. Printing "0 songs · 0 min" reads like a
 * measurement of nothing rather than an empty shelf.
 */
fun songSummaryLabel(count: Int, durationMs: Long): String {
    val songs = songCountLabel(count)
    if (count == 0 || durationMs <= 0L) return songs
    return "$songs \u00b7 ${formatListDuration(durationMs)}"
}
