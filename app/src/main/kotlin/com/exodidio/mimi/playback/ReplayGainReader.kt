package com.exodidio.mimi.playback

import android.content.Context
import android.net.Uri
import android.system.Os
import java.io.FileDescriptor
import java.nio.charset.Charset

/**
 * Reads the ReplayGain tags out of an audio file.
 *
 * The device's own metadata reader is not used here: it answers with a fixed set of
 * fields — title, artist, running time — and ReplayGain is not one of them, so the file's
 * container is read directly instead. Only the walking of the container lives here; the
 * reading of the blocks inside it is done by the pure functions in `TagComments`.
 *
 * The file is opened for reading and never written to, and nothing is kept between calls
 * but the tags themselves.
 */
object ReplayGainReader {

    /** Enough of a file to tell what kind of container it is. */
    private const val SNIFF_BYTES = 16

    /** The largest block read out of a file, so a damaged header cannot ask for all of it. */
    private const val MAX_BLOCK = 8L * 1024 * 1024

    /** The opening of an Ogg file holds its comment header. */
    private const val OGG_WINDOW = 256 * 1024

    /**
     * The tags in the file at [uri], or nothing at all when it carries none.
     *
     * A file that cannot be opened or read is treated as a file with no tags, because a
     * song that cannot be examined should still play.
     */
    fun read(context: Context, uri: Uri): ReplayGainTags {
        val comments = runCatching { commentsIn(context, uri) }.getOrDefault(emptyMap())
        return replayGainTagsOf(comments)
    }

    /**
     * The comments in the file at [uri], read out of whichever container it turns out to
     * be.
     */
    private fun commentsIn(context: Context, uri: Uri): Map<String, String> {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyMap()
        descriptor.use { open ->
            val source = FileSource(open.fileDescriptor)
            val head = source.read(0L, SNIFF_BYTES)
            if (head.size < 8) return emptyMap()

            val magic = String(head, 0, 4, Charsets.ISO_8859_1)
            return when {
                magic == "ID3" -> id3Comments(source.read(0L, source.id3Size(head)))
                magic == "fLaC" -> flacComments(source)
                magic == "OggS" -> oggComments(source)
                String(head, 4, 4, Charsets.ISO_8859_1) == "ftyp" -> mp4FileComments(source)
                else -> emptyMap()
            }
        }
    }

    /**
     * The comments in the metadata block of a FLAC file.
     *
     * The blocks come one after another at the front of the file, each with a byte that
     * says what it is, and the one that holds the comments is the fourth kind.
     */
    private fun flacComments(source: FileSource): Map<String, String> {
        var pos = 4L
        repeat(64) {
            val header = source.read(pos, 4)
            if (header.size < 4) return emptyMap()

            val last = header[0].toInt() and 0x80 != 0
            val type = header[0].toInt() and 0x7F
            val length = ((header[1].toInt() and 0xFF) shl 16) or
                ((header[2].toInt() and 0xFF) shl 8) or
                (header[3].toInt() and 0xFF)

            if (type == 4) return vorbisComments(source.read(pos + 4, length))
            if (last) return emptyMap()
            pos += 4 + length
        }
        return emptyMap()
    }

    /**
     * The comments in an Ogg file.
     *
     * Ogg carries the same comments as FLAC does, but inside a page rather than a block,
     * so the header that says where they begin is looked for in the opening of the file.
     */
    private fun oggComments(source: FileSource): Map<String, String> {
        val window = source.read(0L, OGG_WINDOW)
        if (window.size < 16) return emptyMap()

        val vorbis = indexOf(window, byteArrayOf(3) + "vorbis".toByteArray(Charsets.US_ASCII))
        if (vorbis >= 0) {
            return vorbisComments(window.copyOfRange(vorbis + 7, window.size))
        }

        val opus = indexOf(window, "OpusTags".toByteArray(Charsets.US_ASCII))
        if (opus >= 0) {
            return vorbisComments(window.copyOfRange(opus + 8, window.size))
        }
        return emptyMap()
    }

    /**
     * The comments in an MP4 file.
     *
     * The metadata of an MP4 lives in one atom, which can sit at either end of the file,
     * so the atoms are walked until it is found.
     */
    private fun mp4FileComments(source: FileSource): Map<String, String> {
        var pos = 0L
        repeat(128) {
            val header = source.read(pos, 8)
            if (header.size < 8) return emptyMap()

            var size = bigEndianLong(header, 0, 4)
            val type = String(header, 4, 4, Charsets.ISO_8859_1)
            var headerSize = 8

            if (size == 1L) {
                val large = source.read(pos + 8, 8)
                if (large.size < 8) return emptyMap()
                size = bigEndianLong(large, 0, 8)
                headerSize = 16
            } else if (size == 0L) {
                size = source.size - pos
            }
            if (size < headerSize) return emptyMap()

            if (type == "moov") {
                val length = (size - headerSize).coerceAtMost(MAX_BLOCK).toInt()
                return mp4Comments(source.read(pos + headerSize, length))
            }
            pos += size
        }
        return emptyMap()
    }

    /**
     * A window on a file, read without holding any of it in memory.
     *
     * The tags sit at the front of a file, apart from an MP4 whose metadata can sit at
     * the end, so the file is read at the offsets that are wanted rather than as a whole.
     */
    private class FileSource(private val descriptor: FileDescriptor) {

        /** How long the file is, or zero when it cannot be asked. */
        val size: Long = runCatching { Os.fstat(descriptor).st_size }.getOrDefault(0L)

        /**
         * The bytes at [offset], short at the end of the file.
         */
        fun read(offset: Long, length: Int): ByteArray {
            if (length <= 0 || offset < 0L) return ByteArray(0)
            val buffer = ByteArray(length.coerceAtMost(MAX_BLOCK.toInt()))
            val read = runCatching {
                Os.pread(descriptor, buffer, 0, buffer.size, offset)
            }.getOrDefault(-1)
            return if (read <= 0) ByteArray(0) else buffer.copyOf(read)
        }

        /**
         * How long the tag at the front of the file is.
         */
        fun id3Size(head: ByteArray): Int =
            (10L + synchsafeInt(head, 6, 4)).coerceAtMost(MAX_BLOCK).toInt()
    }

    /**
     * Where a run of bytes first appears, or -1 when it does not.
     */
    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        outer@ for (start in 0..haystack.size - needle.size) {
            for (index in needle.indices) {
                if (haystack[start + index] != needle[index]) continue@outer
            }
            return start
        }
        return -1
    }
}
