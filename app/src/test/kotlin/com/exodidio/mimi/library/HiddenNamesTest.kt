package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenNamesTest {

    @Test
    fun `nothing is filtered when no name is hidden`() {
        val songs = listOf(track(1, album = "Blue"), track(2, album = "Red"))

        assertEquals(songs, songs.tracksWithoutHidden(HiddenNames()))
    }

    @Test
    fun `an album is left out whatever its capitals`() {
        val songs = listOf(track(1, album = "Blue"), track(2, album = "Red"))
        val hidden = HiddenNames(albums = setOf("blue"))

        assertEquals(listOf(2L), songs.tracksWithoutHidden(hidden).map { it.id })
    }

    @Test
    fun `a split credit hides every song that names that artist`() {
        val songs = listOf(
            track(1, artist = "Ada & Bea"),
            track(2, artist = "Ada"),
            track(3, artist = "Cy"),
        )
        val hidden = HiddenNames(artists = setOf("bea"))

        assertEquals(listOf(2L, 3L), songs.tracksWithoutHidden(hidden).map { it.id })
    }

    @Test
    fun `the album artist credit counts as well as the track artist`() {
        val songs = listOf(
            track(1, artist = "Various", albumArtist = "Ada"),
            track(2, artist = "Cy"),
        )
        val hidden = HiddenNames(artists = setOf("ada"))

        assertEquals(listOf(2L), songs.tracksWithoutHidden(hidden).map { it.id })
    }

    @Test
    fun `a song with no album is not hidden by an empty album name`() {
        val songs = listOf(track(1, album = null))
        val hidden = HiddenNames(albums = setOf(""))

        assertFalse(songs.single().isHiddenBy(hidden))
    }

    @Test
    fun `hiding a name twice keeps one row`() {
        val first = emptyList<String>().addingHiddenName("Ada")
        val second = first.addingHiddenName(" ada ")

        assertEquals(listOf("Ada"), second)
    }

    @Test
    fun `a blank name is not added`() {
        assertEquals(listOf("Ada"), listOf("Ada").addingHiddenName("   "))
    }

    @Test
    fun `unhiding a name ignores case and spacing`() {
        assertEquals(emptyList<String>(), listOf("Ada").removingHiddenName(" ada "))
        assertEquals(listOf("Bea"), listOf("Ada", "Bea").removingHiddenName("ADA"))
    }

    @Test
    fun `the stored list survives a round trip`() {
        val names = listOf("Ada", "Bea & Cy")

        assertEquals(names, decodeHiddenNames(encodeHiddenNames(names)))
    }

    @Test
    fun `an album row is hidden when its artist is`() {
        val album = AlbumSummary(
            albumId = 1L,
            album = "Blue",
            artist = "Ada & Bea",
            trackCount = 3,
            artworkUri = null,
        )

        assertTrue(album.isHiddenBy(HiddenNames(artists = setOf("bea"))))
        assertFalse(album.isHiddenBy(HiddenNames(artists = setOf("cy"))))
    }

    @Test
    fun `an artist row is hidden by its own name`() {
        val artist = ArtistSummary(artist = "Ada", trackCount = 2, albumCount = 1)

        assertTrue(artist.isHiddenBy(HiddenNames(artists = setOf("ada"))))
        assertFalse(artist.isHiddenBy(HiddenNames()))
    }

    private fun track(
        id: Long,
        artist: String? = null,
        albumArtist: String? = null,
        album: String? = null,
    ) = Track(
        id = id,
        title = "Song $id",
        artist = artist,
        albumArtist = albumArtist,
        album = album,
        albumId = 0L,
        durationMs = 180_000L,
        contentUri = "content://media/audio/$id",
        dateModified = 0L,
        sizeBytes = 1_000L,
        mimeType = "audio/mpeg",
        trackNumber = 1,
    )
}
