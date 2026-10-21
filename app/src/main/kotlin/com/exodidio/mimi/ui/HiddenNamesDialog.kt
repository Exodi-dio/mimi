package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The names hidden from the library, each with the way back.
 *
 * Hiding is done from the artist or album screen, where the name is in front of the
 * listener. This list is where it is undone, because a hidden name has no row left to
 * long-press and would otherwise be unreachable.
 */
@Composable
fun HiddenNamesDialog(
    artists: List<String>,
    albums: List<String>,
    onUnhideArtist: (String) -> Unit,
    onUnhideAlbum: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hidden artists and albums") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Hidden names are left out of every list, search and count. " +
                        "Their files stay on the device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                if (artists.isEmpty() && albums.isEmpty()) {
                    Text(
                        text = "Nothing is hidden.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (artists.isNotEmpty()) {
                    Text(
                        text = "Artists",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    artists.forEach { name ->
                        HiddenNameRow(name = name, onUnhide = { onUnhideArtist(name) })
                    }
                }

                if (albums.isNotEmpty()) {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    albums.forEach { name ->
                        HiddenNameRow(name = name, onUnhide = { onUnhideAlbum(name) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun HiddenNameRow(name: String, onUnhide: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onUnhide) { Text("Show") }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
