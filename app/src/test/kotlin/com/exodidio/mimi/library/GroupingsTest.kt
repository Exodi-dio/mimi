package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupingsTest {

    @Test
    fun `albums are counted from the songs that name them`() {
        val songs = listOf(
            track(1, album = "Blue", albumId = 7L),
            track(2, album = "Blue", albumId = 7L),
            track(3, album = "Red", albumId = 8L),
        )

        val albums = songs.albumSummaries()

        assertEquals(listOf("Blue", "Red"), albums.map { it.album })
        assertEquals(listOf(2, 1), albums.map { it.trackCount })
    }

    @Test
    fun `a song without an album makes no album row`() {
        val songs = listOf(track(1, album = null, albumId = 0L))

        assertEquals(emptyList<AlbumSummary>(), songs.albumSummaries())
    }

    @Test
    fun `an artist credit is split into one row per name`() {
        val songs = listOf(
            track(1, artist = "Ada & Bea", albumId = 1L),
            track(2, artist = "Ada", albumId = 2L),
        )

        val artists = songs.artistSummaries(Settings.DEFAULT_CREDIT_SEPARATORS)

        assertEquals(listOf("Ada", "Bea"), artists.map { it.artist })
        assertEquals(listOf(2, 1), artists.map { it.trackCount })
        assertEquals(listOf(2, 1), artists.map { it.albumCount })
    }

    @Test
    fun `genres split on the same separators as artists`() {
        val songs = listOf(
            track(1, genre = "Jazz; Soul"),
            track(2, genre = "Jazz"),
        )

        val genres = songs.genreSummaries(Settings.DEFAULT_CREDIT_SEPARATORS)

        assertEquals(listOf("Jazz", "Soul"), genres.map { it.genre })
        assertEquals(listOf(2, 1), genres.map { it.trackCount })
    }

    @Test
    fun `composers are shown as tagged and trimmed`() {
        val songs = listOf(
            track(1, composer = " Ada "),
            track(2, composer = "Bea"),
        )

        val composers = songs.composerSummaries()

        assertEquals(listOf("Ada", "Bea"), composers.map { it.composer })
    }

    @Test
    fun `years read newest first`() {
        val songs = listOf(
            track(1, year = 1999),
            track(2, year = 2010),
            track(3, year = 1999),
        )

        val years = songs.yearSummaries()

        assertEquals(listOf(2010, 1999), years.map { it.year })
        assertEquals(listOf(1, 2), years.map { it.trackCount })
    }

    @Test
    fun `folders keep their whole path so two names stay apart`() {
        val songs = listOf(
            track(1, folder = "Music/Live"),
            track(2, folder = "Recordings/Live"),
        )

        val folders = songs.folderSummaries()

        assertEquals(listOf("Music/Live", "Recordings/Live"), folders.map { it.folder })
    }

    @Test
    fun `a blank composer or folder makes no row`() {
        val songs = listOf(track(1, composer = "  ", folder = "  "))

        assertEquals(emptyList<ComposerSummary>(), songs.composerSummaries())
        assertEquals(emptyList<FolderSummary>(), songs.folderSummaries())
    }

    private fun track(
        id: Long,
        artist: String? = null,
        album: String? = null,
        albumId: Long = 1L,
        genre: String? = null,
        composer: String? = null,
        year: Int? = null,
        folder: String? = null,
    ) = Track(
        id = id,
        title = "Song $id",
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = 180_000L,
        contentUri = "content://media/audio/$id",
        dateModified = 0L,
        sizeBytes = 1_000L,
        mimeType = "audio/mpeg",
        trackNumber = 1,
        year = year,
        genre = genre,
        composer = composer,
        folder = folder,
    )
}
