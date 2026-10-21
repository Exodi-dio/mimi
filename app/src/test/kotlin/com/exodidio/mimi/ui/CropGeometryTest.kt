package com.exodidio.mimi.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropGeometryTest {

    private val image = IntSize(1000, 1000)
    private val squareWindow = Size(300f, 300f)

    @Test
    fun `a one times crop covers the window without showing an edge`() {
        val crowded = clampOffset(
            offset = Offset(900f, -900f),
            scale = 1f,
            imageSize = image,
            windowSize = squareWindow,
        )
        val area = cropRect(crowded, 1f, image, squareWindow)

        assertInside(area, image)
    }

    @Test
    fun `a five times crop covers the window without showing an edge`() {
        val crowded = clampOffset(
            offset = Offset(9000f, -9000f),
            scale = 5f,
            imageSize = image,
            windowSize = squareWindow,
        )
        val area = cropRect(crowded, 5f, image, squareWindow)

        assertInside(area, image)
    }

    @Test
    fun `a mismatched window is still covered at one times`() {
        val wideWindow = Size(600f, 200f)
        val area = cropRect(
            offset = clampOffset(Offset(-1000f, 1000f), 1f, image, wideWindow),
            scale = 1f,
            imageSize = image,
            windowSize = wideWindow,
        )

        assertInside(area, image)
        // Cover means the long side of the window sets the zoom, so the crop runs the full
        // width of the picture and is taller than the 600 by 200 window.
        assertEquals(1000f, area.width, 0.01f)
        assertEquals(1000f / 3f, area.height, 0.01f)
        // Dragged fully one way, the crop rests against that edge rather than past it.
        assertEquals(0f, area.top, 0.01f)
    }

    @Test
    fun `panning past the edge is pulled back to the last covered position`() {
        val window = Size(400f, 200f)
        val picture = IntSize(1000, 500)

        // A 2x zoom leaves half the overhang on each side: 800 by 400 drawn in 400 by 200.
        val pulled = clampOffset(
            offset = Offset(1000f, -1000f),
            scale = 2f,
            imageSize = picture,
            windowSize = window,
        )

        assertEquals(200f, pulled.x, 0.01f)
        assertEquals(-100f, pulled.y, 0.01f)
    }

    @Test
    fun `a drag that is already inside the picture is left alone`() {
        val window = Size(400f, 200f)
        val picture = IntSize(1000, 500)

        val kept = clampOffset(Offset(120f, -60f), 2f, picture, window)

        assertEquals(120f, kept.x, 0.01f)
        assertEquals(-60f, kept.y, 0.01f)
    }

    @Test
    fun `zooming out pulls a far drag back to the centre`() {
        val window = Size(400f, 200f)
        val picture = IntSize(1000, 500)

        // At 1x the cover is exact, so there is no slack and the old drag must vanish.
        val reset = clampOffset(Offset(180f, -90f), 1f, picture, window)

        assertEquals(Offset.Zero, reset)
    }

    @Test
    fun `a centred one times fit keeps the whole source`() {
        val area = cropRect(Offset.Zero, 1f, image, squareWindow)

        assertEquals(0f, area.left, 0.01f)
        assertEquals(0f, area.top, 0.01f)
        assertEquals(1000f, area.width, 0.01f)
        assertEquals(1000f, area.height, 0.01f)
    }

    @Test
    fun `two times zoom halves the crop width and height`() {
        val atOne = cropRect(Offset.Zero, 1f, image, squareWindow)
        val atTwo = cropRect(Offset.Zero, 2f, image, squareWindow)

        assertEquals(atOne.width / 2f, atTwo.width, 0.01f)
        assertEquals(atOne.height / 2f, atTwo.height, 0.01f)
        assertEquals(250f, atTwo.left, 0.01f)
        assertEquals(250f, atTwo.top, 0.01f)
    }

    @Test
    fun `five times zoom takes a fifth of the crop width and height`() {
        val atOne = cropRect(Offset.Zero, 1f, image, squareWindow)
        val atFive = cropRect(Offset.Zero, 5f, image, squareWindow)

        assertEquals(atOne.width / 5f, atFive.width, 0.01f)
        assertEquals(atOne.height / 5f, atFive.height, 0.01f)
        assertEquals(200f, atFive.width, 0.01f)
        assertEquals(200f, atFive.height, 0.01f)
        assertEquals(400f, atFive.left, 0.01f)
        assertEquals(400f, atFive.top, 0.01f)
    }

    @Test
    fun `a centred crop of a mismatched picture is centred in the short dimension`() {
        val picture = IntSize(1000, 500)
        val window = Size(200f, 200f)

        val area = cropRect(Offset.Zero, 1f, picture, window)

        assertEquals(500f, area.width, 0.01f)
        assertEquals(500f, area.height, 0.01f)
        assertEquals(250f, area.left, 0.01f)
        assertEquals(0f, area.top, 0.01f)
    }

    @Test
    fun `a zoom outside the slider range is clamped`() {
        assertEquals(1f, clampCropScale(0.2f), 0.001f)
        assertEquals(5f, clampCropScale(12f), 0.001f)
        assertEquals(2.5f, clampCropScale(2.5f), 0.001f)
    }

    private fun assertInside(area: Rect, picture: IntSize) {
        assertTrue("left edge shows", area.left >= 0f)
        assertTrue("top edge shows", area.top >= 0f)
        assertTrue("right edge shows", area.right <= picture.width + 0.01f)
        assertTrue("bottom edge shows", area.bottom <= picture.height + 0.01f)
        assertTrue("crop is empty", abs(area.width) > 0f && abs(area.height) > 0f)
    }
}
