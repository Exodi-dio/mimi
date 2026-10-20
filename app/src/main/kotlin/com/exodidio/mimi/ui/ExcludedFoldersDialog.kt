package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.FolderSummary
import com.exodidio.mimi.library.isExcludedFolder

/**
 * Choosing which folders are left out of the library.
 *
 * Each tap is written straight through rather than collected up and applied on a button.
 * An excluded folder is a statement about the library, not a draft of one, and a picker
 * with its own unsaved state is a picker that can be dismissed into a lie.
 */
@Composable
fun ExcludedFoldersDialog(
    folders: List<FolderSummary>,
    excluded: List<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Folders to leave out") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Songs in an excluded folder are not added to the library. " +
                        "Nothing is deleted from the device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                if (folders.isEmpty()) {
                    Text(
                        text = "No folders were reported for your music.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                folders.forEach { folder ->
                    val picked = isExcludedFolder(folder.folder, excluded)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(folder.folder) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = picked, onCheckedChange = { onToggle(folder.folder) })
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = folder.folder,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = songCountLabel(folder.trackCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}
