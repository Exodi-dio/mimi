package com.exodidio.mimi.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EqualizerDialogTest {

    private val curve = listOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f)

    @Test
    fun `dragging one band leaves the other nine alone`() {
        val moved = curve.withGain(3, -5f)

        assertEquals(-5f, moved[3], 0.001f)
        assertEquals(9, moved.size)
        assertEquals(
            curve.filterIndexed { index, _ -> index != 3 },
            moved.filterIndexed { index, _ -> index != 3 },
        )
    }

    @Test
    fun `a band added beyond the end does not shorten the curve`() {
        val moved = curve.withGain(40, 6f)

        assertEquals(curve, moved)
    }

    @Test
    fun `a boost is read with its sign`() {
        assertEquals("+3 dB", signedDecibels(3f))
        assertEquals("+0.5 dB", signedDecibels(0.5f))
    }

    @Test
    fun `a cut is read with its own sign`() {
        assertEquals("-3 dB", signedDecibels(-3f))
        assertEquals("-0.5 dB", signedDecibels(-0.5f))
    }

    @Test
    fun `no change reads as a plain zero`() {
        assertEquals("0 dB", signedDecibels(0f))
    }

    @Test
    fun `whole decibels lose their decimal point`() {
        assertEquals("12", trimZero(12f))
        assertEquals("-12", trimZero(-12f))
        assertEquals("3.5", trimZero(3.5f))
    }
}
