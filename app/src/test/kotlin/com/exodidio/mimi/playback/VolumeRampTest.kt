package com.exodidio.mimi.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeRampTest {

    @Test
    fun `a full ramp reaches the target and stays there`() {
        val ramp = VolumeRamp(durationMs = 500, from = 0f, to = 1f)

        assertEquals(0f, ramp.levelAt(0L), 0.001f)
        assertEquals(0.5f, ramp.levelAt(250L), 0.001f)
        assertEquals(1f, ramp.levelAt(500L), 0.001f)
        assertEquals(1f, ramp.levelAt(5_000L), 0.001f)
        assertTrue(ramp.finished(500L))
        assertFalse(ramp.finished(499L))
    }

    @Test
    fun `a ramp down reaches silence and stays silent`() {
        val ramp = VolumeRamp(durationMs = 750, from = 1f, to = 0f)

        assertEquals(1f, ramp.levelAt(0L), 0.001f)
        assertEquals(0.5f, ramp.levelAt(375L), 0.001f)
        assertEquals(0f, ramp.levelAt(750L), 0.001f)
        assertEquals(0f, ramp.levelAt(9_000L), 0.001f)
    }

    @Test
    fun `a cancelled ramp holds the level it had reached`() {
        val ramp = VolumeRamp(durationMs = 500, from = 0f, to = 1f)

        val held = ramp.stop(200L)

        assertEquals(0.4f, held, 0.001f)
        assertEquals(0.4f, ramp.levelAt(200L), 0.001f)
        assertEquals(0.4f, ramp.levelAt(500L), 0.001f)
        assertEquals(0.4f, ramp.levelAt(10_000L), 0.001f)
    }

    @Test
    fun `a ramp of no length jumps straight to the target`() {
        val up = VolumeRamp(durationMs = 0, from = 0f, to = 1f)
        val down = VolumeRamp(durationMs = 0, from = 1f, to = 0f)

        assertEquals(1f, up.levelAt(0L), 0.001f)
        assertEquals(1f, up.levelAt(1L), 0.001f)
        assertEquals(0f, down.levelAt(0L), 0.001f)
        assertTrue(up.finished(0L))
        assertTrue(down.finished(0L))
    }

    @Test
    fun `a ramp up never passes its target or drops below zero`() {
        val levels = (0L..600L step 25L).map { VolumeRamp(500, 0f, 1f).levelAt(it) }

        assertEquals(levels.sorted(), levels)
        assertTrue(levels.all { it in 0f..1f })
    }

    @Test
    fun `a ramp down never drops below silence or rises`() {
        val ramp = VolumeRamp(500, 1f, 0f)
        val levels = (0L..600L step 25L).map { ramp.levelAt(it) }

        assertEquals(levels.sortedDescending(), levels)
        assertTrue(levels.all { it in 0f..1f })
    }

    @Test
    fun `a ramp between two levels stays inside them`() {
        val ramp = VolumeRamp(400, 0.22f, 0.8f)
        val levels = (0L..400L step 10L).map { ramp.levelAt(it) }

        assertTrue(levels.all { it in 0.22f..0.8f })
    }

    @Test
    fun `a target outside the possible range is clamped`() {
        val loud = VolumeRamp(100, 0.5f, 1.4f)
        val quiet = VolumeRamp(100, 0.5f, -0.4f)

        assertEquals(1f, loud.levelAt(100L), 0.001f)
        assertEquals(0f, quiet.levelAt(100L), 0.001f)
    }
}
