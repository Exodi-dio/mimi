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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.Trash2
import com.exodidio.mimi.library.Track
import kotlinx.coroutines.flow.Flow

/**
 * One playlist, in its own running order.
 *
 * The songs arrive as a flow keyed on the playlist id, so removing a song redraws the
 * list from the database rather than from local state that could drift.
 */
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    name: String,
    tracksForPlaylist: (Long) -> Flow<List<Track>>,
    onBack: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onShuffle: ((List<Track>) -> Unit)? = null,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onRemove: (Long) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    playingMediaId: String? = null,
    modifier: Modifier = Modifier,
) {
    val tracks by tracksForPlaylist(playlistId)
        .collectAsStateWithLifecycle(initialValue = emptyList<Track>())

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = name,
            subtitle = songCountLabel(tracks.size),
            onBack = onBack,
            trailing = {
                IconButton(onClick = onRename) {
                    Icon(
                        imageVector = Lucide.Pencil,
                        contentDescription = "Rename playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = "Delete playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = { onPlay(tracks, 0) }, enabled = tracks.isNotEmpty()) {
                Icon(imageVector = Lucide.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play")
            }
            if (onShuffle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { onShuffle(tracks) }, enabled = tracks.isNotEmpty()) {
                    Icon(imageVector = Lucide.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }
        }

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
                    Text(
                        text = "Nothing in this playlist yet",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Add songs with the list button in your library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        track = track,
                        isPlaying = track.contentUri == playingMediaId,
                        onClick = { onPlay(tracks, index) },
                        onPlayNext = { onPlayNext(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onRemoveFromPlaylist = { onRemove(track.id) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
