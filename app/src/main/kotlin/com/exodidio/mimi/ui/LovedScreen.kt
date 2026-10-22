package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Shuffle
import com.exodidio.mimi.library.Track

/**
 * The songs the user marked as loved, newest first.
 *
 * A place of its own rather than a library section: it answers "what did I keep", which
 * is a different question from "what do I have". Both play buttons start from the list as
 * it is shown, so the newest loved song is the one that plays first.
 */
@Composable
fun LovedScreen(
    tracks: List<Track>,
    favoriteIds: Set<Long>,
    onToggleLove: (Long) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onTrackClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    onShuffle: ((List<Track>) -> Unit)? = null,
    playingMediaId: String? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Loved",
            subtitle = songSummaryLabel(tracks.size, totalDurationMs(tracks)),
        )

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Icon(
                        imageVector = Lucide.Heart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Nothing loved yet",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        text = "Tap the heart on a song to keep it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = { onTrackClick(0) }) {
                    Icon(imageVector = Lucide.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
                if (onShuffle != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { onShuffle(tracks) }) {
                        Icon(
                            imageVector = Lucide.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        track = track,
                        isPlaying = track.contentUri == playingMediaId,
                        onClick = { onTrackClick(index) },
                        isLoved = track.id in favoriteIds,
                        onToggleLove = { onToggleLove(track.id) },
                        onPlayNext = { onPlayNext(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) },
                    )
                }
            }
        }
    }
}
