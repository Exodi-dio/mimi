package com.exodidio.mimi.lyrics

import java.nio.charset.Charset

/**
 * Lyrics stored inside the audio file itself.
 *
 * MIMI does not talk to a lyrics service, so the words it shows are the words the file
 * carries: an ID3 `USLT` frame, a Vorbis comment, or an MP4 lyrics atom. Files that carry
 * none simply have none, which is why this gives up quietly instead of reaching for the
 * network.
 *
 * The container is identified by its own first bytes rather than by the file's declared MIME
 * type, because a file's name and its contents disagree often enough to matter.
 */
object EmbeddedLyrics {

    /** The largest single read, so a damaged header cannot ask for the whole file. */
    private const val MAX_BLOCK = 8L * 1024 * 1024

    /** Enough of the file to identify the container and read its first header. */
    private const val SNIFF_BYTES = 16

    /** An Ogg comment header sits in the opening pages; a quarter of a megabyte is generous. */
    private const val OGG_WINDOW = 256 * 1024

    private const val MAX_COMMENTS = 4096

    /** Keys a Vorbis comment may use for lyrics, best first. */
    private val LYRIC_KEYS = listOf(
        "LYRICS",
        "UNSYNCEDLYRICS",
        "UNSYNCED LYRICS",
        "SYNCEDLYRICS",
        "LRC",
        "LYRIC",
    )

    private val LYRIC_DESCRIPTIONS = setOf("LYRICS", "LYRIC", "UNSYNCEDLYRICS", "UNSYNCED LYRICS")

    /** Atoms an MP4 keeps its lyrics inside. */
    private val CONTAINER_ATOMS = setOf("moov", "udta", "trak", "mdia", "meta", "ilst")

    private const val MP4_LYRICS = "\u00a9lyr"

