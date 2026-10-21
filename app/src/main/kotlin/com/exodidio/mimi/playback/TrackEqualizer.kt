package com.exodidio.mimi.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import com.exodidio.mimi.library.EqualizerProfile
import com.exodidio.mimi.library.gainAtFrequency

/**
 * The audio effects, attached to whatever the player is currently sending sound through.
 *
 * Two effects make the chain: an [Equalizer] that shapes the curve, and a [BassBoost]
 * that adds weight at the bottom of the range. Both are bound to one audio session when
 * they are created, and the player makes a new session when it is rebuilt, so they are
 * made again when the session changes rather than kept across it. Every call is wrapped:
 * there are devices and emulators where an effect cannot be created at all, and a phone
 * that cannot equalise should still play music.
 *
 * The curve is described at ten fixed frequencies, but the device reports its own bands
 * and their own centre frequencies — usually five, at whatever points the maker chose. Each
 * band is therefore set to the curve read at that band's real frequency, so a ten-point
 * curve comes out the same shape on a device that only offers five.
 *
 * The bass boost is not left to do as it likes. The platform's boost already works on the
 * bottom of the range, and a subsonic filter is written alongside it to keep the rumble
 * out, so the weight does not drag the whole mix down with it. The level is trimmed by
 * as much as the boost adds, which is the owner's business rather than the effect's:
 * the player's volume sits in front of these effects, so taking the trim off there is
 * what keeps the chain from being pushed into clipping.
 *
 * Opt-in by design. The effects cost a little power and, on a bad implementation, a
 * little fidelity, so nothing is created until the listener asks for it.
 */
class TrackEqualizer {

    private var equalizer: Equalizer? = null
    private var bass: BassBoost? = null
    private var attachedSession = 0
    private var applied: EffectState? = null

    /**
     * Brings the effects into line with the listener's choice.
     *
     * [bassBoost] is how much weight is added, as a percentage, where nothing is added at
     * zero. Safe to call as often as convenient; a chain that is already applied to the
     * session that is already bound costs one comparison and nothing else.
     */
    fun apply(
        sessionId: Int,
        enabled: Boolean,
        curve: EqualizerProfile,
        bassBoost: Int = 0,
    ) {
        runCatching {
            val wantsBoost = bassBoost > 0
            if (!enabled && !wantsBoost) {
                // Released rather than switched off, so a listener who never turns them
                // on never pays for the effects being present.
                release()
                return
            }

            // A session of zero or less means the player has not made one yet. Trying to
            // bind to it throws, and it will be ready in a moment.
            if (sessionId <= 0) return

            val state = EffectState(sessionId, enabled, curve.gainsDb, bassBoost)
            if (applied == state) return

            val (shaper, booster) = bind(sessionId, wantsBoost)
            if (shaper != null) {
                shaper.enabled = true
                applyCurve(shaper, curve, enabled, bassBoost)
            }
            if (booster != null) {
                if (wantsBoost) {
                    applyBassBoost(booster, bassBoost)
                } else {
                    runCatching { booster.enabled = false }
                }
            }
            applied = state
        }
    }

    /**
     * The effects bound to the session, made afresh when the session changes.
     *
     * An effect belongs to one audio session, so a session that has changed means the
     * old effects are of no use to anybody. The boost is only made while it is asked
     * for, and is let go of when it is not, so a phone that is not using it is not
     * paying for it.
     */
    private fun bind(sessionId: Int, wantsBoost: Boolean): Pair<Equalizer?, BassBoost?> {
        if (attachedSession != sessionId) {
            release()
            attachedSession = sessionId
            equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
            bass = null
        }

        if (wantsBoost) {
            if (bass == null) bass = runCatching { BassBoost(0, sessionId) }.getOrNull()
        } else if (bass != null) {
            runCatching { bass?.release() }
            bass = null
        }
        return equalizer to bass
    }

    /**
     * The curve and the subsonic filter, written onto the bands.
     *
     * The curve is only written when the equaliser is switched on; the filter belongs to
     * the bass boost and is written whenever the boost is asked for.
     */
    private fun applyCurve(
        effect: Equalizer,
        curve: EqualizerProfile,
        curveEnabled: Boolean,
        bassBoost: Int,
    ) {
        val range = effect.bandLevelRange
        val lowest = range[0].toInt()
        val highest = range[1].toInt()
        val bands = effect.numberOfBands.toInt()

        for (band in 0 until bands) {
            // The device reports its centre frequencies in millihertz.
            val centreHz = effect.getCenterFreq(band.toShort()) / 1_000f
            var decibels = if (curveEnabled) gainAtFrequency(curve.gainsDb, centreHz) else 0f
            // The subsonic filter is part of the boost: it is what keeps the rumble out of
            // the bottom of the range while the boost is at work.
            if (bassBoost > 0) decibels += subsonicCutDb(centreHz)
            val millibels = (decibels * 100f).toInt().coerceIn(lowest, highest)
            effect.setBandLevel(band.toShort(), millibels.toShort())
        }
    }

    /**
     * The boost itself.
     *
     * The platform's boost works on the bottom of the range on its own, so all that is
     * left to say is how hard it is pushed. The trim that keeps it from clipping is not
     * part of the effect: it is taken off the player's volume, which sits in front of
     * the effects, and is the owner's business.
     */
    private fun applyBassBoost(effect: BassBoost, strengthPercent: Int) {
        val millis = (MAX_STRENGTH * (strengthPercent.coerceIn(0, 100) / 100f)).toInt()
            .coerceIn(0, MAX_STRENGTH)
        runCatching { effect.setStrength(millis.toShort()) }
        effect.enabled = true
    }

    /** Lets go of the effects, if there are any. */
    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bass?.release() }
        equalizer = null
        bass = null
        attachedSession = 0
        applied = null
    }

    private companion object {
        /**
         * The strongest the platform's boost goes.
         *
         * The effect counts its strength in thousandths, so this is a hundred per cent.
         */
        const val MAX_STRENGTH = 1000
    }
}

/**
 * What the effects were last asked for, so the same thing is not written twice.
 */
private data class EffectState(
    val sessionId: Int,
    val enabled: Boolean,
    val gains: List<Float>,
    val bassBoost: Int,
)
