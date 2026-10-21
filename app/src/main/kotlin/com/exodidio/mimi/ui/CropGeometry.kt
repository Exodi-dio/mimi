package com.exodidio.mimi.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.max

/**
 * The arithmetic behind the crop editor, kept apart from the dialog so it can be checked
 * without a screen.
 *
 * A crop is three numbers and one rectangle: how far the picture is zoomed, where it has
 * been dragged to, and which part of the source that leaves visible. Getting those wrong
 * shows as a black edge in the saved picture, which nobody sees until the crop has already
 * been written over the old one, so the rules live here as plain functions and the dialog
 * only draws what they return.
 *
 * Offsets and window sizes are in pixels, and the offset is measured from the centred
 * position rather than from the top left, because centring is the state the picture starts
 * in and the one Reset returns to.
 */

/** The zoom the editor starts at: the picture already covers the window without cropping. */
internal const val CROP_MIN_SCALE = 1f

/** The furthest the editor zooms, past which a photograph is only a few pixels wide. */
internal const val CROP_MAX_SCALE = 5f

/** Keeps a zoom inside the range the slider offers, so a pinch cannot escape it. */
internal fun clampCropScale(scale: Float): Float = scale.coerceIn(CROP_MIN_SCALE, CROP_MAX_SCALE)

/**
 * How much the source must be multiplied by, at 1x, to cover the window completely.
 *
 * Cover rather than fit: a picture that fits inside the window would leave bars, and a bar
 * is not part of anybody's banner. The larger of the two ratios is the one that makes the
 * narrow side reach across, which is why one dimension is cropped even before zooming.
 */
internal fun coverScale(imageSize: IntSize, windowSize: Size): Float {
    if (imageSize.width <= 0 || imageSize.height <= 0) return 0f
    if (windowSize.width <= 0f || windowSize.height <= 0f) return 0f
    val widthRatio = windowSize.width / imageSize.width
    val heightRatio = windowSize.height / imageSize.height
    return max(widthRatio, heightRatio)
}

/** How far either way the picture may be dragged before an edge would show. */
internal fun maxOffset(scale: Float, imageSize: IntSize, windowSize: Size): Offset {
    val cover = coverScale(imageSize, windowSize)
    if (cover <= 0f) return Offset.Zero
    val drawnWidth = imageSize.width * cover * scale
    val drawnHeight = imageSize.height * cover * scale
    return Offset(
        x = ((drawnWidth - windowSize.width) / 2f).coerceAtLeast(0f),
        y = ((drawnHeight - windowSize.height) / 2f).coerceAtLeast(0f),
    )
}

/**
 * Pulls [offset] back until no edge of the window can see past the picture.
 *
 * Called after every pan and every zoom change, because zooming out shrinks the slack the
 * previous drag was allowed to use; a drag that was legal at 5x would otherwise leave a gap
 * at 1x. A gesture therefore never needs to know where it started from.
 */
internal fun clampOffset(
    offset: Offset,
    scale: Float,
    imageSize: IntSize,
    windowSize: Size,
): Offset {
    if (imageSize.width <= 0 || imageSize.height <= 0) return Offset.Zero
    if (windowSize.width <= 0f || windowSize.height <= 0f) return Offset.Zero
    val limit = maxOffset(clampCropScale(scale), imageSize, windowSize)
    return Offset(
        x = offset.x.coerceIn(-limit.x, limit.x),
        y = offset.y.coerceIn(-limit.y, limit.y),
    )
}

/**
 * The part of the source that the window is currently showing, in source pixels.
 *
 * The inverse of what the dialog draws: the window is the centre of the picture, shifted by
 * the drag and divided by the total zoom. The result is kept inside the source before it is
 * returned, so a rounding error at the edge cannot ask [android.graphics.Bitmap.createBitmap]
 * for a rectangle that starts one pixel off the picture.
 */
internal fun cropRect(
    offset: Offset,
    scale: Float,
    imageSize: IntSize,
    windowSize: Size,
): Rect {
    val cover = coverScale(imageSize, windowSize)
    if (cover <= 0f) return Rect.Zero

    val total = cover * clampCropScale(scale)
    val width = (windowSize.width / total).coerceIn(1f, imageSize.width.toFloat())
    val height = (windowSize.height / total).coerceIn(1f, imageSize.height.toFloat())

    val left = (imageSize.width / 2f - width / 2f - offset.x / total)
        .coerceIn(0f, imageSize.width - width)
    val top = (imageSize.height / 2f - height / 2f - offset.y / total)
        .coerceIn(0f, imageSize.height - height)

    return Rect(left = left, top = top, right = left + width, bottom = top + height)
}
