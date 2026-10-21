package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.Settings

/**
 * Editing what separates one credit from the next.
 *
 * The field holds the separators as a comma-separated line because that is the one way to
 * type a list on a phone keyboard without inventing a second editor. A comma therefore
 * cannot itself be a separator here, which is no loss: it is the mark that causes the most
 * damage, and adding it is a decision worth making deliberately rather than by accident.
 *
 * Saving is explicit because a half-typed list would otherwise regroup the library on every
 * keystroke. Blank entries are dropped rather than saved, so a stray comma costs nothing.
 */
@Composable
fun CreditSeparatorsDialog(
    separators: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(separators) { mutableStateOf(separators.joinToString(", ")) }

    val entries = text.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split names at") },
        text = {
            Column {
                Text(
                    text = "Marks and words that separate one artist or genre from the next. " +
                        "In this field they are typed comma separated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("; / & feat. ft. featuring") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                Text(
                    text = "A comma is off by default because it would split " +
                        "Earth, Wind & Fire into three artists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(entries) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(Settings.DEFAULT_CREDIT_SEPARATORS) }) {
                    Text("Reset")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
