package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uTest {

    @Test
    fun `a playlist opens with the header other players look for`() {
        val text = buildM3u(
            listOf(M3uSong(path = "/music/one.mp3", title = "One", artist = "A", durationMs = 210_000L))
        )

        assertTrue(text.startsWith("#EXTM3U\n"))
    }

    @Test
    fun `a song is written with its length, its name and its path`() {
        val text = buildM3u(
            listOf(
                M3uSong(
                    path = "/music/one.mp3",
                    title = "One",
                    artist = "A",
                    durationMs = 210_500L,
                )
            )
        )

        assertTrue(text.contains("#EXTINF:210,A - One\n"))
        assertTrue(text.contains("/music/one.mp3\n"))
    }

    @Test
    fun `a song with no artist is listed under its title alone`() {
        val text = buildM3u(listOf(M3uSong(path = "/music/one.mp3", title = "One")))

        assertTrue(text.contains("#EXTINF:0,One\n"))
    }

    @Test
    fun `a playlist is read back in the order it was written`() {
        val entries = parseM3u(
            buildM3u(
                listOf(
                    M3uSong("/music/one.mp3", "One", "A", 210_000L),
                    M3uSong("/music/two.mp3", "Two", "B", 180_000L),
                )
            )
        )

        assertEquals(listOf("/music/one.mp3", "/music/two.mp3"), entries.map { it.path })
        assertEquals(listOf("One", "Two"), entries.map { it.title })
        assertEquals(listOf("A", "B"), entries.map { it.artist })
        assertEquals(listOf(210, 180), entries.map { it.seconds })
    }

    @Test
    fun `a file with headers, extended headers and plain paths is read`() {
        val entries = parseM3u(
            """
            #EXTM3U
            #EXTINF:210,A - One
            /music/one.mp3
            #EXTINF:-1,Just A Title
            /music/two.mp3
            /music/three.mp3
            """.trimIndent()
        )

        assertEquals(3, entries.size)
        assertEquals("One", entries[0].title)
        assertEquals("A", entries[0].artist)
        assertEquals(210, entries[0].seconds)
        assertEquals("Just A Title", entries[1].title)
        assertNull(entries[1].artist)
        assertNull(entries[2].title)
    }

    @Test
    fun `a directive the app has no use for is passed over`() {
        val entries = parseM3u(
            """
            #EXTM3U
            #EXTVLCOPT:http-referrer=example
            #EXTINF:210,A - One
            /music/one.mp3
            """.trimIndent()
        )

        assertEquals(1, entries.size)
        assertEquals("/music/one.mp3", entries[0].path)
    }

    @Test
    fun `an empty file is a playlist of nothing`() {
        assertTrue(parseM3u("").isEmpty())
        assertTrue(parseM3u("\n\n").isEmpty())
    }

    @Test
    fun `a header with no path after it is forgotten`() {
        val entries = parseM3u(
            """
            #EXTM3U
            #EXTINF:210,A - One
            #EXTINF:180,B - Two
            /music/two.mp3
            """.trimIndent()
        )

        assertEquals(1, entries.size)
        assertEquals("Two", entries[0].title)
    }

    @Test
    fun `the songs of a playlist are matched against the library by their path`() {
        val files = listOf(
            LibraryFile(trackId = 1L, contentUri = "content://one", path = "/music/one.mp3"),
            LibraryFile(trackId = 2L, contentUri = "content://two", path = "/music/two.mp3"),
        )

        val match = matchM3uEntries(
            parseM3u("/music/one.mp3\n/music/missing.mp3\n/music/two.mp3"),
            files,
        )

        assertEquals(listOf(1L, 2L), match.matched.map { it.trackId })
        assertEquals(1, match.skipped)
    }

    @Test
    fun `a path the device spells differently is still the same song`() {
        val files = listOf(
            LibraryFile(trackId = 1L, contentUri = "content://one", path = "/Music/One.mp3"),
        )

        val match = matchM3uEntries(parseM3u("/music/one.mp3"), files)

        assertEquals(1, match.matched.size)
        assertEquals(1L, match.matched.first().trackId)
    }

    @Test
    fun `a song is matched by its file name when the path is of another device`() {
        val files = listOf(
            LibraryFile(trackId = 7L, contentUri = "content://seven", path = null, fileName = "One.mp3"),
        )

        val match = matchM3uEntries(parseM3u("/elsewhere/One.mp3"), files)

        assertEquals(1, match.matched.size)
        assertEquals(7L, match.matched.first().trackId)
    }

    @Test
    fun `a name several songs share is not matched at all`() {
        val files = listOf(
            LibraryFile(trackId = 1L, contentUri = "content://one", fileName = "One.mp3"),
            LibraryFile(trackId = 2L, contentUri = "content://two", fileName = "one.mp3"),
        )

        val match = matchM3uEntries(parseM3u("/elsewhere/one.mp3"), files)

        assertTrue(match.matched.isEmpty())
        assertEquals(1, match.skipped)
    }

    @Test
    fun `a song of the library is named by its address once it has been matched`() {
        val files = listOf(
            LibraryFile(trackId = 3L, contentUri = "content://three", path = "/music/three.mp3"),
        )

        val match = matchM3uEntries(parseM3u("/music/three.mp3"), files)

        assertEquals("content://three", match.matched.first().contentUri)
    }
}
