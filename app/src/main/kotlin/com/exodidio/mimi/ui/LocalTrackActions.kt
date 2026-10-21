package com.exodidio.mimi.ui

import androidx.compose.runtime.compositionLocalOf
import com.exodidio.mimi.library.Track

/**
 * What a song row offers beyond playing the song.
 *
 * Handed down rather than passed through every list. Opening a song's details is the same
 * action wherever the song is found, and threading it through seven screens would put an
 * argument on each of them for something none of them decides.
 *
 * Picking songs out is here for the same reason. Every list draws the same row, so the row
 * can ask whether it is picked and say when it is long-pressed, and none of the seven
 * screens has to know that picking exists.
 */
data class TrackActions(
    /** Show everything known about a song. */
    val onShowInfo: (Track) -> Unit = {},
    /** Open the editor for a song's tags. */
    val onEditTags: (Track) -> Unit = {},
    /** Whether this song is currently picked out. */
    val isSelected: (Long) -> Boolean = { false },
    /** Whether anything is picked out at all, so a tap means "pick" rather than "play". */
    val isSelecting: () -> Boolean = { false },
    /** Add this song to the picked ones, or take it out if it is already in. */
    val onSelect: (Track) -> Unit = {},
)

val LocalTrackActions = compositionLocalOf { TrackActions() }
