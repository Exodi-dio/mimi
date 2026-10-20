package com.exodidio.mimi.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class FadeLevelTest {

    private val fade = 10_000L

    @Test
    fun `before the fade starts nothing is turned down`() {
        assertEquals(1f, fadeLevel(30_000L, fade), 0.001f)
        assertEquals(1f, fadeLevel(fade, fade), 0.001f)
    }

    @Test
    fun `halfway through the fade the player is halfway down`() {
        assertEquals(0.5f, fadeLevel(5_000L, fade), 0.001f)
    }

    @Test
    fun `the last second is nearly silent but not silent`() {
        assertEquals(0.1f, fadeLevel(1_000L, fade), 0.001f)
    }

    @Test
    fun `at the end there is nothing left`() {
        assertEquals(0f, fadeLevel(0L, fade), 0.001f)
        assertEquals(0f, fadeLevel(-4_000L, fade), 0.001f)
    }

    @Test
    fun `the ramp only ever goes down`() {
        val levels = listOf(9_000L, 7_000L, 5_000L, 3_000L, 1_000L, 0L)
            .map { fadeLevel(it, fade) }

        assertEquals(levels.sortedDescending(), levels)
    }

    @Test
    fun `a fade of no length is silent at once`() {
        assertEquals(0f, fadeLevel(5_000L, 0L), 0.001f)
    }
}
