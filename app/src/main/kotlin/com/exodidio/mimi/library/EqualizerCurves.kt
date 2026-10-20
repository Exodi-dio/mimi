package com.exodidio.mimi.library

/**
 * A named set of ten gains, one for each of [EqualizerFrequenciesHz].
 *
 * Ten fixed points rather than "whatever the phone has", because the curve is a statement
 * about how music should sound and not about any particular amplifier. The phone's own
 * equaliser usually offers five bands at frequencies of its choosing; [gainAtFrequency]
 * reads the curve at the frequency each of those bands actually sits on, so a curve means
 * the same thing on a device with five bands and one with ten.
 *
 * [isBuiltIn] separates the curves that came with the app from the ones the listener made.
 * The difference the listener sees is that a built-in curve can be put back and their own
 * can be thrown away.
 */
data class EqualizerProfile(
    val key: String,
    val name: String,
    val gainsDb: List<Float>,
    val isBuiltIn: Boolean = true,
)

/** The ten points every curve is drawn at. */
val EqualizerFrequenciesHz = listOf(32, 64, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)

/** How far a band can be pushed, up or down. Past this the recording starts to suffer. */
const val EQUALIZER_LIMIT_DB = 12f

/** The curve used when nothing has been chosen, and the one every other curve is measured against. */
const val EQUALIZER_FLAT_KEY = "flat"

/** Ten gains of nothing: the curve the app starts on and returns to when reset. */
val EQUALIZER_FLAT_CURVE: List<Float> = List(EqualizerFrequenciesHz.size) { 0f }

/**
 * The curves that come with the app.
 *
 * Deliberately the familiar set: the names are the ones people already know how to ask
 * for, and the shapes behind them are the ones those names have always meant. Harman and
 * the two Sony curves are the exception, included because they are what a listener
 * comparing against a phone's own presets will reach for.
 */
