package com.exodidio.mimi.playback

import android.media.audiofx.Equalizer
import com.exodidio.mimi.library.EqualizerProfile
import com.exodidio.mimi.library.gainAtFrequency

/**
 * The equaliser, attached to whatever the player is currently sending sound through.
 *
 * An [Equalizer] is bound to one audio session at construction, and the player creates a
 * new session when it is rebuilt, so the effect is re-created when the session changes
 * rather than kept across it. Every call is wrapped: there are devices and emulators where
 * the effect cannot be created at all, and a phone that cannot equalise should still play
 * music.
 *
 * The curve is described at ten fixed frequencies, but the device reports its own bands and
 * their own centre frequencies — usually five, at whatever points the maker chose. Each
 * band is therefore set to the curve read at that band's real frequency, so a ten-point
 * curve comes out the same shape on a device that only offers five.
 *
 * Opt-in by design. The effect costs a little power and, on a bad implementation, a little
 * fidelity, so nothing is created until the listener asks for it.
 */
class TrackEqualizer {

    private var equalizer: Equalizer? = null
    private var attachedSession = 0
    private var applied: Pair<Int, List<Float>>? = null

    /**
     * Brings the effect into line with the listener's choice.
     *
     * Safe to call as often as convenient; a curve that is already applied to the session
     * that is already bound costs one comparison and nothing else.
     */
    fun apply(sessionId: Int, enabled: Boolean, curve: EqualizerProfile) {
        runCatching {
            if (!enabled) {
                // Released rather than switched off, so a listener who never turns it on
                // never pays for the effect being present.
                release()
                return
            }

            // A session of zero or less means the player has not made one yet. Trying to
            // bind to it throws, and it will be ready in a moment.
            if (sessionId <= 0) return
            if (applied == (sessionId to curve.gainsDb)) return

            val effect = equalizerFor(sessionId) ?: return
            effect.enabled = true
            applyCurve(effect, curve)
            applied = sessionId to curve.gainsDb
        }
    }

    private fun equalizerFor(sessionId: Int): Equalizer? {
        equalizer?.let { existing ->
            if (attachedSession == sessionId) return existing
        }

        release()
        val created = Equalizer(0, sessionId)
        equalizer = created
        attachedSession = sessionId
        return created
    }

    private fun applyCurve(effect: Equalizer, curve: EqualizerProfile) {
        val range = effect.bandLevelRange
        val lowest = range[0].toInt()
        val highest = range[1].toInt()
        val bands = effect.numberOfBands.toInt()

        for (band in 0 until bands) {
            // The device reports its centre frequencies in millihertz.
            val centreHz = effect.getCenterFreq(band.toShort()) / 1_000f
            val decibels = gainAtFrequency(curve.gainsDb, centreHz)
            val millibels = (decibels * 100f).toInt().coerceIn(lowest, highest)
            effect.setBandLevel(band.toShort(), millibels.toShort())
        }
    }

    /** Lets go of the effect, if there is one. */
    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        attachedSession = 0
        applied = null
    }
}
