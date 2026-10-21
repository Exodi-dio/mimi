package com.exodidio.mimi.playback

import java.nio.charset.Charset

// The name and value pairs a file's metadata holds.
//
// ReplayGain is written into a different place in each container: a described text frame
// in an ID3 tag, a Vorbis comment in a FLAC or Ogg file, and a freeform atom in an MP4.
// Underneath, all three are the same thing — a list of names and values — so each is read
// here into one plain map and the ReplayGain values are picked out of that afterwards.
//
// The reading of a block is pure, so it can be checked without a device and without a file.

/**
 * The comments in a Vorbis comment block.
 *
 * The block is a vendor string, a count, and then that many fields of the form
 * `NAME=value`. The names are read in upper case, because the format says they do not
 * matter and the files in the wild disagree about the case.
 */
fun vorbisComments(block: ByteArray): Map<String, String> {
    val comments = LinkedHashMap<String, String>()
    var pos = 0

    val vendorLength = littleEndianInt(block, pos) ?: return comments
    if (vendorLength < 0) return comments
    pos += 4 + vendorLength

    val count = littleEndianInt(block, pos) ?: return comments
    pos += 4
    if (count <= 0 || count > MAX_COMMENTS) return comments

    repeat(count) {
        val length = littleEndianInt(block, pos) ?: return comments
        pos += 4
        if (length < 0 || pos + length > block.size) return comments

        val field = String(block, pos, length, Charsets.UTF_8)
        pos += length

        val split = field.indexOf('=')
        if (split <= 0) return@repeat
        val name = field.substring(0, split).trim().uppercase()
        if (name.isNotEmpty() && name !in comments) comments[name] = field.substring(split + 1).trim()
    }
    return comments
}

/**
 * The comments in an ID3 tag.
 *
 * An ID3 tag is a list of frames, and the ones that hold a name and a value are the
 * described text frames: an encoding, a name, and then the value. The tag carries its own
 * version in its header, and the two versions that are still met number their frames
 * differently, so the header is read rather than assumed.
 */
fun id3Comments(tag: ByteArray): Map<String, String> {
    val comments = LinkedHashMap<String, String>()
    if (tag.size < 10) return comments
    if (String(tag, 0, 3, Charsets.ISO_8859_1) != "ID3") return comments

    val major = tag[3].toInt() and 0xFF
    // The oldest version numbers its frames with three byte names and is long dead.
    if (major < 3) return comments
    val size = synchsafeInt(tag, 6, 4)
    val body = tag.copyOfRange(10, (10 + size).coerceAtMost(tag.size))

    var pos = 0
    if (major >= 3 && (tag[5].toInt() and 0x40) != 0) {
        // An extended header counts its own size, and the two versions count it
        // differently: a later one as a synchsafe number, an earlier one as a plain one.
        pos = if (major >= 4) {
            synchsafeInt(body, 0, 4)
        } else {
            4 + bigEndianInt(body, 0, 4)
        }
    }

    while (pos + 10 <= body.size) {
        val id = String(body, pos, 4, Charsets.ISO_8859_1)
        if (id.isEmpty() || id[0] == '\u0000') break

        val frameSize = if (major >= 4) {
            synchsafeInt(body, pos + 4, 4)
        } else {
            bigEndianInt(body, pos + 4, 4)
        }
        val start = pos + 10
        val end = start + frameSize
        if (frameSize <= 0 || end > body.size) break

        if (id == "TXXX") {
            val pair = describedText(body, start, end)
            if (pair != null) {
                val name = pair.first.trim().uppercase()
                if (name.isNotEmpty() && name !in comments) comments[name] = pair.second
            }
        }
        pos = end
    }
    return comments
}

/**
 * The freeform comments in the metadata of an MP4 file.
 *
 * MP4 gives the tags it knows an atom of their own, and writes the rest as freeform
 * atoms: a name, a meaning, and the value. ReplayGain is one of the freeform ones, so
 * only those are read here.
 */
fun mp4Comments(moov: ByteArray): Map<String, String> {
    val comments = LinkedHashMap<String, String>()
    freeformAtoms(moov, 0, moov.size, 0).forEach { atom ->
        val name = childText(moov, atom.first, atom.last + 1, "name") ?: return@forEach
        val value = childData(moov, atom.first, atom.last + 1) ?: return@forEach
        val key = name.trim().uppercase()
        if (key.isNotEmpty() && key !in comments) comments[key] = value
    }
    return comments
}

/**
 * The atoms that may hold other atoms.
 *
 * A metadata atom carries four bytes of version and flags before its children, which is
 * the one place the walk has to step over something.
 */
private val CONTAINER_ATOMS = setOf("moov", "udta", "trak", "mdia", "meta", "ilst")

/**
 * How many comments a block may claim to hold.
 *
 * A damaged block can claim a number that no file of that size could hold, and the walk
 * stops rather than believing it.
 */
private const val MAX_COMMENTS = 4096

/**
 * The freeform atoms inside a range, with the range each one covers.
 *
 * The last index is inclusive, so the body of an atom is `first until last + 1`.
 */
private fun freeformAtoms(bytes: ByteArray, from: Int, to: Int, depth: Int): List<IntRange> {
    if (depth > 5) return emptyList()

    val found = mutableListOf<IntRange>()
    var pos = from
    while (pos + 8 <= to) {
        var size = bigEndianLong(bytes, pos, 4)
        val type = String(bytes, pos + 4, 4, Charsets.ISO_8859_1)
        var header = 8

        if (size == 1L) {
            if (pos + 16 > to) break
            size = bigEndianLong(bytes, pos + 8, 8)
            header = 16
        } else if (size == 0L) {
            size = (to - pos).toLong()
        }
        if (size < header || pos + size > to) break

        val bodyStart = pos + header
        val bodyEnd = (pos + size).toInt()
        if (type == "----") {
            found += bodyStart until bodyEnd
        } else if (type in CONTAINER_ATOMS) {
            val nested = if (type == "meta") bodyStart + 4 else bodyStart
            found += freeformAtoms(bytes, nested, bodyEnd, depth + 1)
        }
        pos = bodyEnd
    }
    return found
}