val EqualizerPresets = listOf(
    EqualizerProfile("flat", "Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqualizerProfile("classical", "Classical", listOf(0f, 0f, 0f, 0f, 0f, 0f, -7f, -7f, -7f, -9.5f)),
    EqualizerProfile("club", "Club", listOf(0f, 0f, 8f, 5.5f, 5.5f, 5.5f, 3f, 0f, 0f, 0f)),
    EqualizerProfile("dance", "Dance", listOf(9.5f, 7f, 2.5f, 0f, 0f, -5.5f, -7f, -7f, 0f, 0f)),
    EqualizerProfile("full_bass", "Full Bass", listOf(-8f, 9.5f, 9.5f, 5.5f, 1.5f, -4f, -8f, -10.5f, -11f, -11f)),
    EqualizerProfile("full_treble", "Full Treble", listOf(-9.5f, -9.5f, -9.5f, -4f, 2.5f, 11f, 12f, 12f, 12f, 12f)),
    EqualizerProfile("full_bass_treble", "Full Bass & Treble", listOf(7f, 5.5f, 0f, -7f, -5f, 1.5f, 8f, 11f, 12f, 12f)),
    EqualizerProfile("headphones", "Headphones", listOf(5f, 11f, 5.5f, -3f, -2.5f, 1.5f, 5f, 9.5f, 12f, 12f)),
    EqualizerProfile("large_hall", "Large Hall", listOf(10.5f, 10.5f, 5.5f, 5.5f, 0f, -5f, -5f, -5f, 0f, 0f)),
    EqualizerProfile("live", "Live", listOf(-5f, 0f, 4f, 5.5f, 5.5f, 5.5f, 4f, 2.5f, 2.5f, 2.5f)),
    EqualizerProfile("party", "Party", listOf(7f, 7f, 0f, 0f, 0f, 0f, 0f, 0f, 7f, 7f)),
    EqualizerProfile("pop", "Pop", listOf(-1.5f, 5f, 7f, 8f, 5.5f, 0f, -2.5f, -2.5f, -1.5f, -1.5f)),
    EqualizerProfile("reggae", "Reggae", listOf(0f, 0f, 0f, -5.5f, 0f, 6.5f, 6.5f, 0f, 0f, 0f)),
    EqualizerProfile("rock", "Rock", listOf(8f, 5f, -5.5f, -8f, -3f, 4f, 9f, 11f, 11f, 11f)),
    EqualizerProfile("jazz", "Jazz", listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f)),
    EqualizerProfile("hip_hop", "Hip-Hop", listOf(5f, 4f, 3f, 1f, -1f, -1f, 0f, -1f, 1f, 2f)),
    EqualizerProfile("ska", "Ska", listOf(-2.5f, -5f, -4f, 0f, 4f, 5.5f, 9f, 9.5f, 11f, 9.5f)),
    EqualizerProfile("soft", "Soft", listOf(5f, 1.5f, 0f, -2.5f, 0f, 4f, 8f, 9.5f, 11f, 12f)),
    EqualizerProfile("soft_rock", "Soft Rock", listOf(4f, 4f, 2.5f, 0f, -4f, -5.5f, -3f, 0f, 2.5f, 9f)),
    EqualizerProfile("techno", "Techno", listOf(8f, 5.5f, 0f, -5.5f, -5f, 0f, 8f, 9.5f, 9.5f, 9f)),
    EqualizerProfile("bass_booster", "Bass Booster", listOf(5.5f, 4.5f, 3f, 1.5f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqualizerProfile("treble_booster", "Treble Booster", listOf(0f, 0f, 0f, 0f, 0f, 0f, 1.5f, 3f, 4.5f, 5.5f)),
    EqualizerProfile("acoustic_vocal", "Acoustic / Vocal", listOf(2.5f, 1.5f, 0.5f, 0f, 1f, 2f, 2.5f, 2f, 1.5f, 1f)),
    EqualizerProfile("electronic_dance", "Electronic / Dance", listOf(4.5f, 3.5f, 1.5f, -0.5f, -1.5f, 0f, 1.5f, 2.5f, 3.5f, 4f)),
    EqualizerProfile("rnb_soul", "R&B / Soul", listOf(4.5f, 3.5f, 1.5f, 0f, 1f, 1.5f, 2f, 1.5f, 2.5f, 3f)),
    EqualizerProfile("vocal_booster", "Vocal Booster", listOf(-2f, -1f, 0f, 1f, 2f, 3f, 2.5f, 1.5f, 1f, 0f)),
    EqualizerProfile("loudness", "Loudness", listOf(5f, 3.5f, 1.5f, 0f, -1f, 0f, 1f, 2f, 3.5f, 4.5f)),
    EqualizerProfile("spoken_word_podcast", "Spoken Word / Podcast", listOf(-3f, -2f, -1f, 1f, 2.5f, 3.5f, 3f, 2f, 1f, 0f)),
    EqualizerProfile("harman_target", "Harman Target", listOf(3.5f, 3f, 1.5f, 0f, 0f, 0.5f, 1.5f, 2.5f, 3.5f, 4f)),
    EqualizerProfile("sony_excited", "Sony Excited", listOf(4f, 3f, 1f, 0f, 0f, 1f, 2f, 3f, 4f, 5f)),
    EqualizerProfile("sony_mellow", "Sony Mellow", listOf(2f, 1.5f, 1f, 0f, -1f, -1f, 0f, 1f, 1.5f, 2f)),
)

/**
 * Puts a gain where the listener can actually mean it.
 *
 * Clamped to the [EQUALIZER_LIMIT_DB] ceiling and snapped to half a decibel, so dragging
 * lands on values worth reading out. Zero is kept as ordinary zero rather than being
 * allowed to come back as negative zero, which would print as "-0.0 dB".
 */
fun normalizeGain(gainDb: Float): Float {
    val clamped = gainDb.coerceIn(-EQUALIZER_LIMIT_DB, EQUALIZER_LIMIT_DB)
    val snapped = (kotlin.math.round(clamped * 2f) / 2f)
    return if (snapped == 0f) 0f else snapped
}

/**
 * The gain [curve] asks for at [frequencyHz], read between the two points that surround it.
 *
 * Interpolation happens on a log scale, because that is how pitch and hearing work: 32 to
 * 64 Hz is one octave, and so is 8,000 to 16,000 Hz, even though the second gap is 250
 * times wider. Interpolating linearly would make the bottom of the curve collapse to
 * nothing and the top over-sing. Outside the range the nearest point is held, so a band
 * below 32 Hz takes the bass gain rather than silence.
 *
 * Pure, so the reading can be checked without a device listening to it.
 */
fun gainAtFrequency(curve: List<Float>, frequencyHz: Float): Float {
    if (curve.isEmpty()) return 0f
    if (curve.size == 1) return curve[0]

    val hz = frequencyHz.coerceAtLeast(1f)
    val lowest = EqualizerFrequenciesHz.first().toFloat()
    val highest = EqualizerFrequenciesHz.last().toFloat()
    if (hz <= lowest) return curve.first()
    if (hz >= highest) return curve.last()

    var index = 0
    while (index < EqualizerFrequenciesHz.lastIndex &&
        EqualizerFrequenciesHz[index + 1].toFloat() <= hz
    ) {
        index++
    }

    val below = EqualizerFrequenciesHz[index].toFloat()
    val above = EqualizerFrequenciesHz[index + 1].toFloat()
    val between = kotlin.math.ln(hz / below) / kotlin.math.ln(above / below)
    val atBelow = curve.getOrElse(index) { 0f }
    val atAbove = curve.getOrElse(index + 1) { 0f }
    return atBelow + (atAbove - atBelow) * between
}

/** How a frequency is written wherever a band is labelled. */
fun frequencyLabel(frequencyHz: Int): String =
    if (frequencyHz >= 1_000) "${frequencyHz / 1_000} kHz" else "$frequencyHz Hz"

/**
 * A curve as one string, for storing in a single row.
 *
 * Commas between gains, because a curve is only ever numbers, and the parsing direction
 * refuses anything that does not read back as ten of them.
 */
fun encodeCurve(curve: List<Float>): String = curve.joinToString(",") { it.toString() }

/** The curve written by [encodeCurve], or null if the text is not one. */
fun decodeCurve(text: String?): List<Float>? {
    if (text.isNullOrBlank()) return null
    val gains = text.split(',').mapNotNull { it.trim().toFloatOrNull() }
    return gains.takeIf { it.size == EqualizerFrequenciesHz.size }
}

/**
 * The listener's own profiles, packed into one row.
 *
 * `key|name|gains` per profile, profiles separated by `;`. Names are stripped of those two
 * characters when a profile is made, so nothing here needs escaping and the format stays
 * readable in a database browser.
 */
fun encodeProfiles(profiles: List<EqualizerProfile>): String =
    profiles.joinToString(";") { "${it.key}|${it.name}|${encodeCurve(it.gainsDb)}" }

/** The profiles written by [encodeProfiles]; anything unreadable is left out. */
fun decodeProfiles(text: String?): List<EqualizerProfile> {
    if (text.isNullOrBlank()) return emptyList()
    return text.split(';').mapNotNull { entry ->
        val fields = entry.split('|')
        if (fields.size != 3) return@mapNotNull null
        val gains = decodeCurve(fields[2]) ?: return@mapNotNull null
        EqualizerProfile(
            key = fields[0],
            name = fields[1],
            gainsDb = gains,
            isBuiltIn = false,
        )
    }
}

/** Makes a name safe for [encodeProfiles], and refuses one that is only spaces. */
fun cleanProfileName(name: String): String? {
    val cleaned = name.replace('|', ' ').replace(';', ' ').trim()
    return cleaned.ifEmpty { null }
}
