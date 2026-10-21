package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DurationRangeTest {

    @Test
    fun `no bounds lets every song through`() {
        assertTrue(isWithinDurationRange(0L, 0L, 0L))
        assertTrue(isWithinDurationRange(7L, 0L, 0L))
        assertTrue(isWithinDurationRange(3_600_000L, 0L, 0L))
    }

    @Test
    fun `the lower bound is inclusive and the upper bound is too`() {
        assertFalse(isWithinDurationRange(59_999L, 60_000L, 0L))
        assertTrue(isWithinDurationRange(60_000L, 60_000L, 0L))

        assertTrue(isWithinDurationRange(300_000L, 0L, 300_000L))
        assertFalse(isWithinDurationRange(300_001L, 0L, 300_000L))
    }

    @Test
    fun `both bounds have to hold`() {
        assertFalse(isWithinDurationRange(30_000L, 60_000L, 3_600_000L))
        assertTrue(isWithinDurationRange(120_000L, 60_000L, 3_600_000L))
        assertFalse(isWithinDurationRange(4_000_000L, 60_000L, 3_600_000L))
    }

    @Test
    fun `a stored bound lands on a slider stop`() {
        assertEquals(0, durationMinStop(0L))
        assertEquals(1, durationMinStop(60_000L))
        assertEquals(2, durationMinStop(300_000L))
        assertEquals(3, durationMinStop(3_600_000L))
    }

    @Test
    fun `the old five second bound sits at the off stop but still reads as five seconds`() {
        assertEquals(0, durationMinStop(5_000L))
        assertEquals("5s \u2013 \u221e", durationRangeLabel(5_000L, 0L))
    }

    @Test
    fun `the upper thumb reaches an unlimited stop`() {
        assertEquals(DurationRangeUnlimitedStop, durationMaxStop(0L))
        assertEquals(1, durationMaxStop(60_000L))
        assertEquals(3, durationMaxStop(3_600_000L))
    }

    @Test
    fun `a stop turns back into the length it names`() {
        assertEquals(0L, durationMinMsFor(0))
        assertEquals(60_000L, durationMinMsFor(1))
        assertEquals(300_000L, durationMinMsFor(2))
        assertEquals(3_600_000L, durationMinMsFor(3))
        assertEquals(0L, durationMaxMsFor(DurationRangeUnlimitedStop))
        assertEquals(3_600_000L, durationMaxMsFor(3))
    }

    @Test
    fun `the stops are ordered and the last one is not a length`() {
        assertEquals(listOf(0L, 60_000L, 300_000L, 3_600_000L), DurationRangeStopsMs)
        assertEquals(DurationRangeStopsMs.size, DurationRangeUnlimitedStop)
    }

    @Test
    fun `the value text says what the range really is`() {
        assertEquals("off", durationRangeLabel(0L, 0L))
        assertEquals("1m \u2013 \u221e", durationRangeLabel(60_000L, 0L))
        assertEquals("up to 5m", durationRangeLabel(0L, 300_000L))
        assertEquals("1m \u2013 1h", durationRangeLabel(60_000L, 3_600_000L))
    }

    @Test
    fun `a length that is not a stop still reads exactly`() {
        assertEquals("45s", shortDurationLabel(45_000L))
        assertEquals("5m", shortDurationLabel(300_000L))
        assertEquals("1h", shortDurationLabel(3_600_000L))
        assertEquals("90s", shortDurationLabel(90_000L))
    }
}
