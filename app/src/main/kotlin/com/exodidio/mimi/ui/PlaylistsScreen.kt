package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.exodidio.mimi.library.PlaylistSummary

/**
 * Playlists, with the two automatic lists above the ones that were made by hand.
 *
 * "Last added" and "Recently played" are not playlists in the database: they are views
 * whose period is chosen in settings, so they are drawn apart from the saved ones rather
 * than being able to look like something that can be renamed or deleted.
 */
@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistSummary>,
    lastAddedCount: Int,
    recentlyPlayedCount: Int,
    onPlaylistClick: (PlaylistSummary) -> Unit,
    onOpenAutoPlaylist: (AutoPlaylistKind) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Playlists",
            subtitle = playlistCountLabel(playlists.size),
            trailing = {
                IconButton(onClick = onCreate) {
                    Icon(
                        imageVector = Lucide.Plus,
                        contentDescription = "New playlist",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            item(key = "title:automatic") {
                ListSectionTitle("Automatic")
            }
            item(key = "auto:last_added") {
                AutoPlaylistRow(
                    icon = Lucide.Clock,
                    title = AutoPlaylistKind.LAST_ADDED.title,
                    count = lastAddedCount,
                    onClick = { onOpenAutoPlaylist(AutoPlaylistKind.LAST_ADDED) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item(key = "auto:recently_played") {
                AutoPlaylistRow(
                    icon = Lucide.ListMusic,
                    title = AutoPlaylistKind.RECENTLY_PLAYED.title,
                    count = recentlyPlayedCount,
                    onClick = { onOpenAutoPlaylist(AutoPlaylistKind.RECENTLY_PLAYED) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            item(key = "title:yours") {
                ListSectionTitle("Yours")
            }

            if (playlists.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Lucide.ListMusic,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No playlists yet",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "Make one, then add songs from your library.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistRow(playlist = playlist, onClick = { onPlaylistClick(playlist) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

/** One automatic list: a name and how many songs the chosen period holds. */
@Composable
private fun AutoPlaylistRow(
    icon: ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = songCountLabel(count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaylistRow(playlist: PlaylistSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Lucide.ListMusic,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = songCountLabel(playlist.trackCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
