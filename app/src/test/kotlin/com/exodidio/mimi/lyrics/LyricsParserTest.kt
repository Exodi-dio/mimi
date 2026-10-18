package com.exodidio.mimi.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {

    @Test
    fun `timed lines come back in time order, whatever order they were written in`() {
        val lyrics = LyricsParser.parse("[00:12.00]Second\n[00:01.50]First")

        assertEquals(
            listOf(1_500L to "First", 12_000L to "Second"),
            lyrics.lines.map { it.timeMs to it.text },
        )
        assertTrue(lyrics.synced)
    }

    @Test
    fun `one line may carry several stamps`() {
        val lyrics = LyricsParser.parse("[00:10.00][01:20.00]Chorus")

        assertEquals(
            listOf(10_000L to "Chorus", 80_000L to "Chorus"),
            lyrics.lines.map { it.timeMs to it.text },
        )
    }

    @Test
    fun `a two digit fraction is hundredths and a one digit fraction is tenths`() {
        val lyrics = LyricsParser.parse("[00:01.5]Tenths\n[00:02.25]Hundredths\n[00:03]Whole")

        assertEquals(
            listOf(1_500L, 2_250L, 3_000L),
            lyrics.lines.map { it.timeMs },
        )
    }

    @Test
    fun `an offset shifts every line`() {
        val lyrics = LyricsParser.parse("[offset:+500]\n[00:10.00]Late")

        assertEquals(10_500L, lyrics.lines.single().timeMs)
    }

    @Test
    fun `a negative offset shifts the other way`() {
        val lyrics = LyricsParser.parse("[offset:-250]\n[00:10.00]Early")

        assertEquals(9_750L, lyrics.lines.single().timeMs)
    }

    @Test
    fun `timed lines do not drag their headers along`() {
        val lyrics = LyricsParser.parse(
            """
            [ar:Someone]
            [ti:A song]
            [al:An album]
            [00:01.00]Only the words
            """.trimIndent(),
        )

        assertEquals(listOf("Only the words"), lyrics.lines.map { it.text })
    }

    @Test
    fun `words without any timing are kept as they were written`() {
        val lyrics = LyricsParser.parse("First line\nSecond line\n\nThird line")

        assertEquals(listOf("First line", "Second line", "Third line"), lyrics.lines.map { it.text })
        assertEquals(listOf(null, null, null), lyrics.lines.map { it.timeMs })
        assertFalse(lyrics.synced)
    }

    @Test
    fun `a section marker is a line of the song, not a header`() {
        val lyrics = LyricsParser.parse("Plain words\n[Chorus]\nMore words")

        assertEquals(listOf("Plain words", "[Chorus]", "More words"), lyrics.lines.map { it.text })
    }

    @Test
    fun `enhanced word timings leave the words behind`() {
        val lyrics = LyricsParser.parse("[00:05.00]<00:05.00>Hold <00:05.50>me <00:06.00>close")

        assertEquals(listOf(5_000L to "Hold me close"), lyrics.lines.map { it.timeMs to it.text })
    }

    @Test
    fun `carriage returns are not treated as part of a line`() {
        val lyrics = LyricsParser.parse("[00:01.00]First\r\n[00:02.00]Second\r\n")

        assertEquals(listOf("First", "Second"), lyrics.lines.map { it.text })
    }

    @Test
    fun `a stamp with no words after it is not a line`() {
        val lyrics = LyricsParser.parse("[00:01.00]Real\n[00:02.00]")

        assertEquals(listOf("Real"), lyrics.lines.map { it.text })
    }

    @Test
    fun `blank input has nothing in it`() {
        assertTrue(LyricsParser.parse("").isEmpty)
        assertTrue(LyricsParser.parse("   \n  \n").isEmpty)
    }

    @Test
    fun `indexAt follows the clock and gives nothing before the first line`() {
        val lyrics = LyricsParser.parse("[00:10.00]One\n[00:20.00]Two\n[00:30.00]Three")

        assertEquals(-1, lyrics.indexAt(0L))
        assertEquals(-1, lyrics.indexAt(9_999L))
        assertEquals(0, lyrics.indexAt(10_000L))
        assertEquals(0, lyrics.indexAt(19_999L))
        assertEquals(1, lyrics.indexAt(20_000L))
        assertEquals(2, lyrics.indexAt(999_999L))
    }

    @Test
    fun `indexAt has nothing to say about untimed words`() {
        val lyrics = LyricsParser.parse("Just words")

        assertEquals(-1, lyrics.indexAt(60_000L))
    }
}
