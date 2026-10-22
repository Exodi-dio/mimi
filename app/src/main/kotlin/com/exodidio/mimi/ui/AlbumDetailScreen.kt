package com.exodidio.mimi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
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
    onEditTags: (List<Track>) -> Unit = {},
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
        BarHeader(
            title = title,
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
                                text = { Text("Edit the album tags") },
                                leadingIcon = {
                                    Icon(imageVector = Lucide.Pencil, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    onEditTags(tracks)
                                },
                            )
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

        // The record's own face, large and centred, with its foot fading into the page
        // so the title beneath it reads as part of the same object rather than as a
        // caption stuck under a picture.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.66f)
                    .aspectRatio(1f),
            ) {
                Artwork(
                    bitmap = rememberAlbumArt(
                        tracks.firstOrNull()?.contentUri,
                        targetPx = 512,
                        albumId = albumId,
                    ),
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(8.dp),
                    iconSize = 48.dp,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            )
                        ),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            if (artist != null) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                )
            }
            Text(
                text = headerSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (onShuffle != null) {
                    FilledIconButton(
                        onClick = { onShuffle(shown) },
                        enabled = shown.isNotEmpty(),
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            imageVector = Lucide.Shuffle,
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                FilledIconButton(
                    onClick = { onPlay(shown, 0) },
                    enabled = shown.isNotEmpty(),
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        imageVector = Lucide.Play,
                        contentDescription = "Play",
                        modifier = Modifier.size(24.dp),
                    )
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
                    number = index + 1,
                )
            }
        }
    }
}
