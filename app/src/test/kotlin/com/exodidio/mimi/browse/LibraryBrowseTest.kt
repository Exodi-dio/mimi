package com.exodidio.mimi.browse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBrowseTest {

    @Test
    fun `the root holds the three ways into the library`() {
        val browse = LibraryBrowse(songs())

        val children = browse.childrenOf(LibraryBrowse.ROOT_ID)

        assertEquals(listOf("Songs", "Albums", "Artists"), children.map { it.title })
        assertTrue(children.all { it.browsable })
        assertTrue(children.none { it.playable })
    }

    @Test
    fun `the songs shelf lists every song and can be played`() {
        val browse = LibraryBrowse(songs())

        val children = browse.childrenOf(LibraryBrowse.SONGS_ID)

        assertEquals(listOf("First", "Second", "Third"), children.map { it.title })
        assertTrue(children.all { it.playable })
        assertTrue(children.none { it.browsable })
        assertEquals("Ada", children.first { it.title == "First" }.artist)
        assertEquals(180_000L, children.first { it.title == "First" }.durationMs)
    }

    @Test
    fun `an album holds its own songs in the order the record runs them`() {
        val browse = LibraryBrowse(songs())

        val children = browse.childrenOf("${LibraryBrowse.ALBUM_PREFIX}7")

        assertEquals(listOf("First", "Second"), children.map { it.title })
    }

    @Test
    fun `an album is listed once and named after the record`() {
        val browse = LibraryBrowse(songs())

        val albums = browse.childrenOf(LibraryBrowse.ALBUMS_ID)

        assertEquals(listOf("Blue", "Red"), albums.map { it.title })
        assertEquals("${LibraryBrowse.ALBUM_PREFIX}7", albums.first().mediaId)
    }

    @Test
    fun `an artist holds the songs that name it`() {
        val browse = LibraryBrowse(songs())

        val children = browse.childrenOf("${LibraryBrowse.ARTIST_PREFIX}Ada")

        assertEquals(listOf("First", "Second"), children.map { it.title })
    }

    @Test
    fun `a credit is split into one shelf per name`() {
        val browse = LibraryBrowse(
            listOf(song(id = 1, title = "Duet", artist = "Ada & Bea", albumId = 3L)),
            artistSeparators = listOf("&"),
        )

        val artists = browse.childrenOf(LibraryBrowse.ARTISTS_ID)

        assertEquals(listOf("Ada", "Bea"), artists.map { it.title })
        assertEquals(listOf("Duet"), browse.childrenOf("${LibraryBrowse.ARTIST_PREFIX}Bea").map { it.title })
    }

    @Test
    fun `an id the tree does not hold has nothing under it`() {
        val browse = LibraryBrowse(songs())

        assertEquals(emptyList<BrowseEntry>(), browse.childrenOf("nowhere"))
    }

    @Test
    fun `a song is found by the uri the player knows it by`() {
        val browse = LibraryBrowse(songs())

        assertEquals("First", browse.song("content://media/audio/1")?.title)
        assertNull(browse.song("content://media/audio/99"))
    }

    @Test
    fun `an entry is found by the id a browser asks about`() {
        val browse = LibraryBrowse(songs())

        assertEquals("Songs", browse.entry(LibraryBrowse.SONGS_ID)?.title)
        assertEquals("Blue", browse.entry("${LibraryBrowse.ALBUM_PREFIX}7")?.title)
        assertEquals("Ada", browse.entry("${LibraryBrowse.ARTIST_PREFIX}Ada")?.title)
        assertEquals("First", browse.entry("content://media/audio/1")?.title)
        assertNull(browse.entry("nowhere"))
    }

    private fun songs(): List<BrowseSong> = listOf(
        song(id = 1, title = "First", artist = "Ada", album = "Blue", albumId = 7L, track = 1),
        song(id = 2, title = "Second", artist = "Ada", album = "Blue", albumId = 7L, track = 2),
        song(id = 3, title = "Third", artist = "Bea", album = "Red", albumId = 8L, track = 1),
    )

    private fun song(
        id: Long,
        title: String,
        artist: String? = null,
        album: String? = null,
        albumId: Long = 1L,
        track: Int = 1,
    ) = BrowseSong(
        mediaId = "content://media/audio/$id",
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        trackNumber = track,
        durationMs = 180_000L,
        artworkUri = "content://media/external/audio/albumart/$albumId",
    )
}
