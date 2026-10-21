package com.exodidio.mimi.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsFormatsTest {

    @Test
    fun `a file that announces itself as xml is read as qrc`() {
        val lyrics = LyricsFormats.parse(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <Lyrics_1 LyricsType="1">
              <Lyric_0 Start="0" End="1000">Hi <Lyric_1 Start="0" End="1000">Hi </Lyric_1></Lyric_0>
            </Lyrics_1>
            """.trimIndent(),
        )

        assertEquals(1, lyrics.lines.size)
        assertEquals(1, lyrics.lines.single().words.size)
    }

    @Test
    fun `a file that carries the element of a qrc is read as qrc`() {
        val lyrics = LyricsFormats.parse(
            """<Lyrics_1 LyricsType="1"><Lyric_0 Start="0" End="1000">Hi </Lyric_0></Lyrics_1>""",
        )

        assertEquals("Hi", lyrics.lines.single().text)
    }

    @Test
    fun `a file that does not look like xml is read as lrc`() {
        val lyrics = LyricsFormats.parse("[00:01.00]Hi")

        assertEquals(1_000L, lyrics.lines.single().timeMs)
        assertTrue(lyrics.lines.single().words.isEmpty())
    }

    @Test
    fun `the shape of the text says which format it is`() {
        assertTrue(LyricsFormats.isQrc("<?xml version=\"1.0\"?><Lyrics_1></Lyrics_1>"))
        assertTrue(LyricsFormats.isQrc("<Lyrics_1 LyricsType=\"1\"></Lyrics_1>"))
        assertFalse(LyricsFormats.isQrc("[00:01.00]Hi"))
        assertFalse(LyricsFormats.isQrc("Just words"))
    }
}
