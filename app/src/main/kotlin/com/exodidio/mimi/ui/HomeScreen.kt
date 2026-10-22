package com.exodidio.mimi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.HomeRowId
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.homeRowsIn

/**
 * What the app opens on.
 *
 * A library is a place you browse when you know what you want; this is for when you do
 * not. It answers the questions in the order they are usually asked — what is new, what
 * was I just listening to, what do I actually like, and what have I marked myself.
 *
 * The menu and the profile sit on the same line as the title, because both are about the
 * app rather than about the music: either one opens the drawer, and the drawer's header is
 * the profile.
 *
 * The shelves are drawn in [homeRowsIn] order and only when they hold something, so hiding
 * one, moving one, or having nothing to put in it all leave a screen that still reads.
 */
@Composable
fun HomeScreen(
    recentlyAdded: List<Track>,
    recentlyPlayed: List<Track>,
    mostPlayed: List<Track>,
    loved: List<Track>,
    onTrackClick: (List<Track>, Int) -> Unit,
    onOpenMenu: () -> Unit,
    avatar: ImageBitmap?,
    profileName: String,
    modifier: Modifier = Modifier,
    hasLibrary: Boolean = true,
    onSeeAll: (HomeRowId) -> Unit = {},
) {
    val settings = LocalSettings.current
    val byRow = mapOf(
        HomeRowId.RECENTLY_ADDED to recentlyAdded,
        HomeRowId.RECENTLY_PLAYED to recentlyPlayed,
        HomeRowId.MOST_PLAYED to mostPlayed,
        HomeRowId.LOVED to loved,
    )
    val shelves = homeRowsIn(settings.homeRowOrder, settings.hiddenHomeRows)
        .mapNotNull { row ->
            val tracks = byRow[row].orEmpty()
            if (tracks.isEmpty()) null else row to tracks
        }
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        CollapsingHeader(
            title = "Home",
            listState = listState,
            subtitle = profileName.ifBlank { null },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenMenu) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(2.dp)
                                        .background(MaterialTheme.colorScheme.onSurface),
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable(onClick = onOpenMenu),
                    ) {
                        ProfileAvatar(image = avatar, name = profileName, size = 32.dp)
                    }
                }
            },
        )

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
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp),
        ) {
            shelves.forEach { (row, tracks) ->
                item(key = "title:${row.name}") {
                    SectionShelfTitle(title = row.title, onSeeAll = { onSeeAll(row) })
                }
                item(key = "strip:${row.name}") {
                    TrackStrip(tracks = tracks, onTrackClick = onTrackClick)
                }
            }
        }
    }
}

/**
 * A shelf heading with the way to the whole of what it is a shelf of.
 *
 * "See all" is drawn in the accent so it reads as the one thing on the line that does
 * something, rather than as another label.
 */
@Composable
private fun SectionShelfTitle(title: String, onSeeAll: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "See all",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSeeAll)
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
                albumId = track.albumId,
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
