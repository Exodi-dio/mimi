package com.exodidio.mimi.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
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
 */
@Composable
fun rememberAlbumArt(mediaId: String?, targetPx: Int = 512): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, mediaId, targetPx) {
        value = null
        val id = mediaId ?: return@produceState
        value = withContext(Dispatchers.IO) {
            decodeEmbeddedArt(context, id, targetPx)
        }?.asImageBitmap()
    }.value
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
