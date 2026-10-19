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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Shuffle
import com.exodidio.mimi.library.Track

/**
 * A screen that is a title, optionally a picture, and a list of songs.
 *
 * Albums, artists, genres, composers, years and folders are all this screen with different
 * contents, so they share it rather than each growing their own copy of the same list —
 * which is also what keeps a song looking the same wherever it is found.
 */
@Composable
fun TrackListScreen(
    title: String,
    tracks: List<Track>,
    onBack: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onShuffle: ((List<Track>) -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    artworkUri: String? = null,
    playingMediaId: String? = null,
    favoriteIds: Set<Long> = emptySet(),
    onToggleLove: ((Long) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null,
    onAddToPlaylist: ((Track) -> Unit)? = null,
    onRemoveFromPlaylist: ((Long) -> Unit)? = null,
    emptyMessage: String = "Nothing here yet.",
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = title,
            subtitle = subtitle ?: songCountLabel(tracks.size),
            onBack = onBack,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (artworkUri != null) {
                Artwork(
                    bitmap = rememberAlbumArt(artworkUri, targetPx = 512),
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    iconSize = 40.dp,
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            FilledTonalButton(onClick = { onPlay(tracks, 0) }, enabled = tracks.isNotEmpty()) {
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play")
            }
            if (onShuffle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onShuffle(tracks) },
                    enabled = tracks.isNotEmpty(),
                ) {
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

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = track.contentUri == playingMediaId,
                    onClick = { onPlay(tracks, index) },
                    isLoved = track.id in favoriteIds,
                    onToggleLove = onToggleLove?.let { love -> { love(track.id) } },
                    onPlayNext = onPlayNext?.let { next -> { next(track) } },
                    onAddToQueue = onAddToQueue?.let { add -> { add(track) } },
                    onAddToPlaylist = onAddToPlaylist?.let { add -> { add(track) } },
                    onRemoveFromPlaylist = onRemoveFromPlaylist?.let { remove -> { remove(track.id) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
