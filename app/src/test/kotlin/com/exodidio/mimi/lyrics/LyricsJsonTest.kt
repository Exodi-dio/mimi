package com.exodidio.mimi.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsJsonTest {

    @Test
    fun `a search answer of several songs reads every one`() {
        val raw = """
            [
              {"id": 1, "plainLyrics": "first line\nsecond line", "syncedLyrics": null},
              {"id": 2, "plainLyrics": "other words"}
            ]
        """.trimIndent()

        val objects = LyricsJson.objects(raw)

        assertEquals(2, objects.size)
        assertEquals("first line\nsecond line", objects[0]["plainLyrics"])
        assertEquals("other words", objects[1]["plainLyrics"])
    }

    @Test
    fun `a single song reads as one`() {
        val raw = """{"plainLyrics": "just the one"}"""

        val objects = LyricsJson.objects(raw)

        assertEquals(1, objects.size)
        assertEquals("just the one", objects[0]["plainLyrics"])
    }

    @Test
    fun `a null reads as no value rather than the word null`() {
        val raw = """{"syncedLyrics": null, "plainLyrics": "words"}"""

        val objects = LyricsJson.objects(raw)

        assertEquals(null, objects[0]["syncedLyrics"])
        assertEquals("words", objects[0]["plainLyrics"])
    }

    @Test
    fun `escaped quotes and tabs survive`() {
        val raw = """{"plainLyrics": "she said \"wait\"\tthen left"}"""

        assertEquals("she said \"wait\"\tthen left", LyricsJson.objects(raw)[0]["plainLyrics"])
    }

    @Test
    fun `a unicode escape becomes the character it names`() {
        val raw = """{"plainLyrics": "\u4f60\u597d"}"""

        assertEquals("你好", LyricsJson.objects(raw)[0]["plainLyrics"])
    }

    @Test
    fun `objects and arrays inside a song are stepped over`() {
        // The search answer nests a record under another name; nothing inside it is wanted,
        // and walking into it would leave the reader out of step with the object it is on.
        val raw = """
            {"trackName": "Song", "extra": {"nested": "ignored", "deeper": [1, 2, 3]}, "plainLyrics": "words"}
        """.trimIndent()

        val objects = LyricsJson.objects(raw)

        assertEquals(1, objects.size)
        assertEquals("Song", objects[0]["trackName"])
        assertEquals("words", objects[0]["plainLyrics"])
        assertEquals(null, objects[0]["nested"])
    }

    @Test
    fun `numbers and booleans are stepped over`() {
        val raw = """{"id": 12345, "instrumental": false, "plainLyrics": "words"}"""

        val objects = LyricsJson.objects(raw)

        assertEquals("words", objects[0]["plainLyrics"])
    }

    @Test
    fun `a cut off answer keeps what it managed to read`() {
        // A connection dropped mid answer is a real answer, just a short one. Throwing would
        // turn a half useful result into none at all.
        val raw = """[{"plainLyrics": "words"}, {"plainLyrics": "more"""

        val objects = LyricsJson.objects(raw)

        assertEquals(1, objects.size)
        assertEquals("words", objects[0]["plainLyrics"])
    }

    @Test
    fun `nothing worth reading gives nothing back`() {
        assertTrue(LyricsJson.objects("").isEmpty())
        assertTrue(LyricsJson.objects("not json at all").isEmpty())
        assertTrue(LyricsJson.objects("[]").isEmpty())
    }
}
