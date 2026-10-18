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
}
