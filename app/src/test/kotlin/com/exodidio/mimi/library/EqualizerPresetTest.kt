package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Test

class EqualizerPresetTest {

    @Test
    fun `flat asks for nothing anywhere`() {
        val range = -1500 to 1500
        assertEquals(0, EqualizerPreset.FLAT.gainAt(0f, range.first, range.second).toInt())
        assertEquals(0, EqualizerPreset.FLAT.gainAt(0.5f, range.first, range.second).toInt())
        assertEquals(0, EqualizerPreset.FLAT.gainAt(1f, range.first, range.second).toInt())
    }

    @Test
    fun `a preset reaches its own gains at the ends and the middle`() {
        val range = -1500 to 1500
        // Gains are decibels, and the device speaks millibels.
        assertEquals(600, EqualizerPreset.BASS.gainAt(0f, range.first, range.second).toInt())
        assertEquals(100, EqualizerPreset.BASS.gainAt(0.5f, range.first, range.second).toInt())
        assertEquals(0, EqualizerPreset.BASS.gainAt(1f, range.first, range.second).toInt())
    }

    @Test
    fun `a boost at the top of the range reads as a boost`() {
        val range = -1500 to 1500
        assertEquals(600, EqualizerPreset.TREBLE.gainAt(1f, range.first, range.second).toInt())
    }

    @Test
    fun `a cut is a cut`() {
        val range = -1500 to 1500
        assertEquals(-200, EqualizerPreset.VOCAL.gainAt(0f, range.first, range.second).toInt())
    }

    @Test
    fun `the shape is spread evenly between the three points`() {
        val range = -1500 to 1500
        // Halfway from the bottom to the middle of Bass: 6 dB down to 1 dB is 3.5 dB.
        assertEquals(350, EqualizerPreset.BASS.gainAt(0.25f, range.first, range.second).toInt())
    }

    @Test
    fun `a device with a smaller range gets its own ceiling`() {
        assertEquals(300, EqualizerPreset.BASS.gainAt(0f, -300, 300).toInt())
        // Treble boost's top gain is 0 dB, and this device cannot go below -100: the
        // nearest it can get is its own floor.
        assertEquals(-100, EqualizerPreset.TREBLE.gainAt(1f, -300, -100).toInt())
    }

    @Test
    fun `positions outside the range are treated as the ends`() {
        val range = -1500 to 1500
        assertEquals(600, EqualizerPreset.BASS.gainAt(-4f, range.first, range.second).toInt())
        assertEquals(0, EqualizerPreset.BASS.gainAt(9f, range.first, range.second).toInt())
    }

    @Test
    fun `bands sit between the ends`() {
        assertEquals(0f, bandPosition(0, 5))
        assertEquals(0.5f, bandPosition(2, 5))
        assertEquals(1f, bandPosition(4, 5))
        assertEquals(1f, bandPosition(9, 10))
    }

    @Test
    fun `one band is both ends and reads as the middle`() {
        assertEquals(0.5f, bandPosition(0, 1))
        assertEquals(0.5f, bandPosition(0, 0))
    }

    @Test
    fun `a name that means nothing here falls back to flat`() {
        assertEquals(EqualizerPreset.BASS, EqualizerPreset.of("BASS"))
        assertEquals(EqualizerPreset.FLAT, EqualizerPreset.of("SOMETHING_ELSE"))
        assertEquals(EqualizerPreset.FLAT, EqualizerPreset.of(null))
    }
}
