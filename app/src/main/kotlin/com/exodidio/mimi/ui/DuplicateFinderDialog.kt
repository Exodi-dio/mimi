package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.DuplicateGroup

/**
 * The copies the finder found, with enough detail to tell them apart.
 *
 * The path and size are shown for every copy because two files with the same title and
 * length are otherwise impossible to choose between. Nothing is deleted here; the screen
 * only answers "what is duplicated", and deleting files is a decision for a file manager.
 */
@Composable
fun DuplicateFinderDialog(
    groups: List<DuplicateGroup>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find duplicates") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Tracks sharing a title, artist and a length within two seconds. " +
                        "Nothing is deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                if (groups.isEmpty()) {
                    Text(
                        text = "No duplicates were found.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                groups.forEach { group ->
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = group.artist.orEmpty().ifEmpty { "Unknown artist" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    group.tracks.forEach { track ->
                        Text(
                            text = trackFilePath(track),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = formatFileSize(track.sizeBytes) + " \u00b7 " +
                                formatDuration(track.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
