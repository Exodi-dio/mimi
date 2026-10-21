package com.exodidio.mimi.library

/**
 * Where the user was before the app last went away.
 *
 * Deliberately made of primitives: the library layer has no business knowing about
 * playback enums, so the repeat setting travels as its ordinal and the playback side
 * turns it back into a mode.
 */
data class SavedPlayback(
    val mediaId: String,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatOrdinal: Int,
)

/**
 * What one scan did.
 *
 * [skipped] says the freshness window was still open and nothing was read, which is a
 * different answer from a scan that found no music. [dropped] counts cached rows the new
 * scan no longer contains, whether the file went away or a setting stopped including it.
 */
data class ScanResult(
    val found: Int,
    val dropped: Int,
    val skipped: Boolean,
)
