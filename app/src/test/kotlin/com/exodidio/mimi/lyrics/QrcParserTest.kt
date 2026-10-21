package com.exodidio.mimi.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrcParserTest {

    @Test
    fun `a line with words keeps the time of the line and of every word`() {
        val lyrics = QrcParser.parse(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <Lyrics_1 LyricsType="1">
              <Lyric_0 Start="0" End="4200">Hello <Lyric_1 Start="0" End="900">Hello </Lyric_1><Lyric_2 Start="900" End="4200">world</Lyric_2></Lyric_0>
            </Lyrics_1>
            """.trimIndent(),
        )

        val line = lyrics.lines.single()
        assertEquals(0L, line.timeMs)
        assertEquals("Hello world", line.text)
        assertEquals(listOf("Hello ", "world"), line.words.map { it.text })
        assertEquals(listOf(0L, 900L), line.words.map { it.startMs })
        assertEquals(listOf(900L, 4_200L), line.words.map { it.endMs })
    }

    @Test
    fun `a line without words is still a line`() {
        val lyrics = QrcParser.parse(
            """<Lyrics_1 LyricsType="1"><Lyric_0 Start="1000" End="3000">Just words</Lyric_0></Lyrics_1>""",
        )

        val line = lyrics.lines.single()
        assertEquals(1_000L, line.timeMs)
        assertEquals("Just words", line.text)
        assertTrue(line.words.isEmpty())
    }

    @Test
    fun `a set of lines without timing comes back untimed`() {
        val lyrics = QrcParser.parse(
            """
            <Lyrics_1 LyricsType="2">
              <Lyric_0 Start="0" End="0">First line</Lyric_0>
              <Lyric_1 Start="0" End="0">Second line</Lyric_1>
            </Lyrics_1>
            """.trimIndent(),
        )

        assertEquals(listOf("First line", "Second line"), lyrics.lines.map { it.text })
        assertEquals(listOf(null, null), lyrics.lines.map { it.timeMs })
        assertTrue(lyrics.lines.all { it.words.isEmpty() })
        assertFalse(lyrics.synced)
    }

    @Test
    fun `the lines come back in the order they are sung in`() {
        val lyrics = QrcParser.parse(
            """
            <Lyrics_1 LyricsType="1">
              <Lyric_0 Start="5000" End="6000">Later</Lyric_0>
              <Lyric_1 Start="1000" End="2000">Earlier</Lyric_1>
            </Lyrics_1>
            """.trimIndent(),
        )

        assertEquals(listOf("Earlier", "Later"), lyrics.lines.map { it.text })
        assertEquals(listOf(1_000L, 5_000L), lyrics.lines.map { it.timeMs })
    }

    @Test
    fun `a word without a time is left out of its line`() {
        val lyrics = QrcParser.parse(
            """
            <Lyrics_1 LyricsType="1">
              <Lyric_0 Start="0" End="1000">Hi <Lyric_1 Start="0" End="500">Hi </Lyric_1><Lyric_2>there</Lyric_2></Lyric_0>
            </Lyrics_1>
            """.trimIndent(),
        )

        val line = lyrics.lines.single()
        assertEquals(1, line.words.size)
        assertEquals("Hi ", line.text)
    }

    @Test
    fun `an empty file has nothing in it`() {
        assertTrue(QrcParser.parse("").isEmpty)
        assertTrue(QrcParser.parse("   \n  ").isEmpty)
        assertTrue(QrcParser.parse("""<Lyrics_1 LyricsType="1"></Lyrics_1>""").isEmpty)
    }

    @Test
    fun `a broken file gives nothing rather than an error`() {
        assertTrue(QrcParser.parse("""<Lyrics_1 LyricsType="1"><Lyric_0 Start=""").isEmpty)
        assertTrue(QrcParser.parse("not xml at all").isEmpty)
        assertTrue(QrcParser.parse("[00:01.00]An LRC line").isEmpty)
    }
}
