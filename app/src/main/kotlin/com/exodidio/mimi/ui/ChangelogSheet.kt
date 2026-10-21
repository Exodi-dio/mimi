package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * What changed in this build, as a sheet over the About page.
 *
 * The sheet is titled with the version that is installed, because a list of changes
 * without the build it belongs to cannot be matched to the build on the phone. The lines
 * are kept short and closed by default, so the sheet says what changed without burying
 * the page it was opened from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogSheet(
    version: String,
    onDismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = "What's new in $version",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            changelogSections.forEach { section ->
                ChangelogSectionBlock(section = section, expanded = expanded)
            }

            val hasMore = changelogSections.any { changelogHasMore(it.entries) }
            if (hasMore) {
                Spacer(modifier = Modifier.height(16.dp))
                ChangelogPill(expanded = expanded, onToggle = { expanded = !expanded })
            }
        }
    }
}

/**
 * One heading and the lines under it.
 *
 * The heading is small, capitalised and letterspaced so it reads as a label rather than
 * as another line of prose, which is how the settings pages mark their sections too.
 */
@Composable
private fun ChangelogSectionBlock(
    section: ChangelogSection,
    expanded: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = section.label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
        )

        visibleChangelogEntries(section.entries, expanded).forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
            ) {
                Text(
                    text = "\u2022",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Text(
                    text = entry,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * The pill that opens the rest of the lines up.
 *
 * A pill rather than a plain button because it is not an action on the page but a
 * control on the sheet itself, and the sheet is a sheet rather than a page.
 */
@Composable
private fun ChangelogPill(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = changelogToggleLabel(expanded),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
