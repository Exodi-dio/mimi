package com.exodidio.mimi.ui

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cover art for a song or an album, decoded off the main thread. Null when nothing can
 * be found, so callers draw a placeholder.
 *
 * Two places are tried, in order: the picture embedded in the file's own tags, then the
 * album art the device indexed for that album. The first is authoritative and needs no
 * provider; the second is what finds the covers that live beside the music as a
 * `cover.jpg`, which embedded tags never carry.
 *
 * Reading straight from the file keeps this working entirely offline and independent of
 * what MediaStore chose to index.
 *
 * [cacheKey] lets several callers share one decode. Every song in an album carries the
 * same cover, and a scrolling list asks for all of them, so the songs list keys by album
 * rather than by file and pays for one read instead of ten.
 *
 * [albumId] is only a fallback, and zero means "no album" rather than album number zero.
 *
 * Two settings steer this. "Cache images" keeps a decoded cover in memory so a scrolling
 * list does not read the same file again; with it off every visit decodes afresh, which is
 * slower and smaller. "Use MediaStore provided art" decides whether the device's album-art
 * index is consulted at all, which is the fallback that can be fast but also wrong.
 */
@Composable
fun rememberAlbumArt(
    mediaId: String?,
    targetPx: Int = 512,
    cacheKey: String? = null,
    albumId: Long? = null,
): ImageBitmap? {
    val context = LocalContext.current
    val settings = LocalSettings.current
    val caching = settings.cacheImages
    val storeArt = settings.mediaStoreArt
    val id = mediaId
    val album = albumId?.takeIf { it != 0L }
    val key = if (id == null && album == null) {
        null
    } else {
        "${cacheKey ?: id}${album?.let { "@a$it" }.orEmpty()}@$targetPx"
    }
    return produceState<ImageBitmap?>(initialValue = null, key, caching, storeArt) {
        value = null
        if (key == null) return@produceState

        if (caching) {
            artworkCache.get(key)?.let {
                value = it.asImageBitmap()
                return@produceState
            }
        }

        val decoded = withContext(Dispatchers.IO) {
            id?.let { decodeEmbeddedArt(context, it, targetPx) }
                ?: album?.takeIf { storeArt }?.let { decodeStoredArt(context, it, targetPx) }
        }
        if (caching && decoded != null) artworkCache.put(key, decoded)
        value = decoded?.asImageBitmap()
    }.value
}

/**
 * Album grids ask for the same cover from several rows and re-ask on every scroll, and
 * each decode opens the file and walks its tags. Caching the bitmap is the difference
 * between a grid that scrolls and one that stutters.
 */
private val artworkCache = object : LruCache<String, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 8).toInt(),
) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

private fun decodeEmbeddedArt(context: Context, mediaId: String, targetPx: Int): Bitmap? = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, Uri.parse(mediaId))
        val bytes = retriever.embeddedPicture ?: return@runCatching null

        // Two passes: measure first, then decode at a sample size that lands just
        // above the size we will actually draw.
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
 * The cover the device indexed for [albumId], which is where art kept beside the music
 * turns up. Wrapped, because a device that does not answer on this path should cost a
 * placeholder, not a crash.
 */
private fun decodeStoredArt(context: Context, albumId: Long, targetPx: Int): Bitmap? = runCatching {
    val uri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)

    // Two passes again, so a large cover is not decoded whole only to be shrunk.
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
