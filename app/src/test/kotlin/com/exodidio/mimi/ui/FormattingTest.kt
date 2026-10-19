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
}
