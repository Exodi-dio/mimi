package com.exodidio.mimi.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * How long one song was heard on one local day.
 *
 * The totals in [PlayStat] answer "how many times" but not "when", and a chart of listening
 * needs the second question. A row per song per day is the smallest shape that can be
 * summed into any period the screen offers without storing a row for every tick of the
 * player.
 *
 * The day is kept as `yyyy-MM-dd` in the device's own zone rather than as an instant,
 * because a listen belongs to the evening it was heard and not to a UTC date that may have
 * already rolled over.
 *
 * History begins with this table. An upgrade from an older version keeps its play and skip
 * counts but has no listening time to show, because nothing recorded time before this
 * table existed; inventing a value would be worse than an honest empty chart.
 */
@Entity(
    tableName = "listen_days",
    indices = [Index(value = ["track_id", "day"], unique = true)],
)
data class ListenDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo("track_id") val trackId: Long,
    /** yyyy-MM-dd in the device's own time zone. */
    val day: String,
    @ColumnInfo("listened_ms") val listenedMs: Long = 0,
    val plays: Int = 0,
)

/**
 * The calendar day [epochMs] fell on, using the listener's own clock.
 *
 * Pure so the day a listen belongs to can be checked without a device, and so every writer
 * agrees on the same format. ISO ordering is deliberate: the string sorts in date order,
 * which is what lets the reads compare days without parsing each row.
 */
fun localDayOf(epochMs: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().toString()

/**
 * [count] consecutive day keys, starting at [start].
 *
 * Used to build an axis that includes days with no listening. A chart that only drew days
 * somebody listened on would hide the gaps, which are half of what a listening history has
 * to say.
 */
fun daysBetween(start: LocalDate, count: Int): List<String> {
    if (count <= 0) return emptyList()
    return (0 until count).map { offset -> start.plusDays(offset.toLong()).toString() }
}

/**
 * How much listening elapsed between two readings of a monotonic clock.
 *
 * The player reports a position many times a second, and a wall clock would count a phone
 * call, a time zone change or a manual clock correction as listening. The caller decides
 * whether the previous reading was actually playing, which is what keeps a pause and a
 * later resume from counting the silence between them twice.
 */
fun listeningElapsedMs(wasPlaying: Boolean, sinceElapsedMs: Long?, nowElapsedMs: Long): Long {
    if (!wasPlaying || sinceElapsedMs == null) return 0L
    return (nowElapsedMs - sinceElapsedMs).coerceAtLeast(0L)
}
