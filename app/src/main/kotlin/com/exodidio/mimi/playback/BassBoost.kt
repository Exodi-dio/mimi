package com.exodidio.mimi.playback

import kotlin.math.pow

/**
 * The corner of the filter that keeps the rumble out.
 *
 * Below this the sound is felt rather than heard, and on a phone speaker it is heard as
 * a rattle. It is also the part of the range a boost pushes hardest, so it is the part
 * that has to be taken back.
 */
const val SUBSONIC_CORNER_HZ = 40f

/**
 * How much the subsonic filter takes off at the very bottom of the range.
 */
const val SUBSONIC_CUT_DB = 6f

/**
 * How much of the level the trim takes away when the boost is at its strongest.
 *
 * The same figure as the ceiling the equaliser allows, because that is as much level as
 * the chain is ever prepared to move.
 */
const val BASS_BOOST_MAX_TRIM_DB = 12f

/**
 * The lowest the trim is allowed to pull the level down.
 *
 * A quarter of the level is as far as it goes: past that the song is quieter than it
 * was before the boost was asked for, which is the opposite of what was wanted.
 */
private const val MIN_TRIM_SCALE = 0.25f

/**
 * How much the level is pulled down as the boost rises.
 *
 * The boost adds weight, and the weight has to come from somewhere: taking it off the
 * rest of the mix is what keeps the chain from clipping, and it is the same figure as the
 * boost can add at its strongest.
 *
 * Pure, so the curve can be checked without a device playing anything.
 */
fun bassBoostTrimDb(strengthPercent: Int): Float =
    -BASS_BOOST_MAX_TRIM_DB * (strengthPercent.coerceIn(0, 100) / 100f)

/**
 * The level the trim leaves behind, as a fraction of the level it started with.
 *
 * The trim is taken off the player's volume, which sits in front of the effects, so the
 * boost has less to work with and the chain cannot be pushed into clipping. The question
 * worth asking is what the music ends up playing at. The answer is never more than what
 * it was, and it falls as the boost rises.
 *
 * Pure, so the curve can be checked without a device playing anything.
 */
fun bassBoostTrimScale(strengthPercent: Int): Float =
    10f.pow(bassBoostTrimDb(strengthPercent) / 20f).coerceIn(MIN_TRIM_SCALE, 1f)

/**
 * The cut the subsonic filter takes at a frequency.
 *
 * The cut is nothing at the corner and grows as the frequency falls, so the rumble is
 * taken out without touching the bass that is meant to be heard.
 *
 * Pure, so the curve can be checked without a device playing anything.
 */
fun subsonicCutDb(frequencyHz: Float): Float {
    if (frequencyHz >= SUBSONIC_CORNER_HZ) return 0f
    val bottom = frequencyHz.coerceAtLeast(1f)
    val depth = (SUBSONIC_CORNER_HZ - bottom) / (SUBSONIC_CORNER_HZ - 1f)
    return -SUBSONIC_CUT_DB * depth.coerceIn(0f, 1f)
}
