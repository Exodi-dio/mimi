package com.exodidio.mimi.library

/**
 * The range of song lengths the library will show.
 *
 * The old screen had one minimum and five choices, which answered only half the question:
 * a collection full of hour-long recordings wants an upper bound as much as a collection
 * full of ringtones wants a lower one. Both ends are stored in milliseconds, and zero means
 * "no bound at that end", so the default of two zeroes is exactly the old "any length".
 *
 * The slider moves in stops rather than in milliseconds because nobody wants to drag to
 * 1:23. Every stored value is still a real length, which is what lets a value written by
 * the old screen survive as it was written.
 */

/**
 * The lengths the two thumbs snap to, in order: off, a minute, five minutes, an hour.
 *
 * The last stop on the slider is not in this list because it is not a length: pulling the
 * upper thumb all the way to the right means "no upper limit", which is stored as zero.
 */
val DurationRangeStopsMs: List<Long> = listOf(0L, 60_000L, 300_000L, 3_600_000L)

/** The stop that means "no upper limit" when the upper thumb is dragged to the end. */
const val DurationRangeUnlimitedStop: Int = 4

/** The furthest right the lower thumb may go, which is an hour rather than infinity. */
private const val LAST_MIN_STOP = 3

/** Whether a song of [durationMs] survives a library bounded by [minMs] and [maxMs]. */
fun isWithinDurationRange(durationMs: Long, minMs: Long, maxMs: Long): Boolean {
    if (minMs > 0L && durationMs < minMs) return false
    if (maxMs > 0L && durationMs > maxMs) return false
    return true
}

/** The slider position of the stored lower bound. */
fun durationMinStop(minMs: Long): Int = nearestDurationStop(minMs).coerceIn(0, LAST_MIN_STOP)

/** The slider position of the stored upper bound, or the unlimited stop when there is none. */
fun durationMaxStop(maxMs: Long): Int =
    if (maxMs <= 0L) DurationRangeUnlimitedStop else nearestDurationStop(maxMs).coerceIn(1, LAST_MIN_STOP)

/** The lower bound a slider position stands for. */
fun durationMinMsFor(stop: Int): Long = DurationRangeStopsMs[stop.coerceIn(0, LAST_MIN_STOP)]

/** The upper bound a slider position stands for; zero is "no upper limit". */
fun durationMaxMsFor(stop: Int): Long =
    if (stop >= DurationRangeUnlimitedStop) 0L else DurationRangeStopsMs[stop.coerceIn(1, LAST_MIN_STOP)]

/**
 * The live value the row shows, in the language of the ticks rather than in milliseconds.
 *
 * A value that is not on a stop still reads as itself, so a collection migrated from the
 * old "over five seconds" setting says five seconds instead of quietly claiming to be off.
 */
fun durationRangeLabel(minMs: Long, maxMs: Long): String = when {
    minMs <= 0L && maxMs <= 0L -> "off"
    maxMs <= 0L -> "${shortDurationLabel(minMs)} \u2013 \u221e"
    minMs <= 0L -> "up to ${shortDurationLabel(maxMs)}"
    else -> "${shortDurationLabel(minMs)} \u2013 ${shortDurationLabel(maxMs)}"
}

/** "45s", "5m" or "1h": the shortest reading that is still exact for a stop. */
fun shortDurationLabel(millis: Long): String = when {
    millis <= 0L -> "0s"
    millis % 3_600_000L == 0L -> "${millis / 3_600_000L}h"
    millis % 60_000L == 0L -> "${millis / 60_000L}m"
    millis % 1_000L == 0L -> "${millis / 1_000L}s"
    else -> "${millis / 1_000L}s"
}

/** The stop closest to [millis], so a value between two stops still lands on the nearer one. */
private fun nearestDurationStop(millis: Long): Int {
    var best = 0
    var bestDistance = Long.MAX_VALUE
    DurationRangeStopsMs.forEachIndexed { index, stop ->
        val distance = kotlin.math.abs(stop - millis)
        if (distance < bestDistance) {
            best = index
            bestDistance = distance
        }
    }
    return best
}
