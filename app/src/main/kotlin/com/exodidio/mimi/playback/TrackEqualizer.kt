package com.exodidio.mimi.playback

import android.media.audiofx.Equalizer
import com.exodidio.mimi.library.EqualizerPreset
import com.exodidio.mimi.library.bandPosition
import com.exodidio.mimi.library.gainAt

/**
 * The equaliser, attached to whatever the player is currently sending sound through.
 *
 * An [Equalizer] is bound to one audio session at construction, and the player creates a
 * new session when it is rebuilt, so the effect is re-created when the session changes
 * rather than kept across it. Every call is wrapped: there are devices and emulators where
 * the effect cannot be created at all, and a phone that cannot equalise should still play
 * music.
 *
 * Opt-in by design. The effect costs a little power and, on a bad implementation, a little
 * fidelity, so nothing is created until the listener asks for it.
 */
class TrackEqualizer {

    private var equalizer: Equalizer? = null
    private var attachedSession = 0

    /**
     * Brings the effect into line with the listener's choice.
     *
     * Safe to call as often as convenient; it does nothing when nothing has changed.
     */
    fun apply(sessionId: Int, enabled: Boolean, preset: EqualizerPreset) {
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

            val effect = equalizerFor(sessionId) ?: return
            effect.enabled = true
            applyPreset(effect, preset)
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

    private fun applyPreset(effect: Equalizer, preset: EqualizerPreset) {
        val range = effect.bandLevelRange
        val lowest = range[0].toInt()
        val highest = range[1].toInt()
        val bands = effect.numberOfBands.toInt()

        for (index in 0 until bands) {
            effect.setBandLevel(
                index.toShort(),
                preset.gainAt(bandPosition(index, bands), lowest, highest),
            )
        }
    }

    /** Lets go of the effect, if there is one. */
    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        attachedSession = 0
    }
}
