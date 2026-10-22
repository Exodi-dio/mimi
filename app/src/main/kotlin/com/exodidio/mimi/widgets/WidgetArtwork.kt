package com.exodidio.mimi.widgets

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The cover of a song, read off the disk.
 *
 * The same two places the app looks: the picture inside the file's own tags, then the
 * cover the device indexed for the album. The first is the record's own picture and
 * needs no provider; the second finds the covers that live beside the music as a file
 * of their own.
 *
 * What is read is kept, because a list of songs asks for the same cover again and again
 * and a cover is not worth opening a file for twice.
 */
suspend fun loadArtwork(
    context: Context,
    mediaId: String,
    albumId: Long?,
    sizePx: Int = ARTWORK_SIZE_PX,
): Bitmap? = withContext(Dispatchers.IO) {
    artworkFor(context, mediaId, albumId, sizePx)
}

/**
 * The cover of a song, read now rather than waited for.
 *
 * The rows of a list widget are drawn on a thread of their own, so the read can be made
 * there instead of being waited for.
 */
fun artworkFor(
    context: Context,
    mediaId: String,
    albumId: Long?,
    sizePx: Int = ARTWORK_SIZE_PX,
): Bitmap? {
    val key = "$mediaId@$sizePx"
    artworkCache.get(key)?.let { return it }

    val decoded = decodeEmbeddedArt(context, mediaId, sizePx)
        ?: decodeStoredArt(context, albumId, sizePx)
    if (decoded != null) artworkCache.put(key, decoded)
    return decoded
}

/**
 * The covers that have been read.
 *
 * Kept small on purpose: a widget shows a handful of covers at a time, and a library
 * with thousands of songs would otherwise fill the memory with pictures nobody is
 * looking at.
 */
private val artworkCache = object : LruCache<String, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 16).toInt(),
) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

/** The picture inside the file's own tags, which is the record's own cover. */
private fun decodeEmbeddedArt(context: Context, mediaId: String, targetPx: Int): Bitmap? = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, Uri.parse(mediaId))
        val bytes = retriever.embeddedPicture ?: return@runCatching null

        // Measured first and then decoded small, so a large cover is not opened whole
        // only to be shrunk afterwards.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sampleSize = 1
        val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
        while (longestEdge / (sampleSize * 2) >= targetPx) sampleSize *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } finally {
        runCatching { retriever.release() }
    }
}.getOrNull()

/** Where the device keeps the cover it found for an album. */
private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

/**
 * The cover the device indexed for [albumId].
 *
 * Wrapped, because a device that does not answer on this path should cost a
 * placeholder rather than a crash.
 */
private fun decodeStoredArt(context: Context, albumId: Long?, targetPx: Int): Bitmap? = runCatching {
    val id = albumId?.takeIf { it != 0L } ?: return@runCatching null
    val uri = ContentUris.withAppendedId(ALBUM_ART_URI, id)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    } ?: return@runCatching null

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

    var sampleSize = 1
    val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
    while (longestEdge / (sampleSize * 2) >= targetPx) sampleSize *= 2

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
}.getOrNull()

/**
 * How wide a cover is read.
 *
 * A widget draws a small picture, and reading a full size one for it would cost the
 * memory for nothing.
 */
internal const val ARTWORK_SIZE_PX = 192
