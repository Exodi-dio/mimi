package com.exodidio.mimi.ui

import com.exodidio.mimi.library.SongFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongFormatTest {

    @Test
    fun `the line carries the format, the bitrate and the sample rate`() {
        val line = songFormatLine(
            SongFormat(mimeType = "audio/flac", bitrateBps = 940_000, sampleRateHz = 44_100),
        )

        assertEquals("FLAC \u00b7 940 kb/s \u00b7 44.1 kHz", line)
    }

    @Test
    fun `a part nothing is known about is left out`() {
        val line = songFormatLine(SongFormat(mimeType = "audio/mpeg", bitrateBps = 320_000))

        assertEquals("MP3 \u00b7 320 kb/s", line)
    }

    @Test
    fun `a file nothing is known about has no line at all`() {
        assertNull(songFormatLine(SongFormat()))
        assertNull(songFormatLine(null))
    }

    @Test
    fun `the bitrate is counted in the units it is quoted in`() {
        assertEquals("128 kb/s", songFormatLine(SongFormat(bitrateBps = 128_000)))
        assertEquals("1411 kb/s", songFormatLine(SongFormat(bitrateBps = 1_411_000)))
    }

    @Test
    fun `a bitrate of zero is not a bitrate`() {
        assertNull(songFormatLine(SongFormat(bitrateBps = 0)))
        assertNull(songFormatLine(SongFormat(bitrateBps = -1)))
    }

    @Test
    fun `a sample rate on a whole number of kilohertz drops the decimal`() {
        assertEquals("48 kHz", songFormatLine(SongFormat(sampleRateHz = 48_000)))
        assertEquals("44.1 kHz", songFormatLine(SongFormat(sampleRateHz = 44_100)))
        assertEquals("22.05 kHz", songFormatLine(SongFormat(sampleRateHz = 22_050)))
    }

    @Test
    fun `a sample rate of zero is not a sample rate`() {
        assertNull(songFormatLine(SongFormat(sampleRateHz = 0)))
    }

    @Test
    fun `the format is named from the type the device gives`() {
        assertEquals("MP3", songFormatLine(SongFormat(mimeType = "audio/mpeg")))
        assertEquals("FLAC", songFormatLine(SongFormat(mimeType = "audio/flac")))
        assertEquals("M4A", songFormatLine(SongFormat(mimeType = "audio/mp4")))
        assertEquals("OGG", songFormatLine(SongFormat(mimeType = "audio/ogg")))
        assertEquals("WAV", songFormatLine(SongFormat(mimeType = "audio/x-wav")))
    }

    @Test
    fun `a type nothing knows the name of is left out rather than guessed at`() {
        assertNull(songFormatLine(SongFormat(mimeType = "audio/x-something-else")))
        assertNull(songFormatLine(SongFormat(mimeType = null)))
    }
}
