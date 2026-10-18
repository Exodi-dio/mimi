package com.exodidio.mimi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.exodidio.mimi.library.ComposerSummary
import com.exodidio.mimi.library.FolderSummary
import com.exodidio.mimi.library.GenreSummary
import com.exodidio.mimi.library.YearSummary

/**
 * One value in a browsable list — a genre, a composer, a year or a folder.
 *
 * The four sections differ only in what they are grouping by, so they share one row and
 * one screen. [key] is what a detail screen is opened with; [label] is what is shown,
 * which for a folder is its own name rather than the whole path.
 */
data class BrowseEntry(
    val key: String,
    val label: String,
    val trackCount: Int,
    val artworkUri: String? = null,
)

/**
 * The groupings, flattened into the one shape the browser draws.
 *
 * Named one by one rather than as a single overload, because four extensions taking
 * `List<Something>` would erase to the same JVM signature.
 */
fun List<GenreSummary>.genreEntries(): List<BrowseEntry> =
    map { BrowseEntry(it.genre, it.genre, it.trackCount, it.artworkUri) }

fun List<ComposerSummary>.composerEntries(): List<BrowseEntry> =
    map { BrowseEntry(it.composer, it.composer, it.trackCount, it.artworkUri) }

fun List<YearSummary>.yearEntries(): List<BrowseEntry> =
    map { BrowseEntry(it.year.toString(), it.year.toString(), it.trackCount, it.artworkUri) }

/** Folders show their last segment: two folders called `Live` are told apart by their song lists. */
fun List<FolderSummary>.folderEntries(): List<BrowseEntry> =
    map {
        BrowseEntry(
            key = it.folder,
            label = it.folder.substringAfterLast('/').ifEmpty { it.folder },
            trackCount = it.trackCount,
            artworkUri = it.artworkUri,
        )
    }

/**
 * A list of values to drill into. Used by genres, composers, years and folders, which are
 * the same screen with different contents.
 *
 * [leadingIcon] draws a fixed icon instead of cover art, which is what a folder wants:
 * the art of whatever happens to be in it says less than the fact that it is a folder.
 */
@Composable
fun BrowseScreen(
    title: String,
    entries: List<BrowseEntry>,
    onEntryClick: (BrowseEntry) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    emptyMessage: String = "Nothing here yet.",
    leadingIcon: ImageVector? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = title, subtitle = subtitle)

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(entries, key = { it.key }) { entry ->
                    BrowseRow(entry = entry, leadingIcon = leadingIcon, onClick = { onEntryClick(entry) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun BrowseRow(entry: BrowseEntry, leadingIcon: ImageVector?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon == null) {
            Artwork(
                bitmap = rememberAlbumArt(entry.artworkUri, targetPx = 128),
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                iconSize = 20.dp,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = songCountLabel(entry.trackCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
