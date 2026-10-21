package com.exodidio.mimi.playback

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayGainTest {

    @Test
    fun `a vorbis comment block gives up the gain and the peak`() {
        val block = vorbisBlock(
            "TITLE=A Song",
            "REPLAYGAIN_TRACK_GAIN=-7.12 dB",
            "REPLAYGAIN_TRACK_PEAK=0.988525",
        )

        val comments = vorbisComments(block)

        assertEquals("-7.12 dB", comments["REPLAYGAIN_TRACK_GAIN"])
        assertEquals("0.988525", comments["REPLAYGAIN_TRACK_PEAK"])
        assertEquals("A Song", comments["TITLE"])
    }

    @Test
    fun `an id3 tag gives up the same values`() {
        val tag = id3Tag(
            "REPLAYGAIN_TRACK_GAIN" to "-7.12 dB",
            "REPLAYGAIN_TRACK_PEAK" to "0.988525",
        )

        val comments = id3Comments(tag)

        assertEquals("-7.12 dB", comments["REPLAYGAIN_TRACK_GAIN"])
        assertEquals("0.988525", comments["REPLAYGAIN_TRACK_PEAK"])
    }

    @Test
    fun `an id3 tag of the later version is read the same way`() {
        val tag = id3TagOf(4, "REPLAYGAIN_ALBUM_GAIN" to "-6.20 dB")

        assertEquals("-6.20 dB", id3Comments(tag)["REPLAYGAIN_ALBUM_GAIN"])
    }

    @Test
    fun `a file with no tags gives up nothing at all`() {
        assertTrue(vorbisComments(ByteArray(0)).isEmpty())
        assertTrue(id3Comments(ByteArray(0)).isEmpty())
        assertTrue(mp4Comments(ByteArray(0)).isEmpty())
        assertTrue(id3Comments(id3Tag()).isEmpty())

        val tags = replayGainTagsOf(emptyMap())
        assertNull(tags.trackGainDb)
        assertNull(tags.albumGainDb)
        assertEquals(1f, replayGainScale(tags, ReplayGainMode.TRACK), 0.001f)
    }

    @Test
    fun `the values are read out of the comments`() {
        val tags = replayGainTagsOf(
            mapOf(
                "REPLAYGAIN_TRACK_GAIN" to "-7.12 dB",
                "REPLAYGAIN_TRACK_PEAK" to "0.988525",
                "REPLAYGAIN_ALBUM_GAIN" to "-6.20 dB",
                "REPLAYGAIN_ALBUM_PEAK" to "0.95",
            ),
        )

        assertEquals(-7.12f, tags.trackGainDb ?: 0f, 0.001f)
        assertEquals(0.988525f, tags.trackPeak ?: 0f, 0.0001f)
        assertEquals(-6.20f, tags.albumGainDb ?: 0f, 0.001f)
        assertEquals(0.95f, tags.albumPeak ?: 0f, 0.0001f)
    }

    @Test
    fun `a value that is not a number is read as no value`() {
        val tags = replayGainTagsOf(mapOf("REPLAYGAIN_TRACK_GAIN" to "quiet please"))

        assertNull(tags.trackGainDb)
        assertEquals(1f, replayGainScale(tags, ReplayGainMode.TRACK), 0.001f)
    }

    @Test
    fun `the track gain lowers the music by the decibels it names`() {
        val tags = replayGainTagsOf(mapOf("REPLAYGAIN_TRACK_GAIN" to "-6.02 dB"))

        assertEquals(0.5f, replayGainScale(tags, ReplayGainMode.TRACK), 0.01f)
    }

    @Test
    fun `the album gain is used when the album is asked for`() {
        val tags = replayGainTagsOf(
            mapOf(
                "REPLAYGAIN_TRACK_GAIN" to "-6.02 dB",
                "REPLAYGAIN_ALBUM_GAIN" to "0 dB",
            ),
        )

        assertEquals(1f, replayGainScale(tags, ReplayGainMode.ALBUM), 0.001f)
        assertEquals(0.5f, replayGainScale(tags, ReplayGainMode.TRACK), 0.01f)
    }

    @Test
    fun `no mode leaves the music exactly as it was recorded`() {
        val tags = replayGainTagsOf(mapOf("REPLAYGAIN_TRACK_GAIN" to "-6.02 dB"))

        assertEquals(1f, replayGainScale(tags, ReplayGainMode.NONE), 0.001f)
    }

    @Test
    fun `the preamp is added to the gain`() {
        val tags = replayGainTagsOf(mapOf("REPLAYGAIN_TRACK_GAIN" to "-6.02 dB"))

        assertEquals(0.25f, replayGainScale(tags, ReplayGainMode.TRACK, preampDb = -6.02f), 0.01f)
    }

    @Test
    fun `the music is never louder than it was recorded`() {
        val tags = replayGainTagsOf(mapOf("REPLAYGAIN_TRACK_GAIN" to "+6.02 dB"))

        assertEquals(1f, replayGainScale(tags, ReplayGainMode.TRACK), 0.001f)
    }

    @Test
    fun `a peak above one holds the music below it`() {
        val tags = replayGainTagsOf(
            mapOf(
                "REPLAYGAIN_TRACK_GAIN" to "0 dB",
                "REPLAYGAIN_TRACK_PEAK" to "1.5",
            ),
        )

        assertEquals(0.666f, replayGainScale(tags, ReplayGainMode.TRACK), 0.01f)
    }

    @Test
    fun `a peak below one does not hold the music back`() {
        val tags = replayGainTagsOf(
            mapOf(
                "REPLAYGAIN_TRACK_GAIN" to "-6.02 dB",
                "REPLAYGAIN_TRACK_PEAK" to "0.5",
            ),
        )

        assertEquals(0.5f, replayGainScale(tags, ReplayGainMode.TRACK), 0.01f)
    }

    @Test
    fun `the mode a setting names is the mode that is used`() {
        assertEquals(ReplayGainMode.NONE, ReplayGainMode.fromSetting("None"))
        assertEquals(ReplayGainMode.TRACK, ReplayGainMode.fromSetting("Track"))
        assertEquals(ReplayGainMode.ALBUM, ReplayGainMode.fromSetting("Album"))
        assertEquals(ReplayGainMode.NONE, ReplayGainMode.fromSetting("something else"))
    }

    /**
     * A vorbis comment block holding the given fields.
     */
    private fun vorbisBlock(vararg fields: String): ByteArray {
        val vendor = "MIMI test".toByteArray()
        val out = ByteArrayOutputStream()
        out.write(littleEndian(vendor.size))
        out.write(vendor)
        out.write(littleEndian(fields.size))
        fields.forEach { field ->
            val bytes = field.toByteArray()
            out.write(littleEndian(bytes.size))
            out.write(bytes)
        }
        return out.toByteArray()
    }

    /**
     * An ID3 tag holding the given described text frames.
     */
    private fun id3Tag(vararg frames: Pair<String, String>): ByteArray = id3TagOf(3, *frames)

    /**
     * An ID3 tag of the given version, holding the given described text frames.
     */
    private fun id3TagOf(version: Int, vararg frames: Pair<String, String>): ByteArray {
        val body = ByteArrayOutputStream()
        frames.forEach { (name, value) ->
            val content = ByteArrayOutputStream()
            content.write(0)
            content.write(name.toByteArray(Charsets.ISO_8859_1))
            content.write(0)
            content.write(value.toByteArray(Charsets.ISO_8859_1))
            val bytes = content.toByteArray()

            body.write("TXXX".toByteArray(Charsets.ISO_8859_1))
            // The earlier version counts its frames in whole bytes, the later one in
            // seven bits at a time.
            body.write(if (version >= 4) synchsafe(bytes.size) else bigEndian(bytes.size))
            body.write(byteArrayOf(0, 0))
            body.write(bytes)
        }

        val payload = body.toByteArray()
        val header = ByteArrayOutputStream()
        header.write("ID3".toByteArray(Charsets.ISO_8859_1))
        // The version, its revision and the flags, then the size of the tag.
        header.write(byteArrayOf(version.toByte(), 0, 0))
        header.write(synchsafe(payload.size))
        return header.toByteArray() + payload
    }

    /**
     * A number written four bytes at a time, the way the earlier version counts.
     */
    private fun bigEndian(value: Int): ByteArray = byteArrayOf(
        ((value shr 24) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

    /**
     * A number written four bytes at a time, the way the format counts from the front.
     */
    private fun littleEndian(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )

    /**
     * A number written seven bits at a time, as a tag counts its own size.
     */
    private fun synchsafe(value: Int): ByteArray = byteArrayOf(
        ((value shr 21) and 0x7F).toByte(),
        ((value shr 14) and 0x7F).toByte(),
        ((value shr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )
}
