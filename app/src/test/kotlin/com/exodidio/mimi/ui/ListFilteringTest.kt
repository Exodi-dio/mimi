package com.exodidio.mimi.ui

import com.exodidio.mimi.library.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ListFilteringTest {

    @Test
    fun `a blank query leaves the list exactly as it was`() {
        val songs = listOf(track(1, "Blue"), track(2, "Red"))

        assertSame(songs, songs.matchingListFilter("   "))
    }

    @Test
    fun `the filter matches a title, an artist or an album`() {
        val songs = listOf(
            track(1, "Blue", artist = "Ada", album = "First"),
            track(2, "Green", artist = "Bea", album = "Second"),
            track(3, "Red", artist = "Cy", album = "Blue Hour"),
        )

        assertEquals(listOf(1L, 3L), songs.matchingListFilter("blue").map { it.id })
        assertEquals(listOf(2L), songs.matchingListFilter("bea").map { it.id })
    }

    @Test
    fun `matching ignores case and surrounding space`() {
        val songs = listOf(track(1, "Blue", artist = "Ada"))

        assertEquals(listOf(1L), songs.matchingListFilter("  BLUE ").map { it.id })
    }

    @Test
    fun `the filter line says how many of the list survived`() {
        assertEquals("3 songs", listFilterLabel(3, 3))
        assertEquals("1 of 3 songs matches", listFilterLabel(1, 3))
        assertEquals("2 of 3 songs match", listFilterLabel(2, 3))
    }

    private fun track(
        id: Long,
        title: String,
        artist: String? = null,
        album: String? = null,
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = 1L,
        durationMs = 180_000L,
        contentUri = "content://media/audio/$id",
        dateModified = 0L,
        sizeBytes = 1_000L,
        mimeType = "audio/mpeg",
        trackNumber = 1,
    )
}
