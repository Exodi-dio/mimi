package com.exodidio.mimi.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfadeTest {

    @Test
    fun `the track that is ending starts at full level`() {
        val levels = crossfadeLevels(0f)

        assertEquals(1f, levels.first, 0.001f)
        assertEquals(0f, levels.second, 0.001f)
    }

    @Test
    fun `the two tracks meet in the middle`() {
        val levels = crossfadeLevels(0.5f)

        assertEquals(0.5f, levels.first, 0.001f)
        assertEquals(0.5f, levels.second, 0.001f)
    }

    @Test
    fun `the track coming up ends at full level`() {
        val levels = crossfadeLevels(1f)

        assertEquals(0f, levels.first, 0.001f)
        assertEquals(1f, levels.second, 0.001f)
    }

    @Test
    fun `the level of each track only ever falls`() {
        val outgoing = (0..100 step 5).map { crossfadeLevels(it / 100f).first }
        val incoming = (0..100 step 5).map { crossfadeLevels(it / 100f).second }

        assertEquals(outgoing.sortedDescending(), outgoing)
        assertEquals(incoming.sorted(), incoming)
    }

    @Test
    fun `the ramp never goes outside the range it is given`() {
        val levels = crossfadeLevels(4f)

        assertEquals(0f, levels.first, 0.001f)
        assertEquals(1f, levels.second, 0.001f)
        assertTrue(crossfadeLevels(-1f).second <= 0f)
    }
}
