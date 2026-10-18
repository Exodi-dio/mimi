package com.exodidio.mimi.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The containers are built here rather than loaded from disk, so the readers are exercised
 * against bytes whose meaning is known exactly.
 */
class EmbeddedLyricsTest {

    private fun read(bytes: ByteArray): String? = EmbeddedLyrics.read(ByteArraySource(bytes))

    // — MP3, tagged with ID3 ————————————————————————————————————————————————————————

    @Test
    fun `an ID3 v2_3 frame is read`() {
        val bytes = id3(3, frame("USLT", usltBody("eng", "Hello"), synchsafe = false))

        assertEquals("Hello", read(bytes))
    }

    @Test
    fun `an ID3 v2_4 frame size is read as synchsafe`() {
        // Long enough that a synchsafe size and a plain one disagree, which is the point.
        val long = "x".repeat(400)
        val bytes = id3(4, frame("USLT", usltBody("eng", long), synchsafe = true))

        assertEquals(long, read(bytes))
    }

    @Test
    fun `ID3 words may be UTF-16 rather than UTF-8`() {
        val bytes = id3(3, frame("USLT", usltBody("eng", "Hello", utf16 = true), synchsafe = false))

        assertEquals("Hello", read(bytes))
    }

    @Test
    fun `a TXXX frame only counts when it says it is lyrics`() {
        val notes = id3(3, frame("TXXX", txxxBody("NOTES", "Hello"), synchsafe = false))
        val lyrics = id3(3, frame("TXXX", txxxBody("LYRICS", "Hello"), synchsafe = false))

        assertNull(read(notes))
        assertEquals("Hello", read(lyrics))
    }

    @Test
    fun `an ID3 tag with only a title has no lyrics`() {
        val title = frame("TIT2", byteArrayOf(3) + "A song".toByteArray(Charsets.UTF_8), synchsafe = false)

        assertNull(read(id3(3, title)))
    }

    @Test
    fun `a frame holding nothing but spaces is not lyrics`() {
        val bytes = id3(3, frame("USLT", usltBody("eng", "   "), synchsafe = false))

        assertNull(read(bytes))
    }

    @Test
    fun `a truncated ID3 tag is refused rather than half read`() {
        assertNull(read("ID3".toByteArray(Charsets.US_ASCII)))
    }

    // — FLAC ————————————————————————————————————————————————————————————————

    @Test
    fun `a FLAC vorbis comment is read under either of its names`() {
        assertEquals("Hello", read(flacWith("LYRICS" to "Hello")))
        assertEquals("Hello", read(flacWith("UNSYNCEDLYRICS" to "Hello")))
    }

    @Test
    fun `a FLAC comment that is not lyrics is ignored`() {
        assertNull(read(flacWith("TITLE" to "Hello", "ARTIST" to "Someone")))
    }

    @Test
    fun `a FLAC comment list is walked to the field that matters`() {
        val bytes = flacWith("TITLE" to "A song", "ARTIST" to "Someone", "LYRICS" to "Hello")

        assertEquals("Hello", read(bytes))
    }

    // — Ogg ————————————————————————————————————————————————————————————————

    @Test
    fun `an Ogg field is matched from its start, not from the tail of a longer key`() {
        val bytes = oggWith("LYRICS" to "Timed", "UNSYNCEDLYRICS" to "Plain")

        assertEquals("Timed", read(bytes))
    }

    @Test
    fun `an Ogg file carrying only unsynced words still gives them up`() {
        assertEquals("Plain", read(oggWith("UNSYNCEDLYRICS" to "Plain")))
    }

    @Test
    fun `an Ogg file of comments that are not lyrics is ignored`() {
        assertNull(read(oggWith("TITLE" to "A song", "ARTIST" to "Someone")))
    }

    // — MP4 ————————————————————————————————————————————————————————————————

    @Test
    fun `an M4A lyrics atom is read`() {
        assertEquals("Hello", read(mp4With("Hello")))
    }

    @Test
    fun `an MP4 that keeps its index at the end of the file is still read`() {
        // Nothing is streamed from these files, so the metadata is often after the audio.
        val bytes = atom("ftyp", "M4A ".toByteArray(Charsets.US_ASCII)) +
            atom("mdat", ByteArray(4096)) +
            moovAtom("Hello")

        assertEquals("Hello", read(bytes))
    }

    @Test
    fun `an MP4 whose moov holds no lyrics has none`() {
        val bytes = atom("ftyp", "M4A ".toByteArray(Charsets.US_ASCII)) + moovAtom(null)

        assertNull(read(bytes))
    }

