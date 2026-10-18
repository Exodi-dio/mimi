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
