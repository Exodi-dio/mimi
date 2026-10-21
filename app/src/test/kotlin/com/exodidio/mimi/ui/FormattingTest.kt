package com.exodidio.mimi.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun `unknown duration renders as placeholder`() {
        assertEquals("--:--", formatDuration(0L))
        assertEquals("--:--", formatDuration(-1L))
    }

    @Test
    fun `sub-minute durations keep a zero minute prefix`() {
        assertEquals("0:07", formatDuration(7_000L))
        assertEquals("0:59", formatDuration(59_999L))
    }

    @Test
    fun `minutes and seconds are padded`() {
        assertEquals("3:05", formatDuration(185_000L))
        assertEquals("12:34", formatDuration(754_000L))
    }

    @Test
    fun `hours only appear when there are hours`() {
        assertEquals("59:59", formatDuration(3_599_000L))
        assertEquals("1:00:00", formatDuration(3_600_000L))
        assertEquals("2:03:04", formatDuration(7_384_000L))
    }

    @Test
    fun `a file size under a kilobyte is counted in bytes`() {
        assertEquals("0 B", formatFileSize(0L))
        assertEquals("999 B", formatFileSize(999L))
    }

    @Test
    fun `sizes climb one unit at a time`() {
        assertEquals("1 kB", formatFileSize(1_000L))
        assertEquals("4.2 MB", formatFileSize(4_200_000L))
        assertEquals("1.5 GB", formatFileSize(1_500_000_000L))
    }

    @Test
    fun `a size that would round into the next unit is shown as that unit`() {
        // 999999 bytes is 999.999 kB, which reads better as 1 MB than as 1000 kB.
        assertEquals("1 MB", formatFileSize(999_999L))
    }

    @Test
    fun `whole numbers lose the needless decimal`() {
        assertEquals("2 MB", formatFileSize(2_000_000L))
        assertEquals("2.5 MB", formatFileSize(2_500_000L))
    }

    @Test
    fun `an empty library has no running time to speak of`() {
        assertEquals("No music yet", formatTotalDuration(0L))
        assertEquals("No music yet", formatTotalDuration(-1L))
    }

    @Test
    fun `a running time under an hour is counted in minutes`() {
        assertEquals("30 seconds", formatTotalDuration(30_000L))
        assertEquals("1 minute", formatTotalDuration(60_000L))
        assertEquals("59 minutes", formatTotalDuration(3_540_000L))
    }

    @Test
    fun `a running time under a day is counted in hours and minutes`() {
        assertEquals("1 hour", formatTotalDuration(3_600_000L))
        assertEquals("1 hour 5 minutes", formatTotalDuration(3_900_000L))
        assertEquals("23 hours", formatTotalDuration(82_800_000L))
    }

    @Test
    fun `a running time past a day drops the minutes`() {
        assertEquals("1 day", formatTotalDuration(86_400_000L))
        assertEquals("1 day 1 hour", formatTotalDuration(90_000_000L))
        assertEquals("2 days 3 hours", formatTotalDuration(183_600_000L))
    }

    @Test
    fun `a list header keeps the two largest units in short form`() {
        assertEquals("0 min", formatListDuration(0L))
        assertEquals("30 sec", formatListDuration(30_000L))
        assertEquals("2 min", formatListDuration(120_000L))
        assertEquals("1 hr 5 min", formatListDuration(3_900_000L))
        assertEquals("1 hr", formatListDuration(3_600_000L))
        assertEquals("1 day 2 hr", formatListDuration(93_600_000L))
    }

    @Test
    fun `a song summary carries the count and the running time`() {
        assertEquals("1 song \u00b7 3 min", songSummaryLabel(1, 180_000L))
        assertEquals("12 songs \u00b7 36 min", songSummaryLabel(12, 2_160_000L))
    }

    @Test
    fun `an empty list has no running time bolted on`() {
        assertEquals("0 songs", songSummaryLabel(0, 0L))
        assertEquals("3 songs", songSummaryLabel(3, 0L))
    }

    @Test
    fun `the total ignores songs whose length is unknown`() {
        val songs = listOf(
            track(1, 120_000L),
            track(2, 180_000L),
            track(3, -1L),
        )

        assertEquals(300_000L, totalDurationMs(songs))
    }

    private fun track(id: Long, durationMs: Long) = com.exodidio.mimi.library.Track(
        id = id,
        title = "Song $id",
        artist = null,
        album = null,
        albumId = 0L,
        durationMs = durationMs,
        contentUri = "content://media/audio/$id",
        dateModified = 0L,
        sizeBytes = 0L,
        mimeType = null,
        trackNumber = 0,
    )
}
