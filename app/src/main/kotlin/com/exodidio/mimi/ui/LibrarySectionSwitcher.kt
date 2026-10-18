package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Songs / Albums / Artists / Playlists.
 *
 * Four sections is one too many for a bottom tab bar, and a fifth tab would leave the
 * bar mostly full of the library itself. Keeping them inside the library means the tabs
 * stay about *places* — library, loved, search, settings — while this row is about how
 * the collection is sliced. A plain row of pills is deliberate: the skin will replace
 * this with whatever the design calls for.
 */
@Composable
fun LibrarySectionSwitcher(
    selected: LibrarySection,
    onSelect: (LibrarySection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibrarySection.entries.forEach { section ->
            val active = section == selected
            Surface(
                onClick = { onSelect(section) },
                shape = CircleShape,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (active) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
