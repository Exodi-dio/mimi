package com.exodidio.mimi.ui

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
import java.io.File
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
