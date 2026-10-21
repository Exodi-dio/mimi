package com.exodidio.mimi.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedPitchSheetTest {

    @Test
    fun `a whole rate loses its decimal`() {
        assertEquals("1\u00d7", speedLabel(1f))
        assertEquals("2\u00d7", speedLabel(2f))
    }

    @Test
    fun `a rate with a part keeps it`() {
        assertEquals("0.75\u00d7", speedLabel(0.75f))
        assertEquals("1.25\u00d7", speedLabel(1.25f))
        assertEquals("1.75\u00d7", speedLabel(1.75f))
    }

    @Test
    fun `no shift is called off rather than zero`() {
        assertEquals("Off", pitchLabel(0))
        assertEquals("Off", pitchLabel(-1))
    }

    @Test
    fun `a shift is written as it is said`() {
        assertEquals("+1", pitchLabel(1))
        assertEquals("+5", pitchLabel(5))
    }

    @Test
    fun `the stops are the ones the sheet offers`() {
        assertEquals(listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f), speedPresets)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), pitchPresets)
    }
}
