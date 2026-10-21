package com.exodidio.mimi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.exodidio.mimi.library.PlayStat
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.addedWithin
import com.exodidio.mimi.library.playedWithin

/** The two lists that are views over the library rather than playlists that are written down. */
enum class AutoPlaylistKind(val title: String) {
    LAST_ADDED("Last added"),
    RECENTLY_PLAYED("Recently played"),
}

/**
 * One automatic list, sliced by the period chosen on the Playlists settings page.
 *
 * The period is applied when the screen is built rather than stored as a playlist, which
 * is what makes changing "Past three months" to "Past year" a setting instead of an edit
 * to every list that might have been made. The clock is read once per visit so a list does
 * not shuffle under a finger while it is being read.
 */
@Composable
fun AutoPlaylistScreen(
    kind: AutoPlaylistKind,
    tracks: List<Track>,
    playStats: Map<Long, PlayStat>,
    playingMediaId: String?,
    favoriteIds: Set<Long>,
    onToggleLove: (Long) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onShuffle: (List<Track>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = LocalSettings.current
    val now = remember { System.currentTimeMillis() }

    val range = when (kind) {
        AutoPlaylistKind.LAST_ADDED -> settings.lastAddedRange
        AutoPlaylistKind.RECENTLY_PLAYED -> settings.recentlyPlayedRange
    }
    val shown = remember(kind, tracks, playStats, range, now) {
        when (kind) {
            AutoPlaylistKind.LAST_ADDED -> tracks.addedWithin(range, now)
            AutoPlaylistKind.RECENTLY_PLAYED -> tracks.playedWithin(playStats, range, now)
        }
    }

    TrackListScreen(
        title = kind.title,
        subtitle = range.label,
        tracks = shown,
        onBack = onBack,
        onPlay = onPlay,
        onShuffle = onShuffle,
        playingMediaId = playingMediaId,
        favoriteIds = favoriteIds,
        onToggleLove = onToggleLove,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onAddToPlaylist = onAddToPlaylist,
        emptyMessage = when (kind) {
            AutoPlaylistKind.LAST_ADDED -> "No songs were added in this period."
            AutoPlaylistKind.RECENTLY_PLAYED -> "No songs were played in this period."
        },
        modifier = modifier,
    )
}
