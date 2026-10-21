package com.exodidio.mimi.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.exodidio.mimi.library.Settings

/**
 * The key a library section's remembered scroll position is filed under.
 *
 * The order and the hidden names are part of the key, not an extra check: a remembered
 * offset means nothing in a list that has been reordered or filtered, so changing either
 * starts the section at the top instead of dropping the listener into an arbitrary place.
 *
 * Pure, so the discarding rule can be checked without a device.
 */
fun libraryScrollKey(section: LibrarySection, settings: Settings): String {
    val order = when (section) {
        LibrarySection.SONGS ->
            "${settings.songSort.name}:${settings.sortDescending}"

        LibrarySection.ALBUMS ->
            "${settings.albumSort.name}:${settings.sortDescending}"

        LibrarySection.ARTISTS ->
            "${settings.artistSort.name}:${settings.sortDescending}"

        LibrarySection.PLAYLISTS,
        LibrarySection.GENRES,
        LibrarySection.COMPOSERS,
        LibrarySection.YEARS,
        LibrarySection.FOLDERS,
        -> "listed"
    }
    val hidden = (settings.hiddenArtists + settings.hiddenAlbums)
        .map { it.trim().lowercase() }
        .sorted()
        .joinToString(",")
    return "${section.name}|$order|$hidden"
}

/**
 * A list state that starts where the section was left and reports where it goes.
 *
 * The position is held by the navigator rather than by this composable because the
 * composable is thrown away when a detail screen opens; a value remembered here would be
 * gone by the time the listener came back, which is exactly when it is wanted.
 */
@Composable
fun rememberKeptListState(
    initialIndex: Int,
    onIndexChange: (Int) -> Unit,
): LazyListState {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val keep by rememberUpdatedState(onIndexChange)
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.collect { keep(it) }
    }
    return state
}

/** The grid equivalent, for the albums shelf. */
@Composable
fun rememberKeptGridState(
    initialIndex: Int,
    onIndexChange: (Int) -> Unit,
): LazyGridState {
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)
    val keep by rememberUpdatedState(onIndexChange)
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.collect { keep(it) }
    }
    return state
}
