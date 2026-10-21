package com.exodidio.mimi.playback

/**
 * The levels the two tracks of an overlap play at.
 *
 * The track that is ending starts at full level and is taken down to nothing, while the
 * one coming up is brought up from nothing to full level. The two meet in the middle,
 * where the sum of them is at its largest.
 *
 * Pure, so the ramp can be checked without a device playing anything.
 */
fun crossfadeLevels(progress: Float): Pair<Float, Float> {
    val along = progress.coerceIn(0f, 1f)
    return (1f - along) to along
}

/**
 * How long before a track ends the overlap begins.
 *
 * The overlap has to be running before the track ends, or the player would carry on into
 * the next one by itself and the two would be heard at once. Starting a little early
 * costs the last moment of the track that is ending, which is what a crossfade does
 * anyway.
 */
const val CROSSFADE_MARGIN_MS = 300L

/**
 * How long the silence is that stands in for a gap the decoder would close.
 *
 * Long enough to be heard as a break between the tracks, short enough that the album
 * does not feel broken up.
 */
const val GAPLESS_GAP_MS = 180L
