package com.exodidio.mimi.ui

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
 * Cover art embedded in the audio file, decoded off the main thread. Null when the
 * file carries no picture or the tag cannot be read, so callers draw a placeholder.
 *
 * Reading the tag straight from the file keeps this working entirely offline and
 * independent of what MediaStore chose to index.
 *
 * [cacheKey] lets several callers share one decode. Every song in an album carries the
 * same cover, and a scrolling list asks for all of them, so the songs list keys by album
 * rather than by file and pays for one read instead of ten.
 */
@Composable
fun rememberAlbumArt(
    mediaId: String?,
    targetPx: Int = 512,
    cacheKey: String? = null,
): ImageBitmap? {
    val context = LocalContext.current
    val id = mediaId
    val key = if (id == null) null else "${cacheKey ?: id}@$targetPx"
    return produceState<ImageBitmap?>(initialValue = null, key) {
        value = null
        if (id == null || key == null) return@produceState

        artworkCache.get(key)?.let {
            value = it.asImageBitmap()
            return@produceState
        }

        val decoded = withContext(Dispatchers.IO) {
            decodeEmbeddedArt(context, id, targetPx)
        }
        if (decoded != null) artworkCache.put(key, decoded)
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
