package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Test

class SongSortingTest {

    @Test
    fun `title order ignores case`() {
        val songs = listOf(track(1, "banana"), track(2, "Apple"), track(3, "cherry"))

        assertEquals(
            listOf("Apple", "banana", "cherry"),
            songs.sortedWith(SongSort.TITLE.comparator(descending = false)).titles(),
        )
    }

    @Test
    fun `title order runs backwards when asked`() {
        val songs = listOf(track(1, "banana"), track(2, "Apple"), track(3, "cherry"))

        assertEquals(
            listOf("cherry", "banana", "Apple"),
            songs.sortedWith(SongSort.TITLE.comparator(descending = true)).titles(),
        )
    }

    @Test
    fun `artist order groups an artist's songs together`() {
        val songs = listOf(
            track(1, "One", artist = "Zed"),
            track(2, "Two", artist = "Ada"),
            track(3, "Three", artist = "Ada"),
        )

        assertEquals(
            listOf("Two", "Three", "One"),
            songs.sortedWith(SongSort.ARTIST.comparator(descending = false)).titles(),
        )
    }

    @Test
    fun `songs with no artist sort last, either way round`() {
        val songs = listOf(track(1, "Untitled"), track(2, "Named", artist = "Ada"))

        assertEquals(
            listOf("Named", "Untitled"),
            songs.sortedWith(SongSort.ARTIST.comparator(descending = false)).titles(),
        )
        assertEquals(
            listOf("Named", "Untitled"),
            songs.sortedWith(SongSort.ARTIST.comparator(descending = true)).titles(),
        )
    }

    @Test
    fun `newest first means the largest date first`() {
        val songs = listOf(
            track(1, "Older", dateAdded = 100L),
            track(2, "Newest", dateAdded = 300L),
            track(3, "Middle", dateAdded = 200L),
        )

        assertEquals(
            listOf("Newest", "Middle", "Older"),
            songs.sortedWith(SongSort.DATE_ADDED.comparator(descending = true)).titles(),
        )
    }

    @Test
    fun `years sort newest first, with untagged songs last`() {
        val songs = listOf(
            track(1, "Nineties", year = 1994),
            track(2, "Unknown"),
            track(3, "Eighties", year = 1984),
            track(4, "Later", year = 2011),
        )

        assertEquals(
            listOf("Later", "Nineties", "Eighties", "Unknown"),
            songs.sortedWith(SongSort.YEAR.comparator(descending = true)).titles(),
        )
    }

    @Test
    fun `duration sorts by length and not by title`() {
        val songs = listOf(
            track(1, "Zebra", durationMs = 60_000L),
            track(2, "Apple", durationMs = 240_000L),
            track(3, "Mango", durationMs = 180_000L),
        )

        assertEquals(
            listOf("Zebra", "Mango", "Apple"),
            songs.sortedWith(SongSort.DURATION.comparator(descending = false)).titles(),
        )
        assertEquals(
            listOf("Apple", "Mango", "Zebra"),
            songs.sortedWith(SongSort.DURATION.comparator(descending = true)).titles(),
        )
    }

    @Test
    fun `songs that agree on the chosen field keep a settled order`() {
        // Two songs off the same album: the album order decides nothing, so the titles
        // must, and the result must not depend on the order they arrived in.
        val songs = listOf(
            track(30, "Zebra", album = "Same"),
            track(10, "Apple", album = "Same"),
            track(20, "Mango", album = "Same"),
        )

        val forward = songs.sortedWith(SongSort.ALBUM.comparator(descending = false)).titles()
        val reversedInput = songs.reversed()
            .sortedWith(SongSort.ALBUM.comparator(descending = false)).titles()

        assertEquals(listOf("Apple", "Mango", "Zebra"), forward)
        assertEquals(forward, reversedInput)
    }

    @Test
    fun `identical titles fall back to the id so the order is reproducible`() {
        val songs = listOf(track(9, "Same"), track(4, "Same"), track(7, "Same"))

        assertEquals(
            listOf(4L, 7L, 9L),
            songs.sortedWith(SongSort.TITLE.comparator(descending = false)).map { it.id },
        )
    }

    @Test
    fun `artists differing only in case still compare as one artist`() {
        val songs = listOf(
            track(1, "B", artist = "ada"),
            track(2, "A", artist = "Ada"),
        )

        assertEquals(
            listOf("A", "B"),
            songs.sortedWith(SongSort.ARTIST.comparator(descending = false)).titles(),
        )
    }

    private fun List<Track>.titles(): List<String> = map { it.title }

    private fun track(
        id: Long,
        title: String,
        artist: String? = null,
        album: String? = null,
        year: Int? = null,
        dateAdded: Long? = null,
        durationMs: Long = 0L,
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = 0L,
        durationMs = durationMs,
        contentUri = "content://media/external/audio/media/$id",
        dateModified = 0L,
        sizeBytes = 0L,
        mimeType = null,
        trackNumber = 0,
        year = year,
        dateAdded = dateAdded,
    )
}
