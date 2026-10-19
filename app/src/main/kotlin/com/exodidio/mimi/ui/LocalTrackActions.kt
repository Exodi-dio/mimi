package com.exodidio.mimi.ui

import androidx.compose.runtime.compositionLocalOf
import com.exodidio.mimi.library.Track

/**
 * What a song row offers beyond playing the song.
 *
 * Handed down rather than passed through every list. Opening a song's details is the same
 * action wherever the song is found, and threading it through seven screens would put an
 * argument on each of them for something none of them decides.
 */
data class TrackActions(
    /** Show everything known about a song. */
    val onShowInfo: (Track) -> Unit = {},
)

val LocalTrackActions = compositionLocalOf { TrackActions() }
