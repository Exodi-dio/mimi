package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exodidio.mimi.library.AlbumSummary
import com.exodidio.mimi.library.Track
import kotlinx.coroutines.flow.Flow

@Composable
fun ArtistDetailScreen(
    artist: String,
    tracksForArtist: (String) -> Flow<List<Track>>,
    albumsForArtist: (String) -> Flow<List<AlbumSummary>>,
    onBack: () -> Unit,
    onAlbumClick: (AlbumSummary) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    modifier: Modifier = Modifier,
    playingMediaId: String? = null,
    favoriteIds: Set<Long> = emptySet(),
    onToggleLove: (Long) -> Unit = {},
    onAddToPlaylist: (Track) -> Unit = {},
) {
    val tracks by remember(artist) { tracksForArtist(artist) }
        .collectAsStateWithLifecycle(initialValue = emptyList<Track>())
    val albums by remember(artist) { albumsForArtist(artist) }
        .collectAsStateWithLifecycle(initialValue = emptyList<AlbumSummary>())

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = artist,
            subtitle = "${songCountLabel(tracks.size)} \u00b7 ${albumCountLabel(albums.size)}",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (albums.isNotEmpty()) {
                item {
                    SectionTitle("Albums")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(albums, key = { it.albumId }) { album ->
                            AlbumCell(
                                album = album,
                                onClick = { onAlbumClick(album) },
                                modifier = Modifier.width(136.dp),
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("Songs")
            }
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = track.contentUri == playingMediaId,
                    onClick = { onPlay(tracks, index) },
                    isLoved = track.id in favoriteIds,
                    onToggleLove = { onToggleLove(track.id) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
    )
}
