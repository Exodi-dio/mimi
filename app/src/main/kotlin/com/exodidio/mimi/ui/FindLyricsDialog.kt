package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.lyrics.LyricsCandidate
import com.exodidio.mimi.lyrics.LyricsQuery
import kotlinx.coroutines.launch

/**
 * Looking for the words of a song that has none of its own.
 *
 * The title and the artist can be corrected before asking, because tags are wrong often
 * enough that a search which only works on tidy files would only work on half a library.
 * The length is shown and not editable: it comes from the player, which knows it, and a
 * confidently wrong length would rule out the right answer.
 *
 * A find is offered as a list rather than chosen for you. Two databases disagree, a search
 * can return the live version of a song when a studio one was asked for, and the words are
 * easy to glance at and hard to describe, so tapping one shows the whole thing before it is
 * kept.
 */
@Composable
fun FindLyricsDialog(
    question: LyricsQuery,
    onKept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as MimiApplication }
    val scope = rememberCoroutineScope()

    var title by remember(question.key) { mutableStateOf(question.title) }
    var artist by remember(question.key) {
        mutableStateOf(question.artist?.substringBefore(',')?.trim().orEmpty())
    }
    var results by remember(question.key) { mutableStateOf<List<LyricsCandidate>>(emptyList()) }
    var searching by remember(question.key) { mutableStateOf(false) }
    var searched by remember(question.key) { mutableStateOf(false) }
    var preview by remember(question.key) { mutableStateOf<LyricsCandidate?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 4.dp) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Find lyrics", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Asks two public lyrics databases. Nothing is sent but what is in these fields, and only when you search.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Length " + formatDuration(question.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        preview = null
                        searching = true
                        scope.launch {
                            results = app.lyricsFinder.search(
                                question.copy(title = title.trim(), artist = artist.trim().ifBlank { null }),
                            )
                            searched = true
                            searching = false
                        }
                    },
                    enabled = title.isNotBlank() && !searching,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (searching) "Searching\u2026" else "Search")
                }

                val shown = preview
                if (shown != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = shown.trackName + " \u00b7 " + shown.provider.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = shown.content, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { preview = null }) { Text("Back") }
                        Button(
                            onClick = {
                                scope.launch {
                                    app.lyricsStore.keep(question.key, shown.content)
                                    onKept()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Keep these words")
                        }
                    }
                } else if (searched && results.isEmpty() && !searching) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nothing found. A shorter title, or just the artist, sometimes finds it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (results.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    results.forEach { candidate ->
                        CandidateRow(candidate = candidate, onClick = { preview = candidate })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: LyricsCandidate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.trackName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = candidate.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = candidate.provider.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (candidate.durationMs > 0) {
                Text(
                    text = formatDuration(candidate.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
