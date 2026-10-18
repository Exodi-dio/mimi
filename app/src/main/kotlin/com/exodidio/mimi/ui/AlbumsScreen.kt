package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.AlbumSummary

@Composable
fun AlbumsScreen(
    albums: List<AlbumSummary>,
    scanning: Boolean,
    onAlbumClick: (AlbumSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Albums",
            subtitle = if (scanning) "Scanning\u2026" else albumCountLabel(albums.size),
        )

        if (albums.isEmpty()) {
            EmptyLibraryMessage(scanning, modifier = Modifier.weight(1f))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(albums, key = { it.albumId }) { album ->
                    AlbumCell(album = album, onClick = { onAlbumClick(album) })
                }
            }
        }
    }
}
