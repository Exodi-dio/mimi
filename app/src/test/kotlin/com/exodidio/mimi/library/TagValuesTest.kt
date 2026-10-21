package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagValuesTest {

    @Test
    fun `an empty year is no year rather than the year zero`() {
        assertNull(parseYear(""))
        assertNull(parseYear("   "))
        assertNull(parseYear(null))
        assertNull(parseYear("0"))
    }

    @Test
    fun `a year is read as the number it stands for`() {
        assertEquals(1999, parseYear("1999"))
        assertEquals(1999, parseYear(" 1999 "))
    }

    @Test
    fun `a date written into the year field gives the year it belongs to`() {
        assertEquals(1999, parseYear("1999-05-06"))
    }

    @Test
    fun `a year that is not a number is no year`() {
        assertNull(parseYear("nineteen ninety nine"))
        assertNull(parseYear("-5"))
    }

    @Test
    fun `an empty track number is no number rather than the zeroth one`() {
        assertNull(parseTrackNumber(""))
        assertNull(parseTrackNumber(null))
        assertNull(parseTrackNumber("0"))
    }

    @Test
    fun `a track number is read as the number it stands for`() {
        assertEquals(3, parseTrackNumber("3"))
        assertEquals(3, parseTrackNumber(" 3 "))
    }

    @Test
    fun `a track number written as a fraction is the song's place in the record`() {
        assertEquals(3, parseTrackNumber("3/12"))
    }

    @Test
    fun `the editor refuses a title that is not there`() {
        val input = readTagInput(
            title = "  ",
            artist = "A",
            album = "B",
            albumArtist = "C",
            year = "1999",
            trackNumber = "3",
            genre = "Rock",
        )

        assertTrue(input is TagInput.Invalid)
    }

    @Test
    fun `the editor refuses a year that is not a number`() {
        val input = readTagInput(
            title = "One",
            artist = "",
            album = "",
            albumArtist = "",
            year = "nineteen",
            trackNumber = "",
            genre = "",
        )

        assertTrue(input is TagInput.Invalid)
        assertEquals("The year has to be a number.", (input as TagInput.Invalid).message)
    }

    @Test
    fun `the editor refuses a track number that is not a number`() {
        val input = readTagInput(
            title = "One",
            artist = "",
            album = "",
            albumArtist = "",
            year = "",
            trackNumber = "third",
            genre = "",
        )

        assertTrue(input is TagInput.Invalid)
        assertEquals("The track number has to be a number.", (input as TagInput.Invalid).message)
    }

    @Test
    fun `an empty field in the editor is a field with nothing in it`() {
        val input = readTagInput(
            title = "One",
            artist = "",
            album = "",
            albumArtist = "",
            year = "",
            trackNumber = "",
            genre = "",
        )

        val values = (input as TagInput.Valid).values
        assertEquals("One", values.title)
        assertNull(values.artist)
        assertNull(values.year)
        assertNull(values.trackNumber)
    }

    @Test
    fun `the values the platform reports are read into the tags of a song`() {
        val values = tagValuesFrom(
            mapOf(
                "title" to "One",
                "artist" to "A",
                "album" to "B",
                "album_artist" to "C",
                "year" to "1999",
                "track" to "3/12",
                "genre" to "Rock",
            )
        )

        assertEquals("One", values.title)
        assertEquals("A", values.artist)
        assertEquals("B", values.album)
        assertEquals("C", values.albumArtist)
        assertEquals(1999, values.year)
        assertEquals(3, values.trackNumber)
        assertEquals("Rock", values.genre)
    }

    @Test
    fun `a year of zero from the platform is no year`() {
        val values = tagValuesFrom(mapOf("title" to "One", "year" to "0"))

        assertNull(values.year)
    }

    @Test
    fun `a field the platform did not have is read as nothing`() {
        val values = tagValuesFrom(mapOf("title" to "One"))

        assertNull(values.artist)
        assertNull(values.album)
        assertNull(values.albumArtist)
        assertNull(values.genre)
        assertNull(values.trackNumber)
    }

    @Test
    fun `only the fields that changed are written back`() {
        val before = TagValues(title = "One", artist = "A", album = "B", year = 1999)
        val after = before.copy(title = "Two")

        assertEquals(setOf(TagField.TITLE), after.differencesFrom(before))
    }

    @Test
    fun `a year that was emptied is a change`() {
        val before = TagValues(title = "One", year = 1999)
        val after = before.copy(year = null)

        assertEquals(setOf(TagField.YEAR), after.differencesFrom(before))
    }

    @Test
    fun `the write is believed when the platform reports what was asked for`() {
        val asked = TagValues(title = "One", year = 1999, trackNumber = 3)
        val reported = TagValues(title = "One", year = 1999, trackNumber = 3)

        assertTrue(readBackMatches(asked, reported, setOf(TagField.TITLE, TagField.YEAR)))
    }

    @Test
    fun `the write is not believed when the platform reports something else`() {
        val asked = TagValues(title = "One", year = 1999)
        val reported = TagValues(title = "One", year = 1998)

        assertTrue(!readBackMatches(asked, reported, setOf(TagField.YEAR)))
    }

    @Test
    fun `a field that was not touched is not held against the write`() {
        val asked = TagValues(title = "One", genre = "Rock")
        val reported = TagValues(title = "One", genre = null)

        assertTrue(readBackMatches(asked, reported, setOf(TagField.TITLE)))
    }

    @Test
    fun `the tags of a song are read from the library as it holds them`() {
        val track = Track(
            id = 1L,
            title = "One",
            artist = "A",
            album = "B",
            albumId = 2L,
            durationMs = 1_000L,
            contentUri = "content://one",
            dateModified = 0L,
            sizeBytes = 1L,
            mimeType = null,
            trackNumber = 0,
        )

        val values = track.tagValues()

        assertEquals("One", values.title)
        assertNull(values.trackNumber)
        assertNull(values.year)
    }
}