    fun read(source: ByteSource): String? {
        val head = source.read(0L, SNIFF_BYTES)
        val raw = when {
            startsWith(head, "ID3") -> readId3(source, head)
            startsWith(head, "fLaC") -> readFlac(source)
            startsWith(head, "OggS") -> readOgg(source)
            isMp4(head) -> readMp4(source)
            else -> null
        }
        return raw
            ?.replace("\u0000", "")
            ?.replace("\uFEFF", "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    // — ID3v2, which is what an MP3 or a tagged WAV carries ————————————————————————

    private fun readId3(source: ByteSource, head: ByteArray): String? {
        if (head.size < 10) return null
        val major = head[3].toInt() and 0xFF
        // Version 2.2 numbers its frames with three byte ids and is long dead.
        if (major < 3) return null

        val tagSize = synchsafe(head, 6, 4).toLong().coerceAtMost(MAX_BLOCK).toInt()
        val body = source.read(10L, tagSize)
        var pos = 0

        if (head[5].toInt() and 0x40 != 0) {
            if (body.size < 4) return null
            // A 2.4 extended header counts its own size; a 2.3 one does not.
            pos = if (major >= 4) synchsafe(body, 0, 4) else 4 + beInt(body, 0, 4)
        }

        var lyrics: String? = null
        while (pos + 10 <= body.size) {
            val id = ascii(body, pos, 4)
            if (id.isEmpty() || id[0] == '\u0000') break

            val frameSize = if (major >= 4) synchsafe(body, pos + 4, 4) else beInt(body, pos + 4, 4)
            val frameStart = pos + 10
            val frameEnd = frameStart + frameSize
            if (frameSize <= 0 || frameEnd > body.size) break

            when (id) {
                "USLT" -> if (lyrics == null) lyrics = decodeLyricsFrame(body, frameStart, frameEnd)
                "TXXX" -> if (lyrics == null) lyrics = decodeDescribedFrame(body, frameStart, frameEnd)
            }
            pos = frameEnd
        }
        return lyrics
    }

    /** `USLT`: an encoding byte, a three letter language, a description, then the words. */
    private fun decodeLyricsFrame(bytes: ByteArray, start: Int, end: Int): String? {
        if (end - start < 5) return null
        val rest = String(bytes, start + 4, end - start - 4, charsetFor(bytes[start].toInt() and 0xFF))
        val nul = rest.indexOf('\u0000')
        val text = if (nul >= 0) rest.substring(nul + 1) else rest
        return text.replace("\u0000", "").trim().ifEmpty { null }
    }

    /** `TXXX`: a described text frame, used for lyrics by anything that dislikes `USLT`. */
    private fun decodeDescribedFrame(bytes: ByteArray, start: Int, end: Int): String? {
        if (end - start < 2) return null
        val rest = String(bytes, start + 1, end - start - 1, charsetFor(bytes[start].toInt() and 0xFF))
        val nul = rest.indexOf('\u0000')
        if (nul < 0) return null
        val description = rest.substring(0, nul).trim().uppercase()
        if (description !in LYRIC_DESCRIPTIONS) return null
        return rest.substring(nul + 1).replace("\u0000", "").trim().ifEmpty { null }
    }

    // — FLAC ————————————————————————————————————————————————————————————————————————

    private fun readFlac(source: ByteSource): String? {
        var pos = 4L
        var guard = 0
        while (guard++ < 64) {
            val header = source.read(pos, 4)
            if (header.size < 4) return null

            val last = header[0].toInt() and 0x80 != 0
            val type = header[0].toInt() and 0x7F
            val length = beInt(header, 1, 3)
            if (length.toLong() > MAX_BLOCK) return null

            if (type == 4) {
                val block = source.read(pos + 4, length)
                if (block.size >= length) return readVorbisComment(block)
            }
            if (last) return null
            pos += 4 + length
        }
        return null
    }

    private fun readVorbisComment(bytes: ByteArray): String? {
        var pos = 0

        val vendorLength = leInt(bytes, pos) ?: return null
        if (vendorLength < 0 || vendorLength > bytes.size) return null
        pos += 4 + vendorLength

        val count = leInt(bytes, pos) ?: return null
        pos += 4
        if (count <= 0 || count > MAX_COMMENTS) return null

        val found = HashMap<String, String>()
        for (index in 0 until count) {
            val length = leInt(bytes, pos) ?: break
            pos += 4
            if (length < 0 || pos + length > bytes.size) break

            val field = String(bytes, pos, length, Charsets.UTF_8)
            pos += length

            val split = field.indexOf('=')
            if (split <= 0) continue
            val key = field.substring(0, split).trim().uppercase()
            if (key in LYRIC_KEYS && key !in found) found[key] = field.substring(split + 1).trim()
        }
        return LYRIC_KEYS.firstNotNullOfOrNull { found[it]?.takeIf { value -> value.isNotEmpty() } }
    }

    // — Ogg, which carries the same Vorbis comments inside its pages ————————————————

    private fun readOgg(source: ByteSource): String? {
        val window = source.read(0L, OGG_WINDOW)
        if (window.size < 16) return null

        for (key in LYRIC_KEYS) {
            val needle = "$key=".toByteArray(Charsets.US_ASCII)
            var at = indexOf(window, needle, 0)
            while (at >= 0) {
                // A comment field starts right after the previous field's terminator, which is
                // what keeps "LYRICS=" from matching the tail of "UNSYNCEDLYRICS=".
                if (at > 0 && window[at - 1] == 0.toByte()) {
                    val value = readNulTerminated(window, at + needle.size).trim()
                    if (value.isNotEmpty()) return value
                }
                at = indexOf(window, needle, at + 1)
            }
        }
        return null
    }

    // — MP4, which is what an M4A or an ALAC file is ————————————————————————————————

    private fun readMp4(source: ByteSource): String? {
        var pos = 0L
        var guard = 0
        while (guard++ < 128) {
            val header = source.read(pos, 8)
            if (header.size < 8) return null

            var size = beInt(header, 0, 4).toLong()
            val type = ascii(header, 4, 4)
            var headerSize = 8L

            if (size == 1L) {
                val large = source.read(pos + 8, 8)
                if (large.size < 8) return null
                size = beLong(large, 0)
                headerSize = 16L
            } else if (size == 0L) {
                if (source.size <= pos) return null
                size = source.size - pos
            }
            if (size < headerSize) return null

            // The index of everything the file says about itself; lyrics live in here, and a
            // file that was not written for streaming keeps this at the very end, hence the
            // walk rather than a fixed offset.
            if (type == "moov") {
                val length = (size - headerSize).coerceAtMost(MAX_BLOCK).toInt()
                val moov = source.read(pos + headerSize, length)
                if (moov.size < length) return null
                return readMp4Lyrics(moov)
            }
            pos += size
        }
        return null
    }

    private fun readMp4Lyrics(moov: ByteArray): String? {
        val (from, to) = findAtom(moov, 0, moov.size, MP4_LYRICS, 0) ?: return null
        val data = findAtom(moov, from, to, "data", 0) ?: return null
        // A data atom is: size, type, well known type, locale, then the value.
        val start = data.first + 8
        if (start >= data.second) return null
        return String(moov, start, data.second - start, Charsets.UTF_8).trim().ifEmpty { null }
    }

    private fun findAtom(bytes: ByteArray, from: Int, to: Int, wanted: String, depth: Int): Pair<Int, Int>? {
        if (depth > 4) return null

        var pos = from
        while (pos + 8 <= to) {
            var size = beInt(bytes, pos, 4).toLong()
            val type = ascii(bytes, pos + 4, 4)
            var headerSize = 8

            if (size == 1L) {
                if (pos + 16 > to) return null
                size = beLong(bytes, pos + 8)
                headerSize = 16
            } else if (size == 0L) {
                size = (to - pos).toLong()
            }
            if (size < headerSize || pos + size > to) return null

            val bodyStart = pos + headerSize
            val bodyEnd = (pos + size).toInt()
            if (type == wanted) return bodyStart to bodyEnd

            if (type in CONTAINER_ATOMS) {
                // A meta atom carries four bytes of version and flags before its children.
                val nested = if (type == "meta") bodyStart + 4 else bodyStart
                findAtom(bytes, nested, bodyEnd, wanted, depth + 1)?.let { return it }
            }
            pos = bodyEnd
        }
        return null
    }

    // — Small helpers ————————————————————————————————————————————————————————————————

    private fun charsetFor(encoding: Int): Charset = when (encoding) {
        0 -> Charsets.ISO_8859_1
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        else -> Charsets.UTF_8
    }

    private fun isMp4(head: ByteArray): Boolean = ascii(head, 4, 4) == "ftyp"

    private fun startsWith(bytes: ByteArray, tag: String): Boolean =
        bytes.size >= tag.length && ascii(bytes, 0, tag.length) == tag

    private fun ascii(bytes: ByteArray, start: Int, length: Int): String {
        if (start < 0 || length <= 0 || start + length > bytes.size) return ""
        return String(bytes, start, length, Charsets.ISO_8859_1)
    }

    private fun beInt(bytes: ByteArray, start: Int, length: Int): Int {
        var value = 0
        for (index in 0 until length) {
            if (start + index >= bytes.size) return value
            value = (value shl 8) or (bytes[start + index].toInt() and 0xFF)
        }
        return value
    }

    private fun beLong(bytes: ByteArray, start: Int): Long {
        var value = 0L
        for (index in 0 until 8) {
            if (start + index >= bytes.size) return value
            value = (value shl 8) or (bytes[start + index].toLong() and 0xFF)
        }
        return value
    }

    private fun leInt(bytes: ByteArray, start: Int): Int? {
        if (start < 0 || start + 4 > bytes.size) return null
        return (bytes[start].toInt() and 0xFF) or
            ((bytes[start + 1].toInt() and 0xFF) shl 8) or
            ((bytes[start + 2].toInt() and 0xFF) shl 16) or
            ((bytes[start + 3].toInt() and 0xFF) shl 24)
    }

    /** Seven bits per byte, which is how ID3 sizes stay readable whatever the platform. */
    private fun synchsafe(bytes: ByteArray, start: Int, length: Int): Int {
        var value = 0
        for (index in 0 until length) {
            if (start + index >= bytes.size) break
            value = (value shl 7) or (bytes[start + index].toInt() and 0x7F)
        }
        return value
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray, from: Int): Int {
        if (needle.isEmpty()) return -1
        outer@ for (start in from.coerceAtLeast(0)..haystack.size - needle.size) {
            for (index in needle.indices) {
                if (haystack[start + index] != needle[index]) continue@outer
            }
            return start
        }
        return -1
    }

    private fun readNulTerminated(bytes: ByteArray, start: Int): String {
        var end = start
        while (end < bytes.size && bytes[end] != 0.toByte()) end++
        return String(bytes, start, (end - start).coerceAtLeast(0), Charsets.UTF_8)
    }
}
