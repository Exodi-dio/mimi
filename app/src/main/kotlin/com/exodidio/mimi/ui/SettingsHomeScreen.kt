package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search

/**
 * The Settings destination: a search box, then every page of settings by what it is about.
 *
 * The pages are filed under four plain questions — how it plays, what is in the library,
 * how it looks, and what is stored — rather than in the order the enum happens to declare,
 * because this is the one screen somebody arrives at without knowing what they want.
 *
 * Search is the first row rather than a field at the top because it opens a screen of its
 * own; a field here would either search nothing visible or rebuild the whole index below
 * it on every keystroke.
 */
@Composable
fun SettingsHomeScreen(
    onOpenSearch: () -> Unit,
    onOpenGroup: (SettingsGroupId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SettingsPageHeader(
            title = "Settings",
            subtitle = "Every page of settings. Search when you know the word but not the page.",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "search") {
                SettingsRowBackground(first = true, last = true) {
                    SearchRow(onClick = onOpenSearch)
                }
            }

            SettingsPageGroups.forEach { (heading, pages) ->
                item(key = "heading:$heading") {
                    SettingsSectionHeading(heading)
                }
                item(key = "card:$heading") {
                    SettingsRowBackground(first = true, last = true) {
                        pages.forEachIndexed { index, group ->
                            PageRow(
                                group = group,
                                onClick = { onOpenGroup(group) },
                            )
                            if (index != pages.lastIndex) SettingsRowDivider()
                        }
                    }
                }
            }
        }
    }
}

/** The pages, in the order the index reads them, grouped by the question they answer. */
private val SettingsPageGroups: List<Pair<String, List<SettingsGroupId>>> = listOf(
    "PLAYBACK" to listOf(SettingsGroupId.AUDIO, SettingsGroupId.NOW_PLAYING),
    "LIBRARY" to listOf(SettingsGroupId.LIBRARY, SettingsGroupId.PLAYLISTS),
    "APPEARANCE" to listOf(SettingsGroupId.IMAGES, SettingsGroupId.CUSTOMIZATION),
    "DATA" to listOf(SettingsGroupId.BACKUP, SettingsGroupId.ABOUT),
)

/** The way into the search screen, drawn as the first row of the destination. */
@Composable
private fun SearchRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Lucide.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Search settings",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                ),
            )
            Text(
                text = "Find a row by name or by what it does.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** One page of settings, named by the page itself and described by its own subtitle. */
@Composable
private fun PageRow(group: SettingsGroupId, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                ),
            )
            Text(
                text = group.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}
