package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentRangeTest {

    private val now = 1_000_000_000_000L
    private val dayMs = 86_400_000L

    private fun track(id: Long, addedDaysAgo: Long?): Track = Track(
        id = id,
        title = "Track $id",
        artist = null,
        album = null,
        albumId = 0L,
        durationMs = 180_000L,
        contentUri = "content://media/external/audio/media/$id",
        dateAdded = addedDaysAgo?.let { (now - it * dayMs) / 1_000L },
        dateModified = 0L,
        sizeBytes = 0L,
        mimeType = null,
        trackNumber = 0,
    )

    @Test
    fun `all time has no beginning`() {
        assertNull(RecentRange.ALL_TIME.cutoffMs(now))
    }

    @Test
    fun `a bounded range begins the right number of days back`() {
        assertEquals(now - 30 * dayMs, RecentRange.PAST_MONTH.cutoffMs(now))
        assertEquals(now - 90 * dayMs, RecentRange.PAST_THREE_MONTHS.cutoffMs(now))
        assertEquals(now - 365 * dayMs, RecentRange.PAST_YEAR.cutoffMs(now))
    }

    @Test
    fun `added songs are newest first and leave out the older ones`() {
        val tracks = listOf(
            track(1, 40),
            track(2, 1),
            track(3, 10),
        )

        val found = tracks.addedWithin(RecentRange.PAST_MONTH, now)

        assertEquals(listOf(2L, 3L), found.map { it.id })
    }

    @Test
    fun `a song at the edge of the period is kept`() {
        val tracks = listOf(track(1, 30))

        assertEquals(listOf(1L), tracks.addedWithin(RecentRange.PAST_MONTH, now).map { it.id })
    }

    @Test
    fun `a song with no added date is left out of a bounded period`() {
        val tracks = listOf(track(1, null))

        assertTrue(tracks.addedWithin(RecentRange.PAST_MONTH, now).isEmpty())
    }

    @Test
    fun `all time keeps everything, dated or not`() {
        val tracks = listOf(track(1, 10), track(2, 4_000), track(3, null))

        assertEquals(
            listOf(1L, 2L, 3L),
            tracks.addedWithin(RecentRange.ALL_TIME, now).map { it.id },
        )
    }

    @Test
    fun `played songs come back most recent first and only inside the period`() {
        val tracks = listOf(track(1, 10), track(2, 10), track(3, 10))
        val stats = mapOf(
            1L to PlayStat(trackId = 1L, playCount = 2, lastPlayedAt = now - 40 * dayMs),
            2L to PlayStat(trackId = 2L, playCount = 1, lastPlayedAt = now - 2 * dayMs),
            3L to PlayStat(trackId = 3L, playCount = 1, lastPlayedAt = now - 20 * dayMs),
        )

        val found = tracks.playedWithin(stats, RecentRange.PAST_MONTH, now)

        assertEquals(listOf(2L, 3L), found.map { it.id })
    }

    @Test
    fun `a song that is gone from the library takes its play with it`() {
        val tracks = listOf(track(1, 10))
        val stats = mapOf(
            9L to PlayStat(trackId = 9L, playCount = 1, lastPlayedAt = now - dayMs),
        )

        assertTrue(tracks.playedWithin(stats, RecentRange.ALL_TIME, now).isEmpty())
    }

    @Test
    fun `a play with no time is left out of a bounded list and out of all time too`() {
        val tracks = listOf(track(1, 10))
        val stats = mapOf(1L to PlayStat(trackId = 1L, playCount = 3, lastPlayedAt = null))

        assertTrue(tracks.playedWithin(stats, RecentRange.PAST_MONTH, now).isEmpty())
        assertTrue(tracks.playedWithin(stats, RecentRange.ALL_TIME, now).isEmpty())
    }
}
