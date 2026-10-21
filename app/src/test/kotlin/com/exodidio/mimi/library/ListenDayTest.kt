package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ListenDayTest {

    private val zone = ZoneId.of("Europe/London")

    @Test
    fun `a day is named in the zone it was heard in`() {
        // 23:30 UTC on the fifth is already the sixth in London during summer time.
        val lateEvening = 1_625_527_800_000L

        assertEquals("2021-07-05", localDayOf(lateEvening, ZoneId.of("UTC")))
        assertEquals("2021-07-06", localDayOf(lateEvening, zone))
    }

    @Test
    fun `days run forward from the start without gaps`() {
        val days = daysBetween(LocalDate.of(2026, 2, 27), 4)

        assertEquals(
            listOf("2026-02-27", "2026-02-28", "2026-03-01", "2026-03-02"),
            days,
        )
    }

    @Test
    fun `a period of no days has no keys`() {
        assertTrue(daysBetween(LocalDate.of(2026, 1, 1), 0).isEmpty())
        assertTrue(daysBetween(LocalDate.of(2026, 1, 1), -3).isEmpty())
    }

    @Test
    fun `a playing stretch counts the time between readings`() {
        assertEquals(12_500L, listeningElapsedMs(true, 1_000L, 13_500L))
    }

    @Test
    fun `a pause does not count the silence before it`() {
        assertEquals(0L, listeningElapsedMs(false, 1_000L, 60_000L))
    }

    @Test
    fun `a missing previous reading counts nothing`() {
        assertEquals(0L, listeningElapsedMs(true, null, 60_000L))
    }

    @Test
    fun `a clock that appears to go backwards counts nothing rather than owing time`() {
        assertEquals(0L, listeningElapsedMs(true, 9_000L, 4_000L))
    }
}
