package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.Track

/**
 * What the library adds up to: how much of it there is, and how much of it has been played.
 *
 * Plain rows of facts rather than charts. A chart needs a skin to say what its colours
 * mean, and until then a number with a label next to it is the more honest report.
 */
@Composable
fun StatisticsScreen(
    stats: LibraryStats,
    mostPlayed: List<Track>,
    onBack: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    modifier: Modifier = Modifier,
    playingMediaId: String? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Statistics", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSectionHeading("The library")
            SettingsInfoRow(title = "Songs", value = stats.songs.toString())
            SettingsInfoRow(title = "Albums", value = stats.albums.toString())
            SettingsInfoRow(title = "Artists", value = stats.artists.toString())
            SettingsInfoRow(title = "Loved", value = stats.loved.toString())
            SettingsInfoRow(title = "Playlists", value = stats.playlists.toString())
            SettingsInfoRow(
                title = "Playing time",
                value = formatTotalDuration(stats.totalDurationMs),
                subtitle = "End to end, without a break",
            )

            stats.longestSong?.let { longest ->
                SettingsInfoRow(
                    title = "Longest song",
                    value = formatDuration(longest.durationMs),
                    subtitle = longest.title,
                )
            }

            SettingsSectionHeading("Listening")
            SettingsInfoRow(title = "Times played", value = stats.totalPlays.toString())
            SettingsInfoRow(
                title = "Times skipped",
                value = stats.totalSkips.toString(),
                subtitle = "Moved on before the song had a chance",
            )

            if (mostPlayed.isEmpty()) {
                SettingsNote(
                    "Nothing has been played enough times to have a favourite yet. " +
                        "Play something and it will turn up here."
                )
            } else {
                SettingsSectionHeading("Most played")
                mostPlayed.forEachIndexed { index, track ->
                    MostPlayedRow(
                        position = index + 1,
                        track = track,
                        playing = track.contentUri == playingMediaId,
                        onClick = { onPlayTrack(track) },
                    )
                }
            }

            SettingsPageFooter()
        }
    }
}

/**
 * One line of the most played list.
 *
 * The position is what makes the list worth reading, so it is drawn as a wide, quiet
 * number beside the song rather than as a bullet.
 */
@Composable
private fun MostPlayedRow(
    position: Int,
    track: Track,
    playing: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (playing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val byline = listOfNotNull(track.artist, track.album).joinToString(" \u00b7 ")
                if (byline.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = byline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