/**
 * The text of a child atom, with the version and flags taken off the front.
 */
private fun childText(bytes: ByteArray, from: Int, to: Int, wanted: String): String? {
    val atom = childAtom(bytes, from, to, wanted) ?: return null
    val start = atom.first + 4
    if (start > atom.last + 1) return null
    return String(bytes, start, atom.last + 1 - start, Charsets.UTF_8).trim()
}

/**
 * The value of a data atom.
 *
 * A data atom says what kind of value it holds before the value itself, and the kinds
 * that matter here are text and a whole number of decibels.
 */
private fun childData(bytes: ByteArray, from: Int, to: Int): String? {
    val atom = childAtom(bytes, from, to, "data") ?: return null
    if (atom.last + 1 - atom.first < 8) return null

    val kind = bigEndianInt(bytes, atom.first, 4)
    val start = atom.first + 8
    val value = when (kind) {
        21 -> bigEndianShort(bytes, start).toString()
        22 -> (bigEndianShort(bytes, start).toInt() and 0xFFFF).toString()
        else -> String(bytes, start, atom.last + 1 - start, Charsets.UTF_8)
    }
    return value.trim().ifEmpty { null }
}

/**
 * A child atom of a range, with the last index inclusive.
 */
private fun childAtom(bytes: ByteArray, from: Int, to: Int, wanted: String): IntRange? {
    var pos = from
    while (pos + 8 <= to) {
        var size = bigEndianLong(bytes, pos, 4)
        val type = String(bytes, pos + 4, 4, Charsets.ISO_8859_1)
        var header = 8

        if (size == 1L) {
            if (pos + 16 > to) return null
            size = bigEndianLong(bytes, pos + 8, 8)
            header = 16
        } else if (size == 0L) {
            size = (to - pos).toLong()
        }
        if (size < header || pos + size > to) return null

        val bodyStart = pos + header
        val bodyEnd = (pos + size).toInt()
        if (type == wanted) return bodyStart until bodyEnd
        pos = bodyEnd
    }
    return null
}

/**
 * A described text frame: an encoding, a name, and then the value.
 *
 * The name ends at the first null, and the value is whatever is left. A name written in
 * wide characters ends on two nulls, so one of them can be left at the front of the
 * value.
 */
private fun describedText(bytes: ByteArray, start: Int, end: Int): Pair<String, String>? {
    if (end - start < 2) return null

    val charset = charsetFor(bytes[start].toInt() and 0xFF)
    val rest = String(bytes, start + 1, end - start - 1, charset)
    val split = rest.indexOf('\u0000')
    if (split < 0) return null

    val name = rest.substring(0, split).trim()
    val value = rest.substring(split + 1).trimStart('\u0000').trim()
    return name to value
}

/**
 * The character set a frame's encoding byte names.
 */
private fun charsetFor(encoding: Int): Charset = when (encoding) {
    0 -> Charsets.ISO_8859_1
    1 -> Charsets.UTF_16
    2 -> Charsets.UTF_16BE
    else -> Charsets.UTF_8
}

/**
 * A number read from four little-endian bytes, or nothing when the bytes are not there.
 */
internal fun littleEndianInt(bytes: ByteArray, start: Int): Int? {
    if (start < 0 || start + 4 > bytes.size) return null
    return (bytes[start].toInt() and 0xFF) or
        ((bytes[start + 1].toInt() and 0xFF) shl 8) or
        ((bytes[start + 2].toInt() and 0xFF) shl 16) or
        ((bytes[start + 3].toInt() and 0xFF) shl 24)
}

/**
 * A number read from big-endian bytes, or zero when the bytes are not there.
 */
internal fun bigEndianInt(bytes: ByteArray, start: Int, length: Int): Int {
    if (start < 0 || start + length > bytes.size) return 0
    var value = 0
    for (index in 0 until length) {
        value = (value shl 8) or (bytes[start + index].toInt() and 0xFF)
    }
    return value
}

/**
 * A number read from big-endian bytes, or zero when the bytes are not there.
 */
internal fun bigEndianLong(bytes: ByteArray, start: Int, length: Int): Long {
    if (start < 0 || start + length > bytes.size) return 0L
    var value = 0L
    for (index in 0 until length) {
        value = (value shl 8) or (bytes[start + index].toLong() and 0xFF)
    }
    return value
}

/**
 * A number read from two big-endian bytes, read as a signed one.
 */
internal fun bigEndianShort(bytes: ByteArray, start: Int): Short {
    if (start < 0 || start + 2 > bytes.size) return 0
    val value = ((bytes[start].toInt() and 0xFF) shl 8) or (bytes[start + 1].toInt() and 0xFF)
    return value.toShort()
}

/**
 * A number read from bytes that only use seven bits each.
 *
 * This is how an ID3 tag counts its own size, and it is what keeps a tag of any length
 * readable on any platform.
 */
internal fun synchsafeInt(bytes: ByteArray, start: Int, length: Int): Int {
    var value = 0
    for (index in 0 until length) {
        if (start + index >= bytes.size) break
        value = (value shl 7) or (bytes[start + index].toInt() and 0x7F)
    }
    return value
}
