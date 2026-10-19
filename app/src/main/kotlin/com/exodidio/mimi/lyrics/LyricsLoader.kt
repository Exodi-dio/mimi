package com.exodidio.mimi.lyrics

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.system.Os
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads lyrics out of the file behind a content URI, once per song.
 *
 * A song that was asked for and turned out to have no lyrics is remembered as empty, so
 * reopening the player does not go back to the disk to be disappointed again.
 */
class LyricsLoader(private val context: Context) {

    private val cache = LruCache<String, Lyrics>(CACHE_ENTRIES)

    suspend fun load(mediaId: String): Lyrics {
        cache.get(mediaId)?.let { return it }
        val lyrics = withContext(Dispatchers.IO) { read(mediaId) }
        cache.put(mediaId, lyrics)
        return lyrics
    }

    private fun read(mediaId: String): Lyrics {
        val raw = runCatching {
            context.contentResolver.openFileDescriptor(Uri.parse(mediaId), "r")?.use { descriptor ->
                EmbeddedLyrics.read(DescriptorSource(descriptor))
            }
        }.getOrNull()

        val parsed = raw?.let(LyricsParser::parse) ?: return Lyrics.EMPTY
        return if (parsed.isEmpty) Lyrics.EMPTY else parsed
    }

    private companion object {
        const val CACHE_ENTRIES = 32
    }
}

/**
 * A [ByteSource] over an open file descriptor.
 *
 * Reads are positional, so a parser can jump straight to a metadata block without loading the
 * audio in between. The descriptor is owned by the caller and is left alone here.
 */
private class DescriptorSource(private val descriptor: ParcelFileDescriptor) : ByteSource {

    override val size: Long = descriptor.statSize

    override fun read(offset: Long, length: Int): ByteArray {
        if (offset < 0L || length <= 0) return ByteArray(0)

        val bytes = ByteArray(length)
        var total = 0
        while (total < length) {
            val read = runCatching {
                Os.pread(descriptor.fileDescriptor, bytes, total, length - total, offset + total)
            }.getOrDefault(0)
            if (read <= 0) break
            total += read
        }
        return if (total == length) bytes else bytes.copyOf(total)
    }
}
