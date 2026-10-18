package com.exodidio.mimi.lyrics

/**
 * Random access to the bytes of a file without holding the whole file in memory.
 *
 * Lyrics sit in a container's metadata, which is at a known offset, so reading them means a
 * handful of small reads rather than loading a fifty megabyte album track. Keeping this an
 * interface also lets the parsers be tested against byte arrays instead of real files.
 */
interface ByteSource {

    /** Total length in bytes, or -1 when it is not known. */
    val size: Long

    /** Reads up to [length] bytes starting at [offset], short at the end of the file. */
    fun read(offset: Long, length: Int): ByteArray
}

/** A [ByteSource] over bytes already in memory. */
class ByteArraySource(private val bytes: ByteArray) : ByteSource {

    override val size: Long = bytes.size.toLong()

    override fun read(offset: Long, length: Int): ByteArray {
        if (offset < 0L || length <= 0 || offset >= bytes.size) return ByteArray(0)
        val end = (offset + length).coerceAtMost(bytes.size.toLong()).toInt()
        return bytes.copyOfRange(offset.toInt(), end)
    }
}
