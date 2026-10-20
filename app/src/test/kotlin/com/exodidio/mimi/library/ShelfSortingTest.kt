package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfSortingTest {

    private fun album(
        id: Long,
        name: String,
        artist: String? = "Someone",
        songs: Int = 1,
    ) = AlbumSummary(
        albumId = id,
        album = name,
        artist = artist,
        trackCount = songs,
        artworkUri = null,
    )

    private fun artist(name: String, songs: Int = 1, albums: Int = 1) = ArtistSummary(
        artist = name,
        trackCount = songs,
        albumCount = albums,
    )

    @Test
    fun `album names read upwards and ignore case`() {
        val sorted = listOf(album(1, "beta"), album(2, "Alpha"), album(3, "Gamma"))
            .sortedWith(AlbumSort.NAME.comparator())
            .map { it.album }

        assertEquals(listOf("Alpha", "beta", "Gamma"), sorted)
    }

    @Test
    fun `albums that share a name settle on their id`() {
        val sorted = listOf(album(9, "Same"), album(4, "Same"), album(7, "Same"))
            .sortedWith(AlbumSort.NAME.comparator())
            .map { it.albumId }

        assertEquals(listOf(4L, 7L, 9L), sorted)
    }

    @Test
    fun `albums by the same artist are grouped and then named`() {
        val sorted = listOf(
            album(1, "Zebra", artist = "Ada"),
            album(2, "Apple", artist = "Bo"),
            album(3, "Banana", artist = "Ada"),
        ).sortedWith(AlbumSort.ARTIST.comparator()).map { it.album }

        assertEquals(listOf("Banana", "Zebra", "Apple"), sorted)
    }

    @Test
    fun `an album with no album artist goes last`() {
        val sorted = listOf(
            album(1, "A", artist = null),
            album(2, "B", artist = "Zed"),
        ).sortedWith(AlbumSort.ARTIST.comparator()).map { it.album }

        assertEquals(listOf("B", "A"), sorted)
    }

    @Test
    fun `most songs puts the biggest album first`() {
        val sorted = listOf(
            album(1, "Small", songs = 3),
            album(2, "Big", songs = 40),
            album(3, "Middle", songs = 12),
        ).sortedWith(AlbumSort.SONGS.comparator()).map { it.album }

        assertEquals(listOf("Big", "Middle", "Small"), sorted)
    }

    @Test
    fun `albums of the same size stay in name order`() {
        val sorted = listOf(
            album(1, "Zebra", songs = 5),
            album(2, "Apple", songs = 5),
        ).sortedWith(AlbumSort.SONGS.comparator()).map { it.album }

        assertEquals(listOf("Apple", "Zebra"), sorted)
    }

    @Test
    fun `artist names read upwards and ignore case`() {
        val sorted = listOf(artist("zeta"), artist("Alpha"), artist("mike"))
            .sortedWith(ArtistSort.NAME.comparator())
            .map { it.artist }

        assertEquals(listOf("Alpha", "mike", "zeta"), sorted)
    }

    @Test
    fun `most albums puts the busiest artist first`() {
        val sorted = listOf(
            artist("Ada", albums = 2),
            artist("Bo", albums = 9),
            artist("Cy", albums = 5),
        ).sortedWith(ArtistSort.ALBUMS.comparator()).map { it.artist }

        assertEquals(listOf("Bo", "Cy", "Ada"), sorted)
    }

    @Test
    fun `most songs puts the biggest artist first, ties by name`() {
        val sorted = listOf(
            artist("Zed", songs = 30),
            artist("Ada", songs = 90),
            artist("Bo", songs = 30),
        ).sortedWith(ArtistSort.SONGS.comparator()).map { it.artist }

        assertEquals(listOf("Ada", "Bo", "Zed"), sorted)
    }
}
