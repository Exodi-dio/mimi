package com.exodidio.mimi.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PitchTest {

    @Test
    fun `no shift leaves the pitch where it was`() {
        assertEquals(1f, pitchFactor(0), 0.0001f)
    }

    @Test
    fun `a semitone is a twelfth of an octave`() {
        assertEquals(1.0595f, pitchFactor(1), 0.001f)
        assertEquals(1.3348f, pitchFactor(5), 0.001f)
    }

    @Test
    fun `a factor is read back as the semitones it stands for`() {
        assertEquals(0, semitonesForPitch(1f))
        assertEquals(1, semitonesForPitch(pitchFactor(1)))
        assertEquals(5, semitonesForPitch(pitchFactor(5)))
    }

    @Test
    fun `a factor that means nothing is read as no shift`() {
        assertEquals(0, semitonesForPitch(0f))
        assertEquals(0, semitonesForPitch(-1f))
    }
}
