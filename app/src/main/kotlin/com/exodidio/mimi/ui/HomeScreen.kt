package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Shuffle
import com.exodidio.mimi.library.Track

/**
 * What the app opens on.
 *
 * A library is a place you browse when you know what you want; this is for when you do
 * not. It answers three questions in the order they are usually asked — what is new, what
 * was I just listening to, and what do I actually like — and offers to just start playing.
 *
 * A section with nothing in it is left out rather than shown empty. On a fresh library
 * that means the only thing on screen is "Shuffle all", which is honest: there is a
 * library, and there is nothing to say about how it has been used yet.
 */
@Composable
fun HomeScreen(
    recentlyAdded: List<Track>,
    recentlyPlayed: List<Track>,
    mostPlayed: List<Track>,
    onTrackClick: (List<Track>, Int) -> Unit,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier,
    hasLibrary: Boolean = true,
) {
    val sections = listOfNotNull(
        recentlyAdded.takeIf { it.isNotEmpty() }?.let { "Recently added" to it },
        recentlyPlayed.takeIf { it.isNotEmpty() }?.let { "Recently played" to it },
        mostPlayed.takeIf { it.isNotEmpty() }?.let { "Most played" to it },
    )

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Home")

        if (!hasLibrary) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No music yet.\nOnce your library has been read, this is where it starts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                ShuffleAllRow(onClick = onShuffleAll)
            }

            sections.forEach { (title, tracks) ->
                item(key = "title:$title") {
                    ListSectionTitle(title)
                }
                item(key = "strip:$title") {
                    TrackStrip(tracks = tracks, onTrackClick = onTrackClick)
                }
            }
        }
    }
}

@Composable
private fun ShuffleAllRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Lucide.Shuffle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Shuffle all",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** One row of covers that scrolls sideways, which is how a section stays one screen tall. */
@Composable
private fun TrackStrip(tracks: List<Track>, onTrackClick: (List<Track>, Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackCard(track = track, onClick = { onTrackClick(tracks, index) })
        }
    }
}

@Composable
private fun TrackCard(track: Track, onClick: () -> Unit) {
    Column(modifier = Modifier.width(136.dp).clickable(onClick = onClick)) {
        Artwork(
            // Keyed by album for the same reason the song list is: every track on an album
            // carries the same picture, so one read covers the whole row.
            bitmap = rememberAlbumArt(
                mediaId = track.contentUri,
                targetPx = 384,
                cacheKey = track.albumId.takeIf { it != 0L }?.let { "album:$it" },
            ),
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = RoundedCornerShape(8.dp),
            iconSize = 32.dp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val subtitle = track.artist
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
