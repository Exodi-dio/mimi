package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Shuffle
import com.exodidio.mimi.library.AlbumSummary
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.sortedForDetail
import kotlinx.coroutines.flow.Flow

/**
 * One artist: their records, then their songs.
 *
 * Playing starts with the songs as this screen shows them, so a search or a chosen order
 * carries into the queue instead of quietly playing something else. Hiding the artist is
 * offered here because this is the screen where their name is in front of the listener.
 */
@Composable
fun ArtistDetailScreen(
    artist: String,
    tracksForArtist: (String) -> Flow<List<Track>>,
    albumsForArtist: (String) -> Flow<List<AlbumSummary>>,
    onBack: () -> Unit,
    onAlbumClick: (AlbumSummary) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    modifier: Modifier = Modifier,
    onShuffle: ((List<Track>) -> Unit)? = null,
    onHideArtist: (String) -> Unit = {},
    playingMediaId: String? = null,
    favoriteIds: Set<Long> = emptySet(),
    onToggleLove: (Long) -> Unit = {},
    onPlayNext: (Track) -> Unit = {},
    onAddToQueue: (Track) -> Unit = {},
    onAddToPlaylist: (Track) -> Unit = {},
) {
    val tracks by remember(artist) { tracksForArtist(artist) }
        .collectAsStateWithLifecycle(initialValue = emptyList<Track>())
    val albums by remember(artist) { albumsForArtist(artist) }
        .collectAsStateWithLifecycle(initialValue = emptyList<AlbumSummary>())
    val settings = LocalSettings.current

    var searching by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    val ordered = remember(tracks, settings.detailSort, settings.sortDescending) {
        tracks.sortedForDetail(settings.detailSort, settings.sortDescending)
    }
    val shown = remember(ordered, filter) { ordered.matchingListFilter(filter) }

    val summary = when {
        filter.isNotBlank() -> listFilterLabel(shown.size, tracks.size)
        else -> buildString {
            append(songCountLabel(tracks.size))
            append(" \u00b7 ")
            append(albumCountLabel(albums.size))
            if (tracks.isNotEmpty()) {
                append(" \u00b7 ")
                append(formatListDuration(totalDurationMs(tracks)))
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        BarHeader(
            title = artist,
            subtitle = summary,
            onBack = onBack,
            trailing = {
                Row {
                    IconButton(onClick = { searching = !searching; if (!searching) filter = "" }) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = if (searching) "Close list search" else "Search these songs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DetailSortButton()
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Lucide.Ellipsis,
                                contentDescription = "More artist actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Hide this artist") },
                                leadingIcon = {
                                    Icon(imageVector = Lucide.EyeOff, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    onHideArtist(artist)
                                },
                            )
                        }
                    }
                }
            },
        )

        if (searching) {
            ListSearchBar(
                query = filter,
                onQueryChange = { filter = it },
                label = listFilterLabel(shown.size, tracks.size),
                onClose = { searching = false; filter = "" },
            )
        }

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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(onClick = { onPlay(shown, 0) }, enabled = shown.isNotEmpty()) {
                        Icon(imageVector = Lucide.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play")
                    }
                    if (onShuffle != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = { onShuffle(shown) }, enabled = shown.isNotEmpty()) {
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
            }

            if (tracks.isNotEmpty() && shown.isEmpty()) {
                item {
                    Text(
                        text = "No songs by this artist match \u201c$filter\u201d.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    )
                }
            }

            itemsIndexed(shown, key = { _, track -> track.id }) { index, track ->
                TrackRow(
                    track = track,
                    isPlaying = track.contentUri == playingMediaId,
                    onClick = { onPlay(shown, index) },
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
