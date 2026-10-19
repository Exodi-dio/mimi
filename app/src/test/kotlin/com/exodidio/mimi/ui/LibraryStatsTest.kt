package com.exodidio.mimi.ui

import com.exodidio.mimi.library.PlayStat
import com.exodidio.mimi.library.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryStatsTest {

    private fun track(
        id: Long,
        durationMs: Long = 180_000L,
        title: String = "Song $id",
        artist: String = "Someone",
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "An album",
        albumId = 1L,
        durationMs = durationMs,
        contentUri = "content://media/audio/$id",
        dateModified = 0L,
        sizeBytes = 1_000L,
        mimeType = "audio/mpeg",
        trackNumber = 1,
    )

    @Test
    fun `an empty library adds up to nothing`() {
        val stats = libraryStats(
            songs = emptyList(),
            albumCount = 0,
            artistCount = 0,
            lovedIds = emptySet(),
            playlistCount = 0,
            playStats = emptyMap(),
        )

        assertEquals(0, stats.songs)
        assertEquals(0L, stats.totalDurationMs)
        assertNull(stats.longestSong)
    }

    @Test
    fun `counts come from the songs and the shelves they sit on`() {
        val songs = listOf(track(1L), track(2L), track(3L))
        val stats = libraryStats(
            songs = songs,
            albumCount = 2,
            artistCount = 2,
            lovedIds = setOf(1L, 3L),
            playlistCount = 4,
            playStats = emptyMap(),
        )

        assertEquals(3, stats.songs)
        assertEquals(2, stats.albums)
        assertEquals(2, stats.artists)
        assertEquals(2, stats.loved)
        assertEquals(4, stats.playlists)
    }

    @Test
    fun `total playing time is the sum of the songs`() {
        val stats = libraryStats(
            songs = listOf(
                track(1L, durationMs = 60_000L),
                track(2L, durationMs = 90_000L),
                track(3L, durationMs = 30_000L),
            ),
            albumCount = 0,
            artistCount = 0,
            lovedIds = emptySet(),
            playlistCount = 0,
            playStats = emptyMap(),
        )

        assertEquals(180_000L, stats.totalDurationMs)
    }

    @Test
    fun `the longest song is the longest one`() {
        val long = track(7L, durationMs = 600_000L)
        val stats = libraryStats(
            songs = listOf(track(1L), long, track(2L)),
            albumCount = 0,
            artistCount = 0,
            lovedIds = emptySet(),
            playlistCount = 0,
            playStats = emptyMap(),
        )

        assertEquals(long, stats.longestSong)
    }

    @Test
    fun `listening totals add up across every song`() {
        val stats = libraryStats(
            songs = listOf(track(1L), track(2L)),
            albumCount = 0,
            artistCount = 0,
            lovedIds = emptySet(),
            playlistCount = 0,
            playStats = mapOf(
                1L to PlayStat(trackId = 1L, playCount = 3, skipCount = 1),
                2L to PlayStat(trackId = 2L, playCount = 4, skipCount = 2),
            ),
        )

        assertEquals(7L, stats.totalPlays)
        assertEquals(3L, stats.totalSkips)
    }

    @Test
    fun `a tally left behind by a deleted song is not counted`() {
        val stats = libraryStats(
            songs = listOf(track(1L)),
            albumCount = 0,
            artistCount = 0,
            lovedIds = emptySet(),
            playlistCount = 0,
            playStats = mapOf(
                1L to PlayStat(trackId = 1L, playCount = 2, skipCount = 0),
                9L to PlayStat(trackId = 9L, playCount = 500, skipCount = 500),
            ),
        )

        assertEquals(2L, stats.totalPlays)
        assertEquals(0L, stats.totalSkips)
    }

    @Test
    fun `a loved song that has gone is not still loved`() {
        val stats = libraryStats(
            songs = listOf(track(1L)),
            albumCount = 0,
            artistCount = 0,
            lovedIds = setOf(1L, 42L),
            playlistCount = 0,
            playStats = emptyMap(),
        )

        assertEquals(1, stats.loved)
    }
}
