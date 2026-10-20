package com.exodidio.mimi.library

/**
 * The shapes the equaliser can take.
 *
 * Three numbers rather than a table per band, because the number of bands is a property of
 * the phone and not of the preset: one device offers five and the next offers ten, and a
 * preset that named a gain for each of five bands would be meaningless on the other. Low,
 * middle and high describe the same three intentions on any device, and [gainAt] spreads
 * them across however many bands there turn out to be.
 *
 * The gains are in decibels. Kept modest on purpose: a preset that adds twelve decibels at
 * the bottom will clip on most recordings, and a phone speaker asked to reproduce it will
 * simply distort.
 */
enum class EqualizerPreset(
    val label: String,
    val low: Float,
    val mid: Float,
    val high: Float,
) {
    FLAT("Flat", 0f, 0f, 0f),
    BASS("Bass boost", 6f, 1f, 0f),
    VOCAL("Vocal", -2f, 4f, 2f),
    ROCK("Rock", 5f, -1f, 4f),
    CLUB("Club", 4f, 2f, 1f),
    TREBLE("Treble boost", 0f, 1f, 6f),
    LOUDNESS("Loudness", 6f, 0f, 5f);

    companion object {
        /** The preset with this name, or [FLAT] if the stored name means nothing here. */
        fun of(name: String?): EqualizerPreset =
            entries.firstOrNull { it.name == name } ?: FLAT
    }
}

/**
 * The gain this preset wants at [position], where 0 is the lowest band and 1 the highest.
 *
 * Clamped to [min] and [max], which are the millibel limits the device reports for itself.
 * A preset asking for more than the amplifier allows is not an error worth raising — the
 * device's own ceiling is simply what it gets.
 *
 * Pure, so the spreading can be checked without a device in hand.
 */
fun EqualizerPreset.gainAt(position: Float, min: Int, max: Int): Short {
    val at = position.coerceIn(0f, 1f)

    // Low to middle across the first half, middle to high across the second, so a five band
    // device and a ten band device both end up with the same shape instead of the same
    // numbers.
    val decibels = if (at < 0.5f) {
        low + (mid - low) * (at / 0.5f)
    } else {
        mid + (high - mid) * ((at - 0.5f) / 0.5f)
    }

    return (decibels * 100f).toInt().coerceIn(min, max).toShort()
}

/**
 * Where the band at [index] of [bands] sits between the bottom and the top.
 *
 * One band is both the bottom and the top, and reads as the middle.
 */
fun bandPosition(index: Int, bands: Int): Float {
    if (bands <= 1) return 0.5f
    return (index.toFloat() / (bands - 1).toFloat()).coerceIn(0f, 1f)
}
