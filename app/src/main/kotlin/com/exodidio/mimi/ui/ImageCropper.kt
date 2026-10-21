package com.exodidio.mimi.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Crops and zooms a picked picture before it is kept.
 *
 * Picking used to be the whole decision: the picture was copied and shrunk to a fixed size,
 * which was quick but made the crop for the listener. A wide photograph became a banner
 * with its sides cut off wherever the app felt like cutting them, and an off-centre face
 * came back as an avatar with the wrong half of its head. The editor gives that decision
 * back: the window shows exactly what will be kept, pinch and drag position it, and the
 * slider does the same for one hand.
 *
 * The dialog is the width of the screen rather than the platform's narrow default, because
 * a crop of a banner in a 280dp box is too small to judge. The window is the only bright
 * thing on the dark scrim; everything around it is dimmed by the scrim, so the edges of what
 * survives are unambiguous.
 *
 * [aspectRatio] is what decides the shape and the saved size: sixteen by nine is a banner
 * at 1440 by 810, and anything squarer is an avatar at 512 by 512. The caller passes one of
 * those two, which keeps the saved file matched to where the picture is drawn.
 */
@Composable
fun ImageCropperDialog(
    source: Uri,
    aspectRatio: Float,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val wide = aspectRatio > 1.1f

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    var scale by remember { mutableStateOf(CROP_MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var windowSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(source) {
        loading = true
        failed = false
        // Decoding is not cancellable halfway through a stream, so it is allowed to finish
        // even if the dialog is closing; the result is then freed instead of being put into
        // state that nothing will draw.
        val decoded = withContext(NonCancellable) { decodeCropSource(context, source) }
        if (!isActive) {
            decoded?.recycle()
            return@LaunchedEffect
        }
        bitmap = decoded
        failed = decoded == null
        loading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            // The working copy is ours; the finished crop belongs to the store, which frees
            // it after writing. Saving one and freeing the other is what keeps a gesture
            // session from leaving two large bitmaps behind.
            bitmap?.recycle()
        }
    }

    val image = bitmap
    val imageSize = if (image != null) IntSize(image.width, image.height) else IntSize.Zero
    val preview = remember(image) { image?.asImageBitmap() }
    val cover = coverScale(imageSize, windowSize)

    LaunchedEffect(windowSize, imageSize) {
        offset = clampOffset(offset, scale, imageSize, windowSize)
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (wide) "Crop banner" else "Crop picture",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Drag to move, pinch or slide to zoom. The window is what gets kept.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .onSizeChanged {
                            windowSize = Size(it.width.toFloat(), it.height.toFloat())
                        }
                        .pointerInput(imageSize, windowSize) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = clampCropScale(scale * zoom)
                                // Clamped against the new scale, not the old one: zooming
                                // out is exactly when an earlier legal drag starts to
                                // expose an edge.
                                scale = nextScale
                                offset = clampOffset(
                                    offset = offset + pan,
                                    scale = nextScale,
                                    imageSize = imageSize,
                                    windowSize = windowSize,
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (preview != null && cover > 0f) {
                        // Two extra pixels of overscan absorb the rounding that turns the
                        // cover size into dp and back into layout pixels: without it a
                        // one-pixel shortfall would show as a hairline of black along an
                        // edge at full pan. The surplus is clipped by the window.
                        val drawnWidth = imageSize.width * cover
                        val drawnHeight = imageSize.height * cover
                        Image(
                            bitmap = preview,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(
                                    width = with(density) { (drawnWidth + 2f).toDp() },
                                    height = with(density) { (drawnHeight + 2f).toDp() },
                                )
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                        )
                    }

                    when {
                        loading -> CircularProgressIndicator()
                        failed -> Text(
                            text = "This picture could not be read. Try another one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Zoom",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = scale,
                        onValueChange = { value ->
                            // Zooming from the slider keeps the centre still and pulls the
                            // picture back inside the window if it was resting on an edge.
                            scale = clampCropScale(value)
                            offset = clampOffset(offset, scale, imageSize, windowSize)
                        },
                        valueRange = CROP_MIN_SCALE..CROP_MAX_SCALE,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text(
                        text = zoomLabel(scale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(
                        onClick = {
                            scale = CROP_MIN_SCALE
                            offset = Offset.Zero
                        },
                    ) { Text("Reset") }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onCancel) { Text("Cancel") }

                    Button(
                        onClick = {
                            val picked = bitmap ?: return@Button
                            if (windowSize == Size.Zero) return@Button
                            // The cut and the resize are a single click's work: a 2048px
                            // source shrinks to the saved size in well under a frame's
                            // worth of stalls, and doing it here keeps the source from
                            // being freed underneath a background job when the dialog
                            // closes on the same click.
                            val finished = renderCrop(
                                source = picked,
                                scale = scale,
                                offset = offset,
                                windowSize = windowSize,
                                target = cropTarget(wide),
                            )
                            if (finished != null) onConfirm(finished)
                        },
                        enabled = image != null && !loading && !failed,
                    ) { Text("Use picture") }
                }
            }
        }
    }
}

/** The saved size for the two shapes the profile has, matched to where they are drawn. */
private fun cropTarget(wide: Boolean): IntSize =
    if (wide) IntSize(1440, 810) else IntSize(512, 512)

/** A zoom as the slider's readout, one decimal so the two ends are visibly different. */
private fun zoomLabel(scale: Float): String {
    val tenths = (scale * 10f).roundToInt()
    return "${tenths / 10}.${tenths % 10}x"
}

/**
 * Cuts the visible rectangle out of [source] and scales it to [target].
 *
 * The crop is taken from the bitmap rather than by scaling the whole picture first, because
 * only the visible part is ever needed; the last step is an exact resize to the size the
 * store writes, so the file does not depend on the size of the window it was cropped in.
 * The intermediate crop is freed as soon as it has been scaled, unless it is the same
 * instance as the source or the result, in which case somebody else still owns it.
 */
internal fun renderCrop(
    source: Bitmap,
    scale: Float,
    offset: Offset,
    windowSize: Size,
    target: IntSize,
): Bitmap? = runCatching {
    val imageSize = IntSize(source.width, source.height)
    val area = cropRect(offset, scale, imageSize, windowSize)

    val left = area.left.roundToInt().coerceIn(0, source.width - 1)
    val top = area.top.roundToInt().coerceIn(0, source.height - 1)
    val width = area.width.roundToInt().coerceIn(1, source.width - left)
    val height = area.height.roundToInt().coerceIn(1, source.height - top)

    val cropped = Bitmap.createBitmap(source, left, top, width, height)
    val finished = Bitmap.createScaledBitmap(cropped, target.width, target.height, true)
    if (cropped !== source && cropped !== finished) cropped.recycle()
    finished
}.getOrNull()
