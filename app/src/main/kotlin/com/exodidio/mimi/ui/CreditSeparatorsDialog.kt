package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import com.exodidio.mimi.library.Settings
import com.exodidio.mimi.library.addCreditSeparator
import com.exodidio.mimi.library.splitCredits

/** Separators worth offering, including the comma that the old one-line field could not hold. */
private val SUGGESTED_CREDIT_SEPARATORS = listOf(
    ",", ";", "/", "&", "+", "×", "feat.", "ft.", "featuring", "with", "vs.", "、",
)

/** A line that shows the consequence of a separator before it is saved. */
private const val PREVIEW_CREDIT = "Earth, Wind & Fire feat. Someone"

/**
 * Editing what separates one credit from the next.
 *
 * The list is built one chip at a time rather than typed as a comma-separated line, because
 * a line uses the comma as its own separator and so cannot hold the comma as a value. A
 * comma is the mark with the most visible consequence in a library, and this is the place
 * that consequence is chosen deliberately.
 *
 * Edits are staged and written only on Save, so a half-built list never regroups the
 * library and dismissing the dialog leaves the setting exactly as it was. The preview is
 * split with the same function the library uses, so it cannot promise a result the library
 * will not produce.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreditSeparatorsDialog(
    separators: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember(separators) { mutableStateOf(separators) }
    var draft by remember { mutableStateOf("") }

    fun add(value: String) {
        chosen = addCreditSeparator(chosen, value)
        draft = ""
    }

    val suggestions = SUGGESTED_CREDIT_SEPARATORS
        .filterNot { suggestion -> chosen.any { it.equals(suggestion, ignoreCase = true) } }
    val names = splitCredits(PREVIEW_CREDIT, chosen)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split names at") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Marks and words that separate one artist or genre from the next.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (chosen.isEmpty()) {
                    Text(
                        text = "Nothing is split, so every name stays as it was tagged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        chosen.forEach { separator ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    chosen = chosen.filterNot {
                                        it.equals(separator, ignoreCase = true)
                                    }
                                },
                                label = { Text(separator) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Lucide.X,
                                        contentDescription = "Remove $separator",
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        label = { Text("Add a mark or word") },
                        placeholder = { Text("feat.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { add(draft) }),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { add(draft) },
                        enabled = draft.isNotBlank(),
                    ) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = "Add separator",
                        )
                    }
                }

                if (suggestions.isNotEmpty()) {
                    Text(
                        text = "Often used",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        suggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { add(suggestion) },
                                label = { Text(suggestion) },
                            )
                        }
                    }
                }

                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = PREVIEW_CREDIT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = names.joinToString("  ·  ").ifEmpty { "No names" },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = "Adding a comma splits Earth, Wind & Fire into three artists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(chosen) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { chosen = Settings.DEFAULT_CREDIT_SEPARATORS },
                ) {
                    Text("Reset to defaults")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
