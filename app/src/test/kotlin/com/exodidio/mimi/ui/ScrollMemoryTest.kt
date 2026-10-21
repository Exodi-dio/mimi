package com.exodidio.mimi.ui

import com.exodidio.mimi.library.AlbumSort
import com.exodidio.mimi.library.ArtistSort
import com.exodidio.mimi.library.Settings
import com.exodidio.mimi.library.SongSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScrollMemoryTest {

    @Test
    fun `a different order is a different list to remember`() {
        val ascending = libraryScrollKey(
            LibrarySection.SONGS,
            Settings(songSort = SongSort.TITLE, sortDescending = false),
        )
        val descending = libraryScrollKey(
            LibrarySection.SONGS,
            Settings(songSort = SongSort.TITLE, sortDescending = true),
        )
        val byAlbum = libraryScrollKey(
            LibrarySection.SONGS,
            Settings(songSort = SongSort.ALBUM, sortDescending = false),
        )

        assertNotEquals(ascending, descending)
        assertNotEquals(ascending, byAlbum)
    }

    @Test
    fun `sections keep separate positions`() {
        val settings = Settings()

        assertNotEquals(
            libraryScrollKey(LibrarySection.SONGS, settings),
            libraryScrollKey(LibrarySection.ALBUMS, settings),
        )
    }

    @Test
    fun `the hidden names are part of the key whatever their order`() {
        val one = Settings(hiddenArtists = listOf("Bea", "ada"))
        val other = Settings(hiddenArtists = listOf("Ada", "bea"))

        assertEquals(
            libraryScrollKey(LibrarySection.ARTISTS, one),
            libraryScrollKey(LibrarySection.ARTISTS, other),
        )
    }

    @Test
    fun `hiding something starts the section at the top`() {
        val plain = Settings()
        val hidden = Settings(hiddenAlbums = listOf("Blue"))

        assertNotEquals(
            libraryScrollKey(LibrarySection.ALBUMS, plain),
            libraryScrollKey(LibrarySection.ALBUMS, hidden),
        )
    }

    @Test
    fun `the album and artist keys follow their own sort settings`() {
        val default = Settings()
        val reversedAlbums = Settings(albumSort = AlbumSort.SONGS, sortDescending = true)
        val reversedArtists = Settings(artistSort = ArtistSort.ALBUMS)

        assertNotEquals(
            libraryScrollKey(LibrarySection.ALBUMS, default),
            libraryScrollKey(LibrarySection.ALBUMS, reversedAlbums),
        )
        assertNotEquals(
            libraryScrollKey(LibrarySection.ARTISTS, default),
            libraryScrollKey(LibrarySection.ARTISTS, reversedArtists),
        )
    }
}
