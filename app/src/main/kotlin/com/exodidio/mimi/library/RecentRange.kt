package com.exodidio.mimi.library

/**
 * How far back the automatic lists reach.
 *
 * "Last added" and "Recently played" are views over the library rather than playlists that
 * are written down, so what they hold is decided by a period. The period is stored as a
 * name rather than as a number of days, because the list of choices is the thing the screen
 * shows and a stored count would drift away from the labels.
 */
enum class RecentRange(val label: String, val days: Long?) {
    PAST_MONTH("Past month", 30L),
    PAST_THREE_MONTHS("Past three months", 90L),
    PAST_SIX_MONTHS("Past six months", 180L),
    PAST_YEAR("Past year", 365L),
    ALL_TIME("All time", null),
}

/** The moment [range] begins at [nowMs], or null when it has no beginning. */
fun RecentRange.cutoffMs(nowMs: Long): Long? = days?.let { nowMs - it * MILLIS_PER_DAY }

/**
 * The songs added within [range], newest first.
 *
 * MediaStore reports when a song was added in seconds while everything else here works in
 * milliseconds, so the comparison converts rather than storing a second unit in the model.
 * A song with no date at all is left out of a bounded period: it cannot be shown to belong
 * in it, and guessing would put it in every one.
 */
fun List<Track>.addedWithin(range: RecentRange, nowMs: Long): List<Track> {
    val cutoff = range.cutoffMs(nowMs) ?: return sortedByDescending { it.dateAdded ?: 0L }
    return filter { track ->
        val added = track.dateAdded?.times(1_000L) ?: return@filter false
        added >= cutoff
    }.sortedByDescending { it.dateAdded ?: 0L }
}

/**
 * The songs last played within [range], most recently played first.
 *
 * History is joined by id, so a file that has gone from the device takes its play with it
 * rather than appearing in a list that cannot open it.
 */
fun List<Track>.playedWithin(
    stats: Map<Long, PlayStat>,
    range: RecentRange,
    nowMs: Long,
): List<Track> {
    val cutoff = range.cutoffMs(nowMs)
    return mapNotNull { track ->
        val playedAt = stats[track.id]?.lastPlayedAt ?: return@mapNotNull null
        if (cutoff != null && playedAt < cutoff) return@mapNotNull null
        track to playedAt
    }
        .sortedByDescending { it.second }
        .map { it.first }
}

private const val MILLIS_PER_DAY = 86_400_000L
