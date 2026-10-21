package com.exodidio.mimi.ui

import com.exodidio.mimi.library.SongFormat
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

/**
 * The line under the scrubber: what the file says about itself.
 *
 * The parts come in the order a person would say them — the format, then how good the
 * file is, then how often it plays — and anything the device does not know is left out
 * rather than guessed at. A file nothing is known about has no line at all, because a
 * row of empty separators says less than nothing.
 */
fun songFormatLine(format: SongFormat?): String? {
    if (format == null) return null

    val parts = listOfNotNull(
        formatName(format.mimeType),
        formatBitrate(format.bitrateBps),
        formatSampleRate(format.sampleRateHz),
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" \u00b7 ")
}

/**
 * The name of a file's format, from the type the device gives it.
 *
 * The provider names types rather than formats, so the mapping is written out: a file
 * the table does not know is left out of the line entirely rather than shown under a
 * name that means nothing to the listener.
 */
private fun formatName(mimeType: String?): String? {
    val type = mimeType?.trim()?.lowercase() ?: return null
    return formatNames[type]
}

/**
 * The bitrate, in the units it is usually quoted in.
 *
 * The device counts bits per second and people quote kilobits per second, so the number
 * is divided rather than relabelled. A bitrate of zero is not a bitrate.
 */
private fun formatBitrate(bitrateBps: Int?): String? {
    val bitrate = bitrateBps ?: return null
    if (bitrate <= 0) return null
    return "${(bitrate / 1000.0).roundToInt()} kb/s"
}

/**
 * The sample rate, in the units it is usually quoted in.
 *
 * Forty-four thousand one hundred hertz is read as 44.1 kHz, and a rate that lands on a
 * whole number of kilohertz is written without the decimal, because "48 kHz" is how it
 * is said and "48.0 kHz" only adds a digit.
 */
private fun formatSampleRate(sampleRateHz: Int?): String? {
    val sampleRate = sampleRateHz ?: return null
    if (sampleRate <= 0) return null

    val kilohertz = sampleRate / 1000.0
    val text = if (kilohertz % 1.0 == 0.0) {
        kilohertz.toInt().toString()
    } else {
        kilohertz.toString()
    }
    return "$text kHz"
}

/**
 * The formats the provider names, and what each of them is called.
 *
 * The same file can be named several ways by different devices, which is why the table
 * holds the aliases as well as the plain names.
 */
private val formatNames: Map<String, String> = mapOf(
    "audio/mpeg" to "MP3",
    "audio/mp3" to "MP3",
    "audio/mpeg3" to "MP3",
    "audio/flac" to "FLAC",
    "audio/x-flac" to "FLAC",
    "audio/mp4" to "M4A",
    "audio/m4a" to "M4A",
    "audio/x-m4a" to "M4A",
    "audio/aac" to "AAC",
    "audio/ogg" to "OGG",
    "audio/opus" to "OPUS",
    "audio/x-wav" to "WAV",
    "audio/wav" to "WAV",
    "audio/vnd.wave" to "WAV",
    "audio/amr" to "AMR",
    "audio/3gpp" to "3GP",
    "audio/x-ms-wma" to "WMA",
    "audio/webm" to "WEBM",
)
