package com.exodidio.mimi.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Which slice of the library is on screen: songs, albums, artists or playlists.
 *
 * Deliberately not a set of tabs. Switching between these is not going anywhere, it is
 * looking at the same collection from a different angle, so it must not push a screen onto
 * the back stack — walking back out of the library should take one press, not four.
 */
@Composable
fun LibrarySectionSwitcher(
    selected: LibrarySection,
    onSelect: (LibrarySection) -> Unit,
    modifier: Modifier = Modifier,
) = PillRow(
    options = LibrarySection.entries,
    selected = selected,
    label = { it.label },
    onSelect = onSelect,
    modifier = modifier,
)
