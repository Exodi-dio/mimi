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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.sortedForDetail
import kotlinx.coroutines.flow.Flow

/**
 * One album and its songs.
 *
 * The running order is the album's own until the listener asks for another one, because
 * track numbers are part of what an album is. Hiding the album is offered here rather
 * than on the grid: the grid is for finding a record, and this is the screen where one
 * record is in front of the listener to decide about.
 */
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    tracksForAlbum: (Long) -> Flow<List<Track>>,
    onBack: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    modifier: Modifier = Modifier,
    onShuffle: ((List<Track>) -> Unit)? = null,
    onHideAlbum: (String) -> Unit = {},
    playingMediaId: String? = null,
    favoriteIds: Set<Long> = emptySet(),
    onToggleLove: (Long) -> Unit = {},
    onPlayNext: (Track) -> Unit = {},
    onAddToQueue: (Track) -> Unit = {},
    onAddToPlaylist: (Track) -> Unit = {},
) {
    val tracks by remember(albumId) { tracksForAlbum(albumId) }
        .collectAsStateWithLifecycle(initialValue = emptyList<Track>())
    val settings = LocalSettings.current

    var searching by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    val ordered = remember(tracks, settings.detailSort, settings.sortDescending) {
        tracks.sortedForDetail(settings.detailSort, settings.sortDescending)
    }
    val shown = remember(ordered, filter) { ordered.matchingListFilter(filter) }

    val title = tracks.firstOrNull()?.album ?: "Album"
    val artist = tracks.firstOrNull()?.artist
    val summary = songSummaryLabel(tracks.size, totalDurationMs(tracks))
    val headerSubtitle = when {
        filter.isNotBlank() -> listFilterLabel(shown.size, tracks.size)
        artist == null -> summary
        else -> "$artist \u00b7 $summary"
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = title,
            subtitle = headerSubtitle,
            onBack = onBack,
            trailing = {
                Row {
                    IconButton(onClick = { searching = !searching; if (!searching) filter = "" }) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = if (searching) "Close list search" else "Search this album",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DetailSortButton()
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Lucide.Ellipsis,
                                contentDescription = "More album actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Hide this album") },
                                leadingIcon = {
                                    Icon(imageVector = Lucide.EyeOff, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    onHideAlbum(title)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                bitmap = rememberAlbumArt(
                    tracks.firstOrNull()?.contentUri,
                    targetPx = 512,
                    albumId = albumId,
                ),
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(8.dp),
                iconSize = 40.dp,
            )
            Spacer(modifier = Modifier.width(20.dp))
            FilledTonalButton(onClick = { onPlay(shown, 0) }, enabled = shown.isNotEmpty()) {
                Icon(imageVector = Lucide.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play")
            }
            if (onShuffle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onShuffle(shown) },
                    enabled = shown.isNotEmpty(),
                ) {
                    Icon(imageVector = Lucide.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }
        }

        if (tracks.isNotEmpty() && shown.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No songs in this album match \u201c$filter\u201d.",
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
            verticalArrangement = Arrangement.Top,
        ) {
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