    // — Nothing recognisable ————————————————————————————————————————————————

    @Test
    fun `a container that is none of the four is given up on`() {
        assertNull(read("RIFF\u0000\u0000\u0000\u0000WAVEfmt ".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun `an empty file has no lyrics`() {
        assertNull(read(ByteArray(0)))
    }

    // — Building the containers ————————————————————————————————————————————————

    private fun usltBody(language: String, text: String, utf16: Boolean = false): ByteArray {
        val encoding = if (utf16) 1 else 3
        val words = if (utf16) text.toByteArray(Charsets.UTF_16) else text.toByteArray(Charsets.UTF_8)
        // Encoding, language, an empty description and its terminator, then the words.
        return byteArrayOf(encoding.toByte()) +
            language.toByteArray(Charsets.US_ASCII) +
            byteArrayOf(0, 0) +
            words
    }

    private fun txxxBody(description: String, value: String): ByteArray =
        byteArrayOf(3) +
            description.toByteArray(Charsets.UTF_8) +
            byteArrayOf(0) +
            value.toByteArray(Charsets.UTF_8)

    private fun frame(id: String, body: ByteArray, synchsafe: Boolean): ByteArray =
        id.toByteArray(Charsets.US_ASCII) +
            (if (synchsafe) synchsafeBytes(body.size) else beBytes(body.size, 4)) +
            byteArrayOf(0, 0) +
            body

    private fun id3(major: Int, vararg frames: ByteArray): ByteArray {
        val body = concat(frames.toList())
        return "ID3".toByteArray(Charsets.US_ASCII) +
            byteArrayOf(major.toByte(), 0, 0) +
            synchsafeBytes(body.size) +
            body
    }

    private fun flacWith(vararg fields: Pair<String, String>): ByteArray {
        val encoded = fields.map { (key, value) -> "$key=$value".toByteArray(Charsets.UTF_8) }
        val comment = concat(
            listOf(leBytes(0, 4), leBytes(encoded.size, 4)) + encoded.map { leBytes(it.size, 4) + it },
        )
        // The high bit marks the last metadata block; the low seven bits are the type, and
        // type 4 is where the comments live.
        val header = byteArrayOf(0x84.toByte()) + beBytes(comment.size, 3)
        return concat(listOf("fLaC".toByteArray(Charsets.US_ASCII), header, comment))
    }

    private fun oggWith(vararg fields: Pair<String, String>): ByteArray {
        val comments = fields.joinToString("\u0000", prefix = "\u0000", postfix = "\u0000") { (key, value) ->
            "$key=$value"
        }
        return concat(
            listOf(
                "OggS".toByteArray(Charsets.US_ASCII),
                ByteArray(20),
                comments.toByteArray(Charsets.UTF_8),
            ),
        )
    }

    private fun mp4With(lyrics: String): ByteArray =
        atom("ftyp", "M4A ".toByteArray(Charsets.US_ASCII)) + moovAtom(lyrics)

    private fun moovAtom(lyrics: String?): ByteArray {
        val ilst = if (lyrics == null) {
            atom("ilst", ByteArray(0))
        } else {
            // A data atom is: size, type, well known type, locale, then the value.
            val data = atom("data", beBytes(1, 4) + beBytes(0, 4) + lyrics.toByteArray(Charsets.UTF_8))
            atom("ilst", atom("\u00a9lyr", data))
        }
        // A meta atom carries four bytes of version and flags before its children.
        return atom("moov", atom("udta", atom("meta", beBytes(0, 4) + ilst)))
    }

    private fun atom(type: String, payload: ByteArray): ByteArray =
        beBytes(8 + payload.size, 4) + type.toByteArray(Charsets.ISO_8859_1) + payload

    private fun concat(parts: List<ByteArray>): ByteArray {
        var total = 0
        for (part in parts) total += part.size
        val out = ByteArray(total)
        var at = 0
        for (part in parts) {
            part.copyInto(out, at)
            at += part.size
        }
        return out
    }

    private fun beBytes(value: Int, length: Int): ByteArray =
        ByteArray(length) { ((value shr (8 * (length - 1 - it))) and 0xFF).toByte() }

    private fun leBytes(value: Int, length: Int): ByteArray =
        ByteArray(length) { ((value shr (8 * it)) and 0xFF).toByte() }

    private fun synchsafeBytes(value: Int): ByteArray = byteArrayOf(
        ((value shr 21) and 0x7F).toByte(),
        ((value shr 14) and 0x7F).toByte(),
        ((value shr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )
}
