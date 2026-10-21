package com.exodidio.mimi.ui

import com.exodidio.mimi.lyrics.LyricLine
import kotlin.math.abs

/**
 * How many lines either side of the one being sung stay on the screen.
 *
 * Three either side is what the reference player shows: enough words to know where the
 * song is going, few enough that the line being sung is the one that is read.
 */
const val LYRIC_SIDE_LINES = 3

/**
 * A line of words, and how far it sits from the line being sung.
 *
 * The distance is what the fading is drawn from, so it is kept with the line rather
 * than worked out again at every redraw.
 */
data class LyricWindowItem(
    /** Where the line sits in the song. */
    val index: Int,
    /** The words themselves. */
    val line: LyricLine,
    /** How many lines away from the one being sung this one is. */
    val distance: Int,
)

/**
 * The lines to show around the one being sung.
 *
 * The window is centred on [currentIndex] and never runs past either end of the song,
 * so the first and last lines are shown with fewer neighbours beside them rather than
 * with empty space where words would be.
 *
 * A song that has not reached its first line yet is treated as being at the start: the
 * opening words are shown, dimmed, because there is nothing to be bright yet.
 */
fun lyricWindow(
    lines: List<LyricLine>,
    currentIndex: Int,
    sideLines: Int = LYRIC_SIDE_LINES,
): List<LyricWindowItem> {
    if (lines.isEmpty()) return emptyList()

    val focus = currentIndex.coerceIn(0, lines.lastIndex)
    val first = (focus - sideLines).coerceAtLeast(0)
    val last = (focus + sideLines).coerceAtMost(lines.lastIndex)

    return (first..last).map { index ->
        LyricWindowItem(
            index = index,
            line = lines[index],
            distance = abs(index - focus),
        )
    }
}

/**
 * How strongly a line is drawn, given how far it is from the one being sung.
 *
 * The sung line is at full strength and the ones around it fade away quickly. The fade
 * is steep on purpose: a line four away is invisible rather than faint, because a sheet
 * of half-legible words is harder to read than no words at all.
 */
fun lyricAlpha(distance: Int): Float = when (distance) {
    0 -> 1f
    1 -> 0.5f
    2 -> 0.28f
    3 -> 0.14f
    else -> 0f
}
