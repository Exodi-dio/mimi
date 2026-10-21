package com.exodidio.mimi.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KaraokeSweepTest {

    @Test
    fun `a word half way through its own time is half lit`() {
        val words = sweep(line(word("Hello ", 0L, 1_000L)), 500L)

        assertEquals(0.5f, words.single().brightness, 0.001f)
        assertTrue(words.single().current)
    }

    @Test
    fun `a word before its time is not lit at all`() {
        val words = sweep(line(word("Hello ", 1_000L, 2_000L)), 500L)

        assertEquals(0f, words.single().brightness, 0.001f)
        assertFalse(words.single().current)
    }

    @Test
    fun `a word after its time is fully lit`() {
        val words = sweep(line(word("Hello ", 0L, 1_000L)), 2_000L)

        assertEquals(1f, words.single().brightness, 0.001f)
        assertFalse(words.single().current)
    }

    @Test
    fun `the words of a line are drawn in the order they are sung in`() {
        val words = sweep(line(word("One ", 0L, 500L), word("two", 500L, 1_000L)), 250L)

        assertEquals(listOf("One", "two"), words.map { it.text })
        assertEquals(0.5f, words.first().brightness, 0.001f)
        assertEquals(0f, words.last().brightness, 0.001f)
    }

    @Test
    fun `the space between two words stays between them`() {
        val words = sweep(line(word("One ", 0L, 500L), word("two", 500L, 1_000L)), 250L)

        assertEquals("", words.first().gap)
        assertEquals(" ", words.last().gap)
    }

    @Test
    fun `a line read from right to left is drawn in the other order`() {
        val words = sweep(
            LyricLine(
                timeMs = 0L,
                text = "مرحبا بالعالم",
                words = listOf(word("مرحبا ", 0L, 1_000L), word("بالعالم", 1_000L, 2_000L)),
            ),
            500L,
        )

        assertEquals(listOf("بالعالم", "مرحبا"), words.map { it.text })
        // The word sung first is the one on the right, which is the last of the words
        // as they are drawn.
        assertEquals(0.5f, words.last().brightness, 0.001f)
        assertEquals(0f, words.first().brightness, 0.001f)
    }

    @Test
    fun `a particle written onto a word belongs to it`() {
        val words = sweep(
            LyricLine(
                timeMs = 0L,
                text = "나는",
                words = listOf(word("나", 0L, 500L), word("는", 500L, 1_000L)),
            ),
            500L,
        )

        assertEquals(1, words.size)
        assertEquals("나는", words.single().text)
        assertEquals(0.5f, words.single().brightness, 0.001f)
        assertTrue(words.single().current)
    }

    @Test
    fun `the pieces of a word are joined so that a line cannot break it`() {
        val words = sweep(
            LyricLine(
                timeMs = 0L,
                text = "나는",
                words = listOf(word("나", 0L, 500L), word("는", 500L, 1_000L)),
            ),
            500L,
        )

        assertEquals("나\u2060는", words.single().drawn)
    }

    @Test
    fun `a particle that stands on its own is a word of its own`() {
        val words = sweep(
            LyricLine(
                timeMs = 0L,
                text = "나 는",
                words = listOf(word("나 ", 0L, 500L), word("는", 500L, 1_000L)),
            ),
            250L,
        )

        assertEquals(2, words.size)
        assertEquals(listOf("나", "는"), words.map { it.text })
    }

    @Test
    fun `a line without word timings has nothing to sweep`() {
        assertTrue(KaraokeSweep.sweep(LyricLine(timeMs = 0L, text = "Just words"), 500L).isEmpty())
    }

    @Test
    fun `the direction of a line comes from its first letter`() {
        assertTrue(KaraokeSweep.isRightToLeft("مرحبا"))
        assertTrue(KaraokeSweep.isRightToLeft("שלום"))
        assertFalse(KaraokeSweep.isRightToLeft("Hello"))
        assertFalse(KaraokeSweep.isRightToLeft("안녕하세요"))
    }

    private fun word(text: String, startMs: Long, endMs: Long): LyricWord =
        LyricWord(text = text, startMs = startMs, endMs = endMs)

    private fun line(vararg words: LyricWord): LyricLine = LyricLine(
        timeMs = 0L,
        text = words.joinToString("") { it.text },
        words = words.toList(),
    )

    private fun sweep(line: LyricLine, positionMs: Long): List<SweptWord> =
        KaraokeSweep.sweep(line, positionMs)
}
