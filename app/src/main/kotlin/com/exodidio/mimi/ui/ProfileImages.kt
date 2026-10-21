package com.exodidio.mimi.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.exodidio.mimi.library.decodeProfileImage
import com.exodidio.mimi.library.profileInitial
import com.exodidio.mimi.library.sampleSizeFor
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The picture in [file], decoded off the main thread.
 *
 * [revision] is part of what starts the decode because a replaced picture is written to the
 * same file: the path never changes, so without a value that changes with it the first
 * decode would be kept and the listener would still see the old picture. Null while the
 * decode runs and when there is no file, so callers draw a placeholder in the meantime.
 */
@Composable
fun rememberProfileImage(file: File?, maxEdge: Int, revision: Int = 0): ImageBitmap? {
    return produceState<ImageBitmap?>(initialValue = null, file?.path, revision, maxEdge) {
        value = null
        if (file == null) return@produceState
        value = withContext(Dispatchers.IO) {
            decodeProfileImage(file, maxEdge)?.asImageBitmap()
        }
    }.value
}

/**
 * A round profile picture, or what stands in for one.
 *
 * The first letter of the name when there is a name, and a neutral person otherwise, so an
 * unset profile still has a face on it rather than an empty circle. The ring is the surface
 * colour, which is invisible against a surface and is what separates the circle from a
 * banner when it overlaps one.
 */
@Composable
fun ProfileAvatar(
    image: ImageBitmap?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val initial = profileInitial(name)
            if (initial != null) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = (size.value * 0.42f).sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Icon(
                    imageVector = Lucide.User,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxSize(0.45f),
                )
            }
        }
    }
}

/**
 * The working size the crop editor decodes a picked picture at.
 *
 * Two thousand and forty-eight pixels on the long edge is enough that a crop of the banner
 * still has detail when it is blown up to 1440 wide, and small enough that a twelve
 * megapixel photograph does not become a hundred megabytes of bitmap while somebody drags
 * it around. The saved file is smaller again, so nothing is lost that survives encoding.
 */
internal const val CROP_SOURCE_MAX_EDGE = 2048

/**
 * Reads the picture behind [uri] for the crop editor, off the main thread.
 *
 * A picked picture has to be looked at before it can be cropped, and it arrives as a stream
 * that may be a photograph forty times the size of the window. It is decoded once at a
 * capped size instead of being passed around as the original, and the decoder is asked for
 * a software bitmap because the result is later cut up and encoded to JPEG. Null when the
 * uri cannot be read, which the editor reports rather than crashing on a broken pick.
 */
internal suspend fun decodeCropSource(
    context: Context,
    uri: Uri,
    maxEdge: Int = CROP_SOURCE_MAX_EDGE,
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // The platform decoder applies the camera's rotation for us, so a portrait
            // photograph is not shown on its side in the window and saved that way.
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                val ratio = if (longest > maxEdge) maxEdge.toFloat() / longest else 1f
                decoder.setTargetSize(
                    (info.size.width * ratio).roundToInt().coerceAtLeast(1),
                    (info.size.height * ratio).roundToInt().coerceAtLeast(1),
                )
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight), maxEdge)
            }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@runCatching null

            val longest = maxOf(decoded.width, decoded.height)
            if (longest <= maxEdge) {
                decoded
            } else {
                val ratio = maxEdge.toFloat() / longest
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).roundToInt().coerceAtLeast(1),
                    (decoded.height * ratio).roundToInt().coerceAtLeast(1),
                    true,
                ).also { scaled -> if (scaled !== decoded) decoded.recycle() }
            }
        }
    }.getOrNull()
}
