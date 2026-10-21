package com.exodidio.mimi.playback

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The factor the pitch is multiplied by to shift it by [semitones].
 *
 * A semitone is a twelfth of an octave, so the factor is the twelfth root of two raised
 * to the number of semitones: one semitone is about 1.059, five are about 1.335. The
 * factor is what the player is given, because a player that shifts the pitch has no
 * notion of a semitone.
 *
 * Pure, so the numbers can be tested without a device.
 */
fun pitchFactor(semitones: Int): Float = 2f.pow(semitones / 12f)

/**
 * The semitones a pitch factor stands for, rounded to the nearest whole one.
 *
 * The player reports a factor rather than a semitone count, so the count is worked back
 * out for the sheet to show. A factor the player cannot make sense of is read as no
 * shift at all rather than as a number nobody asked for.
 *
 * Pure, so the numbers can be tested without a device.
 */
fun semitonesForPitch(factor: Float): Int {
    if (factor <= 0f) return 0
    return (12.0 * ln(factor.toDouble()) / LN_2).roundToInt()
}

/** The natural logarithm of two, which is the step from a factor to a semitone count. */
private const val LN_2 = 0.6931471805599453
