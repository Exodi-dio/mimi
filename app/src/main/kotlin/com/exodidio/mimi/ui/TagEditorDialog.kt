package com.exodidio.mimi.ui

import android.app.Activity
import android.content.IntentSender
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.TagField
import com.exodidio.mimi.library.TagInput
import com.exodidio.mimi.library.TagValues
import com.exodidio.mimi.library.TagWriter
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.differencesFrom
import com.exodidio.mimi.library.readTagInput
import kotlinx.coroutines.launch

/**
 * What the tag editor is editing.
 *
 * An album is a list of songs rather than one song, because the tags of a record are
 * written to every file of it in one go: a record whose files disagree about its name
 * is not a record.
 */
sealed interface TagEditorTarget {

    /** One song. */
    data class Song(val track: Track) : TagEditorTarget

    /** Every song of an album. */
    data class Album(val tracks: List<Track>) : TagEditorTarget
}

/**
 * What came of editing the tags of a song or of an album.
 *
 * The two that matter are whether the file itself now holds the tags and whether the app
 * is keeping a correction of its own, because the listener needs to know which of the two
 * happened before they go and look at the file.
 */
sealed interface TagEditOutcome {

    /** The file holds the new tags. */
    data class Written(val values: TagValues) : TagEditOutcome

    /** The platform would not write the file, so the app is keeping the correction. */
    data class KeptInLibrary(val values: TagValues) : TagEditOutcome

    /** The listener did not allow the write, so nothing was changed. */
    data object Refused : TagEditOutcome

    /** Nothing was different, so there was nothing to write. */
    data object Unchanged : TagEditOutcome
}

/**
 * The editor for a song's tags, and for an album's.
 *
 * Which fields it shows is decided by the caller rather than by the editor, because a
 * song can be given a name and a genre while an album is given a record and a year, and
 * the two are the same editor with a different set of boxes.
 *
 * The writing is the system's business rather than the editor's: the system asks the
 * listener whether the app may write to the file, and the editor only puts the request in
 * front of them. What happens when the system says no is the caller's business, and the
 * editor is told which of the two happened so that it can say so.
 */
@Composable
fun TagEditorDialog(
    heading: String,
    initial: TagValues,
    fields: List<TagField>,
    uris: List<Uri>,
    onApply: suspend (TagValues) -> Boolean,
    onKeep: suspend (TagValues) -> Unit,
    onDismiss: () -> Unit,
    onFinished: (TagEditOutcome) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember(initial) { mutableStateOf(initial.title) }
    var artist by remember(initial) { mutableStateOf(initial.artist.orEmpty()) }
    var album by remember(initial) { mutableStateOf(initial.album.orEmpty()) }
    var albumArtist by remember(initial) { mutableStateOf(initial.albumArtist.orEmpty()) }
    var year by remember(initial) { mutableStateOf(initial.year?.toString().orEmpty()) }
    var trackNumber by remember(initial) { mutableStateOf(initial.trackNumber?.toString().orEmpty()) }
    var genre by remember(initial) { mutableStateOf(initial.genre.orEmpty()) }
    var problem by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<TagValues?>(null) }

    val consent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val values = pending
        pending = null
        if (result.resultCode != Activity.RESULT_OK || values == null) {
            onFinished(TagEditOutcome.Refused)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val written = onApply(values)
            if (written) {
                onFinished(TagEditOutcome.Written(values))
            } else {
                onKeep(values)
                onFinished(TagEditOutcome.KeptInLibrary(values))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (TagField.TITLE in fields) {
                    TagFieldBox("Title", title, { title = it }, KeyboardType.Text)
                }
                if (TagField.ARTIST in fields) {
                    TagFieldBox("Artist", artist, { artist = it }, KeyboardType.Text)
                }
                if (TagField.ALBUM in fields) {
                    TagFieldBox("Album", album, { album = it }, KeyboardType.Text)
                }
                if (TagField.ALBUM_ARTIST in fields) {
                    TagFieldBox("Album artist", albumArtist, { albumArtist = it }, KeyboardType.Text)
                }
                if (TagField.YEAR in fields) {
                    TagFieldBox("Year", year, { year = it }, KeyboardType.Number)
                }
                if (TagField.TRACK_NUMBER in fields) {
                    TagFieldBox("Track number", trackNumber, { trackNumber = it }, KeyboardType.Number)
                }
                if (TagField.GENRE in fields) {
                    TagFieldBox("Genre", genre, { genre = it }, KeyboardType.Text)
                }

                problem?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Text(
                    text = "The tags are written into the file when the system allows it. " +
                        "If it does not, the app keeps them instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val input = readTagInput(
                        title = title,
                        artist = artist,
                        album = album,
                        albumArtist = albumArtist,
                        year = year,
                        trackNumber = trackNumber,
                        genre = genre,
                    )
                    when (input) {
                        is TagInput.Invalid -> problem = input.message
                        is TagInput.Valid -> {
                            val values = input.values
                            val changed = values.differencesFrom(initial)
                                .intersect(fields.toSet())
                            if (changed.isEmpty()) {
                                onFinished(TagEditOutcome.Unchanged)
                                return@TextButton
                            }
                            val sender: IntentSender? = TagWriter.writeRequest(context, uris)
                            if (sender == null) {
                                // Before Android 11 the system will not let an app write
                                // into a file it does not own at all, so there is nothing
                                // to ask about and the correction is kept in the app.
                                scope.launch {
                                    onKeep(values)
                                    onFinished(TagEditOutcome.KeptInLibrary(values))
                                }
                            } else {
                                pending = values
                                consent.launch(
                                    IntentSenderRequest.Builder(sender).build()
                                )
                            }
                        }
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * One box of the editor.
 *
 * The year and the track number are given a number keyboard because they are numbers,
 * which is a small kindness on a phone rather than a rule: whatever is typed is still
 * read and checked before it is used.
 */
@Composable
private fun TagFieldBox(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}
