package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioChannelsTest {

    @Test
    fun `one channel is mono and two are stereo`() {
        assertEquals("mono", channelLabel(1))
        assertEquals("stereo", channelLabel(2))
    }

    @Test
    fun `a count the device did not give is called unknown`() {
        assertEquals("unknown", channelLabel(null))
        assertEquals("unknown", channelLabel(0))
        assertEquals("unknown", channelLabel(-1))
    }

    @Test
    fun `a file with more channels is named by its count`() {
        assertEquals("6 channels", channelLabel(6))
    }
}
