package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.exodidio.mimi.library.Track
import kotlinx.coroutines.flow.Flow

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    tracksForAlbum: (Long) -> Flow<List<Track>>,
    onBack: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    modifier: Modifier = Modifier,
    playingMediaId: String? = null,
    favoriteIds: Set<Long> = emptySet(),
    onToggleLove: (Long) -> Unit = {},
    onPlayNext: (Track) -> Unit = {},
    onAddToQueue: (Track) -> Unit = {},
    onAddToPlaylist: (Track) -> Unit = {},
) {
    val tracks by remember(albumId) { tracksForAlbum(albumId) }
        .collectAsStateWithLifecycle(initialValue = emptyList<Track>())

    val album = tracks.firstOrNull()?.album ?: "Album"
    val artist = tracks.firstOrNull()?.artist

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = album,
            subtitle = if (artist == null) songCountLabel(tracks.size) else "$artist \u00b7 ${songCountLabel(tracks.size)}",
            onBack = onBack,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                bitmap = rememberAlbumArt(tracks.firstOrNull()?.contentUri, targetPx = 512),
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(8.dp),
                iconSize = 40.dp,
            )
            Spacer(modifier = Modifier.width(20.dp))
            FilledTonalButton(onClick = { onPlay(tracks, 0) }, enabled = tracks.isNotEmpty()) {
                Icon(imageVector = Lucide.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = track.contentUri == playingMediaId,
                    onClick = { onPlay(tracks, index) },
                    isLoved = track.id in favoriteIds,
                    onToggleLove = { onToggleLove(track.id) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
