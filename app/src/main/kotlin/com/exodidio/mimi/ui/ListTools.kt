package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import com.exodidio.mimi.library.AlbumSort
import com.exodidio.mimi.library.ArtistSort
import com.exodidio.mimi.library.DetailSort
import com.exodidio.mimi.library.SongSort

/**
 * One sheet that picks the field and the direction together.
 *
 * Splitting them into two menus made the second choice a guess: a listener who had just
 * chosen "Album" still had to find a separate switch to know which way it ran. The
 * direction here is the same remembered setting every sorted list reads, and it is
 * worded as "reverse" because some fields — song counts and dates — already have a
 * natural direction that reversing flips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(
    title: String,
    fields: List<String>,
    selectedIndex: Int,
    descending: Boolean,
    onField: (Int) -> Unit,
    onDescending: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)

            fields.forEachIndexed { index, label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onField(index) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = index == selectedIndex,
                        onClick = { onField(index) },
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reverse the order",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Applies to every sorted list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = descending, onCheckedChange = onDescending)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

/** The icon that opens a sort sheet, with the sheet itself living beside it. */
@Composable
fun HeaderSortButton(
    title: String,
    fields: List<String>,
    selectedIndex: Int,
    descending: Boolean,
    onField: (Int) -> Unit,
    onDescending: (Boolean) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    IconButton(onClick = { open = true }) {
        Icon(
            imageVector = Lucide.ListFilter,
            contentDescription = "Sort",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (open) {
        SortSheet(
            title = title,
            fields = fields,
            selectedIndex = selectedIndex,
            descending = descending,
            onField = onField,
            onDescending = onDescending,
            onDismiss = { open = false },
        )
    }
}

/** Sort control for the songs list. */
@Composable
fun SongSortButton() {
    val settings = LocalSettings.current
    val change = LocalSettingsChange.current
    HeaderSortButton(
        title = "Sort songs",
        fields = SongSort.entries.map { it.label },
        selectedIndex = settings.songSort.ordinal,
        descending = settings.sortDescending,
        onField = { index ->
            change { it.copy(songSort = SongSort.entries[index]) }
        },
        onDescending = { value -> change { it.copy(sortDescending = value) } },
    )
}

/** Sort control for the albums grid. */
@Composable
fun AlbumSortButton() {
    val settings = LocalSettings.current
    val change = LocalSettingsChange.current
    HeaderSortButton(
        title = "Sort albums",
        fields = AlbumSort.entries.map { it.label },
        selectedIndex = settings.albumSort.ordinal,
        descending = settings.sortDescending,
        onField = { index ->
            change { it.copy(albumSort = AlbumSort.entries[index]) }
        },
        onDescending = { value -> change { it.copy(sortDescending = value) } },
    )
}

/** Sort control for the artists list. */
@Composable
fun ArtistSortButton() {
    val settings = LocalSettings.current
    val change = LocalSettingsChange.current
    HeaderSortButton(
        title = "Sort artists",
        fields = ArtistSort.entries.map { it.label },
        selectedIndex = settings.artistSort.ordinal,
        descending = settings.sortDescending,
        onField = { index ->
            change { it.copy(artistSort = ArtistSort.entries[index]) }
        },
        onDescending = { value -> change { it.copy(sortDescending = value) } },
    )
}

/** Sort control for any list inside a browse screen. */
@Composable
fun DetailSortButton() {
    val settings = LocalSettings.current
    val change = LocalSettingsChange.current
    HeaderSortButton(
        title = "Sort this list",
        fields = DetailSort.entries.map { it.label },
        selectedIndex = settings.detailSort.ordinal,
        descending = settings.sortDescending,
        onField = { index ->
            change { it.copy(detailSort = DetailSort.entries[index]) }
        },
        onDescending = { value -> change { it.copy(sortDescending = value) } },
    )
}

/**
 * The field under a list header's magnifier.
 *
 * The line underneath says how many songs survived the filter, which is the difference
 * between a list that is still filtered and a library that has lost songs. The trailing
 * button clears the text first and closes the field second, so one press never throws
 * away both at once.
 */
@Composable
fun ListSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search this list") },
            leadingIcon = { Icon(Lucide.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { if (query.isEmpty()) onClose() else onQueryChange("") }) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = if (query.isEmpty()) "Close list search" else "Clear list search",
                    )
                }
            },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
