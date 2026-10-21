package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailSortingTest {

    @Test
    fun `as listed leaves the album's own running order alone`() {
        val songs = listOf(track(1, "Zed", 300_000L), track(2, "Ada", 100_000L))

        assertEquals(
            listOf(1L, 2L),
            songs.sortedForDetail(DetailSort.AS_LISTED, descending = false).map { it.id },
        )
    }

    @Test
    fun `title order runs backwards when asked`() {
        val songs = listOf(track(1, "banana"), track(2, "Apple"), track(3, "cherry"))

        assertEquals(
            listOf(3L, 1L, 2L),
            songs.sortedForDetail(DetailSort.TITLE, descending = true).map { it.id },
        )
    }

    @Test
    fun `duration order puts the shortest first by default`() {
        val songs = listOf(
            track(1, "Long", 300_000L),
            track(2, "Short", 60_000L),
            track(3, "Middle", 180_000L),
        )

        assertEquals(
            listOf(2L, 3L, 1L),
            songs.sortedForDetail(DetailSort.DURATION, descending = false).map { it.id },
        )
    }

    @Test
    fun `the default keeps an album in its tagged order`() {
        assertEquals(DetailSort.AS_LISTED, Settings().detailSort)
    }

    private fun track(id: Long, title: String, durationMs: Long = 180_000L) = Track(
        id = id,
        title = title,
        artist = "Ada",
        album = "Blue",
        albumId = 1L,
        durationMs = durationMs,
        contentUri = "content://media/audio/$id",
        dateModified = 0L,
        sizeBytes = 1_000L,
        mimeType = "audio/mpeg",
        trackNumber = id.toInt(),
    )
}
