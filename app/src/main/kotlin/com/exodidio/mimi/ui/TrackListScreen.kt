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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Shuffle
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.sortedForDetail

/**
 * A screen that is a title, optionally a picture, and a list of songs.
 *
 * Albums, artists, genres, composers, years and folders are all this screen with different
 * contents, so they share it rather than each growing their own copy of the same list —
 * which is also what keeps a song looking the same wherever it is found.
 *
 * The songs are ordered by the detail-sort choice, except that a playlist's own running
 * order is left alone by its screen. Searching inside the list never changes the queue:
 * playing from a filtered list plays what is on screen.
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
    val settings = LocalSettings.current
    var searching by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("") }

    val ordered = remember(tracks, settings.detailSort, settings.sortDescending) {
        tracks.sortedForDetail(settings.detailSort, settings.sortDescending)
    }
    val shown = remember(ordered, filter) { ordered.matchingListFilter(filter) }
    val stops = remember(shown) { alphabetStops(shown.map { it.title }) }
    val rail = alphabetRailVisible(shown.size)
    val listState = rememberLazyListState()
    val active = activeAlphabetLetter(stops, listState.firstVisibleItemIndex)

    val headerSubtitle = when {
        filter.isNotBlank() -> listFilterLabel(shown.size, tracks.size)
        subtitle != null ->
            "$subtitle \u00b7 ${songSummaryLabel(tracks.size, totalDurationMs(tracks))}"

        else -> songSummaryLabel(tracks.size, totalDurationMs(tracks))
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
                            contentDescription = if (searching) "Close list search" else "Search this list",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DetailSortButton()
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
            if (artworkUri != null) {
                Artwork(
                    bitmap = rememberAlbumArt(artworkUri, targetPx = 512),
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    iconSize = 40.dp,
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            FilledTonalButton(onClick = { onPlay(shown, 0) }, enabled = shown.isNotEmpty()) {
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
                    onClick = { onShuffle(shown) },
                    enabled = shown.isNotEmpty(),
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

        if (shown.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No songs in this list match \u201c$filter\u201d.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@Column
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
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
                        onToggleLove = onToggleLove?.let { love -> { love(track.id) } },
                        onPlayNext = onPlayNext?.let { next -> { next(track) } },
                        onAddToQueue = onAddToQueue?.let { add -> { add(track) } },
                        onAddToPlaylist = onAddToPlaylist?.let { add -> { add(track) } },
                        onRemoveFromPlaylist = onRemoveFromPlaylist?.let { remove -> { remove(track.id) } },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            if (rail) {
                AlphabetRail(
                    stops = stops,
                    active = active,
                    onJump = { index -> listState.animateScrollToItem(index) },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}
