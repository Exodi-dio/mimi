package com.exodidio.mimi.playback

/**
 * A level moving from one number to another over a fixed time.
 *
 * The arithmetic is kept out of the service so that the shape of a play or pause ramp can
 * be checked without a device making a sound: the service asks where the ramp is now and
 * multiplies that into the level it already works out. A ramp does not hold the player,
 * and the player does not know a ramp exists, which is what lets the two call each other
 * off without either being left half way.
 *
 * The motion is linear, for the same reason the sleep timer's fade is: on a phone the
 * ends of a ramp are already quiet, and a straight line sounds like a hand on a volume
 * wheel rather than like the sound being taken away.
 *
 * Once stopped the ramp holds the level it had reached, so a cancelled ramp cannot drift
 * while the player is being paused by hand.
 */
class VolumeRamp(
    private val durationMs: Int,
    private val from: Float,
    private val to: Float,
) {

    /** Where the ramp was called off, or null while it is still running. */
    private var stoppedAtMs: Long? = null

    /**
     * The level [elapsedMs] after the ramp began.
     *
     * A ramp of no length is at its target at once, which is what makes a fade of zero
     * behave exactly like no fade at all. The answer is clamped to nothing at the bottom
     * and to the target at the top, so a level is never asked for beyond the two ends.
     */
    fun levelAt(elapsedMs: Long): Float {
        val target = to.coerceIn(0f, 1f)
        if (durationMs <= 0) return target

        val at = (stoppedAtMs ?: elapsedMs).coerceAtLeast(0L)
        val along = (at.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        return (from + (target - from) * along).coerceIn(0f, 1f)
    }

    /** True once there is nothing left of the ramp to run. */
    fun finished(elapsedMs: Long): Boolean =
        durationMs <= 0 || (stoppedAtMs ?: elapsedMs) >= durationMs

    /**
     * Stops the ramp where it is and answers the level it stopped on.
     *
     * The level is held rather than snapped to the target, because a ramp that is called
     * off is one the player has overtaken: the sound is already at that level and moving
     * it again would be a second ramp nobody asked for.
     */
    fun stop(elapsedMs: Long): Float {
        stoppedAtMs = elapsedMs.coerceAtLeast(0L)
        return levelAt(elapsedMs)
    }
}
