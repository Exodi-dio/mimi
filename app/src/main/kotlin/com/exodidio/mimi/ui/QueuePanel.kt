package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.ListX
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.X
import com.exodidio.mimi.playback.QueueItem

/**
 * What plays next, in order.
 *
 * Reordering uses arrows rather than a drag handle. A drag would mean either another
 * dependency or hand-rolled pointer arithmetic, and neither is worth carrying while this
 * screen is still going to be rebuilt by the skin. A drag would call the same engine
 * method the arrows call, so only this file changes when the skin lands.
 */
@Composable
fun QueuePanel(
    queue: List<QueueItem>,
    currentIndex: Int,
    shuffled: Boolean,
    onItemClick: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSaveAsPlaylist: () -> Unit,
    onClearUpcoming: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // A long queue is a queue you cannot find yourself in, so it opens on the song that is
    // playing rather than at the very top. The same happens when the song changes, and when
    // a row is tapped — the list follows the music instead of the music getting lost in it.
    LaunchedEffect(currentIndex) {
        if (currentIndex in queue.indices) listState.scrollToItem(currentIndex)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Up next",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (shuffled) {
                // The list below is the loaded order; shuffle decides what actually comes
                // out of it. Saying so beats a list that quietly lies about what is next.
                Icon(
                    imageVector = Lucide.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Shuffled",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }, enabled = queue.isNotEmpty()) {
                    Icon(
                        imageVector = Lucide.Ellipsis,
                        contentDescription = "Queue options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Keep as a playlist") },
                        leadingIcon = { Icon(Lucide.Save, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onSaveAsPlaylist()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Clear what is next") },
                        leadingIcon = { Icon(Lucide.ListX, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onClearUpcoming()
                        },
                    )
                }
            }
        }

        if (queue.isEmpty()) {
            Text(
                text = "Nothing queued.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(
                    items = queue,
                    // The same song can sit in the queue twice, so the position is part of
                    // the key. Keying on the media id alone would collapse the two rows.
                    key = { index, item -> "$index:${item.mediaId}" },
                ) { index, item ->
                    QueueRow(
                        item = item,
                        position = index + 1,
                        isCurrent = index == currentIndex,
                        canMoveUp = index > 0,
                        canMoveDown = index < queue.lastIndex,
                        onClick = { onItemClick(index) },
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                        onRemove = { onRemove(index) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    item: QueueItem,
    position: Int,
    isCurrent: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.width(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(item.artist, item.album).joinToString(" \u00b7 ")
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
        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Lucide.ArrowUp,
                contentDescription = "Move up",
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Lucide.ArrowDown,
                contentDescription = "Move down",
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = "Remove from queue",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
