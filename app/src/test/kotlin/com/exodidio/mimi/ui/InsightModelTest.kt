package com.exodidio.mimi.ui

import com.exodidio.mimi.library.ListenDay
import com.exodidio.mimi.library.PlayStat
import com.exodidio.mimi.library.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class InsightModelTest {

    private val today = LocalDate.of(2026, 3, 10)
    private val zone = ZoneId.of("UTC")

    @Test
    fun `a format is read from the MIME type before the file name`() {
        assertEquals(AudioFormat.FLAC, audioFormatOf("audio/flac", "song.mp3"))
        assertEquals(AudioFormat.MP3, audioFormatOf("audio/mpeg", null))
        assertEquals(AudioFormat.AAC, audioFormatOf("audio/mp4", "song.flac"))
        assertEquals(AudioFormat.OGG, audioFormatOf("audio/ogg", null))
        assertEquals(AudioFormat.WAV, audioFormatOf("audio/x-wav", null))
    }

    @Test
    fun `the extension is the fallback when the MIME type says nothing useful`() {
        assertEquals(AudioFormat.FLAC, audioFormatOf("audio/*", "song.FLAC"))
        assertEquals(AudioFormat.AAC, audioFormatOf(null, "song.m4a"))
        assertEquals(AudioFormat.OGG, audioFormatOf(null, "song.opus"))
        assertEquals(AudioFormat.OTHER, audioFormatOf(null, "song.aiff"))
        assertEquals(AudioFormat.OTHER, audioFormatOf(null, null))
    }

    @Test
    fun `growth is cumulative and ends on the songs the library holds`() {
        val tracks = listOf(
            track(1, addedAt = "2026-03-01"),
            track(2, addedAt = "2026-03-01"),
            track(3, addedAt = "2026-03-05"),
        )

        val growth = libraryGrowth(tracks, InsightPeriod.WEEK, today, zone)

        assertEquals(7, growth.size)
        assertEquals(2L, growth[0].value)
        assertEquals(3L, growth[1].value)
        assertEquals(3L, growth.last().value)
    }

    @Test
    fun `all time growth is counted by month`() {
        val tracks = listOf(
            track(1, addedAt = "2026-01-04"),
            track(2, addedAt = "2026-02-11"),
            track(3, addedAt = "2026-02-20"),
        )

        val growth = libraryGrowth(tracks, InsightPeriod.ALL, today, zone)

        assertEquals(listOf("Jan 26", "Feb 26", "Mar 26"), growth.map { it.label })
        assertEquals(listOf(1L, 3L, 3L), growth.map { it.value })
    }

    @Test
    fun `formats are ranked and formats with no songs are left out`() {
        val slices = formatBreakdown(
            listOf(
                track(1, mime = "audio/flac"),
                track(2, mime = "audio/flac"),
                track(3, mime = "audio/mpeg"),
            )
        )

        assertEquals(listOf("FLAC", "MP3"), slices.map { it.label })
        assertEquals(listOf(2L, 1L), slices.map { it.value })
    }

    @Test
    fun `a streak counts back from today`() {
        val days = setOf("2026-03-10", "2026-03-09", "2026-03-08", "2026-03-06")

        assertEquals(3, currentStreak(days, today))
    }

    @Test
    fun `a streak survives the morning after it was last listened to`() {
        val days = setOf("2026-03-09", "2026-03-08")

        assertEquals(2, currentStreak(days, today))
    }

    @Test
    fun `two quiet days end a streak`() {
        assertEquals(0, currentStreak(setOf("2026-03-07"), today))
    }

    @Test
    fun `listening adds up, compares with the previous period and rates skips`() {
        val tracks = listOf(track(1), track(2))
        val days = listOf(
            ListenDay(trackId = 1, day = "2026-03-05", listenedMs = 60_000, plays = 1),
            ListenDay(trackId = 2, day = "2026-03-10", listenedMs = 60_000, plays = 1),
            ListenDay(trackId = 1, day = "2026-03-02", listenedMs = 30_000, plays = 1),
        )
        val stats = mapOf(
            1L to PlayStat(trackId = 1, playCount = 2, skipCount = 1),
            2L to PlayStat(trackId = 2, playCount = 1, skipCount = 0),
        )

        val listening = insightListening(
            period = InsightPeriod.WEEK,
            today = today,
            days = days,
            tracks = tracks,
            playStats = stats,
            separators = emptyList(),
        )

        assertEquals(120_000L, listening.listenedMs)
        assertEquals(2, listening.plays)
        assertEquals(25, listening.skipRatePercent)
        assertEquals(60_000L, listening.averageSessionMs)
        assertEquals(100.0, listening.changePercent!!, 0.01)
        assertEquals(7, listening.bars.size)
        assertEquals(3, listening.streakDays)
        assertTrue(listening.hasHistory)
    }

    @Test
    fun `a previous period with no listening has no change to show`() {
        val listening = insightListening(
            period = InsightPeriod.WEEK,
            today = today,
            days = listOf(ListenDay(trackId = 1, day = "2026-03-10", listenedMs = 5_000, plays = 1)),
            tracks = listOf(track(1)),
            playStats = emptyMap(),
            separators = emptyList(),
        )

        assertNull(listening.changePercent)
        assertNull(listening.skipRatePercent)
    }

    @Test
    fun `the genre breakdown keeps five and folds the rest into Other`() {
        val tracks = (1..6).map { id -> track(id.toLong(), genre = "Genre $id") }
        val days = tracks.map { track ->
            ListenDay(trackId = track.id, day = "2026-03-10", listenedMs = 1_000L)
        }

        val slices = genreBreakdown(days, tracks, emptyList())

        assertEquals(6, slices.size)
        assertEquals("Other", slices.last().label)
        assertEquals(1_000L, slices.last().value)
    }

    @Test
    fun `artists are ranked by the time heard`() {
        val tracks = listOf(
            track(1, artist = "Ada"),
            track(2, artist = "Bea"),
            track(3, artist = "Ada"),
        )
        val days = listOf(
            ListenDay(trackId = 1, day = "2026-03-10", listenedMs = 2_000L),
            ListenDay(trackId = 2, day = "2026-03-10", listenedMs = 9_000L),
            ListenDay(trackId = 3, day = "2026-03-10", listenedMs = 3_000L),
        )

        val artists = topArtists(days, tracks, emptyList())

        assertEquals(listOf("Bea", "Ada"), artists.map { it.artist })
        assertEquals(9_000L, artists.first().listenedMs)
        assertEquals(5_000L, artists.last().listenedMs)
    }

    @Test
    fun `top songs carry the plays counted for them`() {
        val tracks = listOf(track(1), track(2))
        val days = listOf(
            ListenDay(trackId = 1, day = "2026-03-10", listenedMs = 5_000L, plays = 1),
            ListenDay(trackId = 2, day = "2026-03-10", listenedMs = 8_000L, plays = 2),
        )

        val top = topTracks(days, tracks)

        assertEquals(listOf(2L, 1L), top.map { it.track.id })
        assertEquals(2, top.first().plays)
    }

    @Test
    fun `history for a song that has gone is not counted`() {
        val listening = insightListening(
            period = InsightPeriod.ALL,
            today = today,
            days = listOf(ListenDay(trackId = 99, day = "2026-03-10", listenedMs = 9_000, plays = 2)),
            tracks = listOf(track(1)),
            playStats = mapOf(99L to PlayStat(trackId = 99, playCount = 50)),
            separators = emptyList(),
        )

        assertEquals(0L, listening.listenedMs)
        assertNull(listening.changePercent)
        assertTrue(listening.topTracks.isEmpty())
    }

    private fun track(
        id: Long,
        artist: String? = "Someone",
        genre: String? = null,
        mime: String? = null,
        addedAt: String? = null,
    ) = Track(
        id = id,
        title = "Song $id",
        artist = artist,
        album = "An album",
        albumId = 1L,
        durationMs = 180_000L,
        contentUri = "content://media/audio/$id",
        dateAdded = addedAt?.let { LocalDate.parse(it).atStartOfDay(zone).toEpochSecond() },
        dateModified = 0L,
        sizeBytes = 1_000L,
        mimeType = mime,
        trackNumber = 1,
        genre = genre,
    )
}
