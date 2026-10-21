package com.exodidio.mimi.playback

import kotlin.math.pow

/**
 * Which of a file's two ReplayGain values the music is normalised by.
 *
 * A file carries one value for the track on its own and one for the album it belongs to,
 * and the two disagree whenever an album was put together from tracks of different
 * loudness. The track value keeps a compilation's cuts beside each other, and the album
 * value keeps one artist's records beside another's.
 */
enum class ReplayGainMode {

    /** Nothing is normalised, and the file plays at the level it was recorded at. */
    NONE,

    /** The value the file carries for the track on its own is used. */
    TRACK,

    /** The value the file carries for the album is used. */
    ALBUM;

    companion object {

        /**
         * The mode a stored setting names.
         *
         * Anything that is not a mode this build knows is read as no mode at all, because
         * a setting written by a later build should leave the music alone rather than be
         * guessed at.
         */
        fun fromSetting(value: String): ReplayGainMode = when (value.trim().lowercase()) {
            "track" -> TRACK
            "album" -> ALBUM
            else -> NONE
        }
    }
}

/**
 * The ReplayGain values a file carries.
 *
 * The gains are in decibels and the peaks are the fraction of full scale the loudest
 * moment of the file reaches. Either can be missing: plenty of files carry a gain and no
 * peak, and plenty carry neither.
 */
data class ReplayGainTags(
    val trackGainDb: Float? = null,
    val trackPeak: Float? = null,
    val albumGainDb: Float? = null,
    val albumPeak: Float? = null,
)

/** The name a track's gain is written under. */
const val REPLAYGAIN_TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"

/** The name a track's peak is written under. */
const val REPLAYGAIN_TRACK_PEAK = "REPLAYGAIN_TRACK_PEAK"

/** The name an album's gain is written under. */
const val REPLAYGAIN_ALBUM_GAIN = "REPLAYGAIN_ALBUM_GAIN"

/** The name an album's peak is written under. */
const val REPLAYGAIN_ALBUM_PEAK = "REPLAYGAIN_ALBUM_PEAK"

/**
 * The ReplayGain values, picked out of a file's comments.
 *
 * The names are read in upper case by the parsers, so a file that writes them in lower
 * case is read the same way. A value that cannot be read as a number is treated as no
 * value at all rather than as silence.
 */
fun replayGainTagsOf(comments: Map<String, String>): ReplayGainTags = ReplayGainTags(
    trackGainDb = comments[REPLAYGAIN_TRACK_GAIN]?.toNumber(),
    trackPeak = comments[REPLAYGAIN_TRACK_PEAK]?.toNumber(),
    albumGainDb = comments[REPLAYGAIN_ALBUM_GAIN]?.toNumber(),
    albumPeak = comments[REPLAYGAIN_ALBUM_PEAK]?.toNumber(),
)

/**
 * The level the music plays at, as a fraction of the level it was recorded at.
 *
 * The gain is a number of decibels, and decibels are turned into a factor because that is
 * what a volume is. The preamp is added to the gain first, since it is the listener's own
 * trim of the same thing.
 *
 * Two things stop the result from being more than it should be. The volume never goes
 * above one, so a gain that would push the music past the loudest the phone will go is
 * scaled back rather than clipped. And a file that says how loud its loudest moment is
 * has that honoured: the music is held below the peak instead of being allowed to spill
 * over the top of it.
 *
 * A file with no tags, or one whose mode is not being used, plays exactly as it was
 * recorded.
 *
 * Pure, so the numbers can be checked without a device playing anything.
 */
fun replayGainScale(
    tags: ReplayGainTags,
    mode: ReplayGainMode,
    preampDb: Float = 0f,
): Float {
    val gainDb = when (mode) {
        ReplayGainMode.NONE -> return 1f
        ReplayGainMode.TRACK -> tags.trackGainDb
        ReplayGainMode.ALBUM -> tags.albumGainDb
    } ?: return 1f

    val peak = when (mode) {
        ReplayGainMode.TRACK -> tags.trackPeak
        ReplayGainMode.ALBUM -> tags.albumPeak
        ReplayGainMode.NONE -> null
    }

    val wanted = 10f.pow((gainDb + preampDb) / 20f)
    // The volume cannot be louder than the loudest, so anything past that is scaled back.
    val capped = wanted.coerceAtMost(1f)
    // A peak above one means the file itself is already at the top of the range, and the
    // music is held below it rather than being allowed to spill over.
    val peakCap = peak?.takeIf { it > 0f }?.let { 1f / it } ?: 1f
    return capped.coerceAtMost(peakCap).coerceIn(0f, 1f)
}

/**
 * A tag's value read as a number.
 *
 * The values are written as `-7.12 dB` by one tagger and as `-7.12` by another, so the
 * unit is taken off before the number is read.
 */
private fun String.toNumber(): Float? {
    val text = trim()
    val bare = text.removeSuffix("dB").removeSuffix("db").removeSuffix("Db").trim()
    return bare.toFloatOrNull()
}
