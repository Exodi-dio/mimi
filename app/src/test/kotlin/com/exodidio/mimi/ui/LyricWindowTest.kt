package com.exodidio.mimi.ui

import com.exodidio.mimi.lyrics.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricWindowTest {

    @Test
    fun `the window holds the sung line and three either side`() {
        val window = lyricWindow(lines(20), currentIndex = 10)

        assertEquals(listOf(7, 8, 9, 10, 11, 12, 13), window.map { it.index })
    }

    @Test
    fun `the distance of a line is how many lines it sits from the sung one`() {
        val window = lyricWindow(lines(20), currentIndex = 10)

        assertEquals(listOf(3, 2, 1, 0, 1, 2, 3), window.map { it.distance })
    }

    @Test
    fun `the window never runs past the start of the song`() {
        val window = lyricWindow(lines(20), currentIndex = 1)

        assertEquals(listOf(0, 1, 2, 3, 4), window.map { it.index })
    }

    @Test
    fun `the window never runs past the end of the song`() {
        val window = lyricWindow(lines(20), currentIndex = 18)

        assertEquals(listOf(15, 16, 17, 18, 19), window.map { it.index })
    }

    @Test
    fun `a song that has not started yet shows its opening lines`() {
        val window = lyricWindow(lines(20), currentIndex = -1)

        assertEquals(listOf(0, 1, 2, 3), window.map { it.index })
        assertEquals(listOf(0, 1, 2, 3), window.map { it.distance })
    }

    @Test
    fun `a song with no lines has no window`() {
        assertTrue(lyricWindow(emptyList(), currentIndex = 0).isEmpty())
    }

    @Test
    fun `the sung line is the strongest and the rest fade away`() {
        assertEquals(1f, lyricAlpha(0), 0.001f)
        assertEquals(0.5f, lyricAlpha(1), 0.001f)
        assertEquals(0.28f, lyricAlpha(2), 0.001f)
        assertEquals(0.14f, lyricAlpha(3), 0.001f)
    }

    @Test
    fun `a line beyond the window is not drawn at all`() {
        assertEquals(0f, lyricAlpha(4), 0.001f)
        assertEquals(0f, lyricAlpha(9), 0.001f)
    }

    private fun lines(count: Int): List<LyricLine> = List(count) { index ->
        LyricLine(timeMs = index * 1_000L, text = "line $index")
    }
}
