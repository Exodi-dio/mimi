package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.exodidio.mimi.library.Track
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    tracks: List<Track>,
    scanning: Boolean,
    favoriteIds: Set<Long>,
    onToggleLove: (Long) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onRefresh: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    initialScrollIndex: Int = 0,
    onScrollIndex: (Int) -> Unit = {},
    playingMediaId: String? = null,
    modifier: Modifier = Modifier,
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("") }
    val shown = remember(tracks, filter) { tracks.matchingListFilter(filter) }
    val listState = rememberKeptListState(initialScrollIndex, onScrollIndex)
    val scope = rememberCoroutineScope()
    val stops = remember(shown) { alphabetStops(shown.map { it.title }) }
    val rail = alphabetRailVisible(shown.size)
    val active = activeAlphabetLetter(stops, listState.firstVisibleItemIndex)

    Column(modifier = modifier.fillMaxSize()) {
        CollapsingHeader(
            title = "Songs",
            listState = listState,
            subtitle = when {
                scanning -> "Scanning\u2026"
                filter.isNotBlank() -> listFilterLabel(shown.size, tracks.size)
                else -> songSummaryLabel(tracks.size, totalDurationMs(tracks))
            },
            trailing = {
                Row {
                    IconButton(onClick = { searching = !searching; if (!searching) filter = "" }) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = if (searching) "Close list search" else "Search this list",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SongSortButton()
                    TextButton(onClick = onRefresh, enabled = !scanning) {
                        Text("Refresh")
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

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (scanning) "Scanning your music\u2026" else "No music found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (shown.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No songs in this list match \u201c$filter\u201d.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        end = if (rail) 28.dp else 0.dp,
                        bottom = 24.dp,
                    ),
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
                    }
                }

                if (rail) {
                    AlphabetRail(
                        stops = stops,
                        active = active,
                        onJump = { index -> scope.launch { listState.animateScrollToItem(index) } },
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        }
    }
}

/** Reused by the browsing screens so every empty state reads the same. */
@Composable
fun EmptyLibraryMessage(scanning: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = if (scanning) "Scanning your music\u2026" else "No music found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}
