package com.exodidio.mimi.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.ListEnd
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.ListStart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Trash2
import com.exodidio.mimi.library.Track

/**
 * One song in a list. Shared by the songs list, album detail, artist detail, search
 * results and playlists so a track always looks the same wherever it is found.
 *
 * The heart stays a button of its own because it is the one action taken often enough
 * mid-scroll to deserve a direct tap. Everything else lives behind the overflow, which
 * keeps the row two controls wide however many actions the app grows.
 *
 * The overflow is always offered, because a song's details are always worth asking for —
 * the actions that are not available simply are not listed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoved: Boolean = false,
    onToggleLove: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    // Songs that name no album share the "unknown album" id, so they key on the file
    // itself rather than on an album they do not have.
    val artKey = track.albumId.takeIf { it != 0L }?.let { "album:$it" }
    val actions = LocalTrackActions.current
    val selected = actions.isSelected(track.id)
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val path = trackFilePath(track)

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // A long press picks the song out, and once anything is picked a tap picks
                // rather than plays. Both are decided by the row itself, so every list in
                // the app gained picking at once.
                .combinedClickable(
                    onClick = {
                        if (actions.isSelecting()) actions.onSelect(track) else onClick()
                    },
                    onLongClick = { actions.onSelect(track) },
                )
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    } else {
                        Color.Transparent
                    }
                )
                .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                // Picking is a mode, so it has to be visible on the row itself rather than
                // only in the bar above the list.
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            if (LocalSettings.current.showArtwork) {
                Artwork(
                    bitmap = rememberAlbumArt(
                        mediaId = track.contentUri,
                        targetPx = 128,
                        cacheKey = artKey,
                        albumId = track.albumId,
                    ),
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(4.dp),
                    iconSize = 20.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
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
            Text(
                text = formatDuration(track.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
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
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Lucide.Ellipsis,
                    contentDescription = "More actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            // The path is shown before it is copied so the listener can see which file
            // they are about to act on; two rows can share a title and only this says
            // where each one lives.
            Text(
                text = path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MenuAction("Copy file path", Lucide.Copy) {
                menuOpen = false
                clipboard.setText(AnnotatedString(path))
                Toast.makeText(context, "File path copied", Toast.LENGTH_SHORT).show()
            }
            MenuAction("Song info", Lucide.Info) {
                menuOpen = false
                actions.onShowInfo(track)
            }
            MenuAction("Edit tags", Lucide.Pencil) {
                menuOpen = false
                actions.onEditTags(track)
            }
            if (onPlayNext != null) {
                MenuAction("Play next", Lucide.ListStart) {
                    menuOpen = false
                    onPlayNext()
                }
            }
            if (onAddToQueue != null) {
                MenuAction("Add to queue", Lucide.ListEnd) {
                    menuOpen = false
                    onAddToQueue()
                }
            }
            if (onAddToPlaylist != null) {
                MenuAction("Add to playlist", Lucide.ListPlus) {
                    menuOpen = false
                    onAddToPlaylist()
                }
            }
            if (onMoveUp != null) {
                MenuAction("Move up", Lucide.ArrowUp) {
                    menuOpen = false
                    onMoveUp()
                }
            }
            if (onMoveDown != null) {
                MenuAction("Move down", Lucide.ArrowDown) {
                    menuOpen = false
                    onMoveDown()
                }
            }
            if (onRemoveFromPlaylist != null) {
                MenuAction("Remove from this playlist", Lucide.Trash2) {
                    menuOpen = false
                    onRemoveFromPlaylist()
                }
            }
        }
    }
}

@Composable
private fun MenuAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null) },
        onClick = onClick,
    )
}
