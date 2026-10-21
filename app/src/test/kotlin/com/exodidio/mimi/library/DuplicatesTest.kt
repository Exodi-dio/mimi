package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicatesTest {

    @Test
    fun `copies within two seconds form one group`() {
        val songs = listOf(
            track(1, "Blue", "Ada", 180_000L),
            track(2, "Blue", "Ada", 181_500L),
        )

        val groups = songs.duplicateGroups()

        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 2L), groups.single().tracks.map { it.id })
    }

    @Test
    fun `the comparison ignores case and spacing`() {
        val songs = listOf(
            track(1, " Blue ", "ada", 180_000L),
            track(2, "blue", "Ada", 180_500L),
        )

        assertEquals(1, songs.duplicateGroups().size)
    }

    @Test
    fun `a different artist is not a duplicate`() {
        val songs = listOf(
            track(1, "Blue", "Ada", 180_000L),
            track(2, "Blue", "Bea", 180_000L),
        )

        assertTrue(songs.duplicateGroups().isEmpty())
    }

    @Test
    fun `a longer gap is a different recording`() {
        val songs = listOf(
            track(1, "Blue", "Ada", 180_000L),
            track(2, "Blue", "Ada", 190_000L),
        )

        assertTrue(songs.duplicateGroups().isEmpty())
    }

    @Test
    fun `a run of copies cannot chain a wide gap together`() {
        val songs = listOf(
            track(1, "Blue", "Ada", 180_000L),
            track(2, "Blue", "Ada", 181_500L),
            track(3, "Blue", "Ada", 183_000L),
        )

        // The third copy is close to the second but three seconds from the shortest, so
        // the comparison is made against the group's first copy rather than its neighbour.
        val groups = songs.duplicateGroups()

        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 2L), groups.single().tracks.map { it.id })
    }

    @Test
    fun `a lone copy is not worth reporting`() {
        val songs = listOf(track(1, "Blue", "Ada", 180_000L))

        assertTrue(songs.duplicateGroups().isEmpty())
    }

    private fun track(id: Long, title: String, artist: String?, durationMs: Long) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Blue",
        albumId = 1L,
        durationMs = durationMs,
        contentUri = "content://media/audio/$id",
        dateModified = 0L,
        sizeBytes = 1_000L,
        mimeType = "audio/mpeg",
        trackNumber = 1,
    )
}
