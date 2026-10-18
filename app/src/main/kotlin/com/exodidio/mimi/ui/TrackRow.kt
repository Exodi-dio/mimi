package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.exodidio.mimi.library.Track

/**
 * One song in a list. Shared by the songs list, album detail, artist detail, search
 * results and playlists so a track always looks the same wherever it is found.
 *
 * [onToggleLove], [onAddToPlaylist] and [onRemove] are optional because not every list
 * should change things: an album being previewed can show the song and nothing more,
 * and a playlist offers removal where a search result offers saving.
 */
@Composable
fun TrackRow(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoved: Boolean = false,
    onToggleLove: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    // Songs that name no album share the "unknown album" id, so they key on the file
    // itself rather than on an album they do not have.
    val artKey = track.albumId.takeIf { it != 0L }?.let { "album:$it" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            bitmap = rememberAlbumArt(track.contentUri, targetPx = 128, cacheKey = artKey),
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(4.dp),
            iconSize = 20.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(track.artist, track.album).joinToString(" \u00b7 ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onAddToPlaylist != null) {
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    imageVector = Lucide.ListPlus,
                    contentDescription = "Add to a playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onToggleLove != null) {
            IconButton(onClick = onToggleLove) {
                Icon(
                    imageVector = Lucide.Heart,
                    contentDescription = if (isLoved) "Remove from loved" else "Add to loved",
                    tint = if (isLoved) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = "Remove from this playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
