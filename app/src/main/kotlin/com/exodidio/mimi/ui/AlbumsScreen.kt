package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Shuffle
import com.exodidio.mimi.library.AlbumSummary

/**
 * The albums shelf, as a grid of covers.
 *
 * The covers arrive already filtered and ordered: hiding an album or reversing the order
 * happens where the library is read, so this screen only has to draw what it is given. The
 * random button plays one of the albums on show, which is why it lives here rather than
 * next to the transport.
 */
@Composable
fun AlbumsScreen(
    albums: List<AlbumSummary>,
    scanning: Boolean,
    onAlbumClick: (AlbumSummary) -> Unit,
    modifier: Modifier = Modifier,
    onPlayRandomAlbum: (() -> Unit)? = null,
    initialScrollIndex: Int = 0,
    onScrollIndex: (Int) -> Unit = {},
) {
    val gridState = rememberKeptGridState(initialScrollIndex, onScrollIndex)

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Albums",
            subtitle = if (scanning) "Scanning\u2026" else albumCountLabel(albums.size),
            trailing = {
                Row {
                    if (onPlayRandomAlbum != null) {
                        IconButton(onClick = onPlayRandomAlbum, enabled = albums.isNotEmpty()) {
                            Icon(
                                imageVector = Lucide.Shuffle,
                                contentDescription = "Play a random album",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    AlbumSortButton()
                }
            },
        )

        if (albums.isEmpty()) {
            EmptyLibraryMessage(scanning, modifier = Modifier.weight(1f))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                state = gridState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(albums, key = { _, album -> album.albumId }) { _, album ->
                    AlbumCell(album = album, onClick = { onAlbumClick(album) })
                }
            }
        }
    }
}
