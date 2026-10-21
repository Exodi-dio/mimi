package com.exodidio.mimi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChartBar
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Library
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.SlidersHorizontal

/** The width the drawer is drawn at, which is the roughly sixty percent the skin asks for. */
private val DRAWER_WIDTH = 300.dp

/**
 * The drawer: the places in the app, with the profile no longer one of them.
 *
 * The profile moved to the top right of the home screen, so this panel is only navigation.
 * It names destinations rather than settings sections: Settings is one row, and the pages
 * behind it are found on the Settings index. That is what lets the rows stay nine plain
 * lines with counts only where a count is a useful thing to know before going there.
 *
 * The selected row is the only one filled, and the fill is a pill rather than a rectangle,
 * so the open destination can be found without the panel looking like a list of buttons.
 */
@Composable
fun AppDrawer(
    current: Screen,
    librarySection: LibrarySection,
    queueOpen: Boolean,
    songCount: Int,
    folderCount: Int,
    onOpenLibrary: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenInsights: () -> Unit,
    onScanMedia: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    showInsights: Boolean = true,
) {
    var showDonate by remember { mutableStateOf(false) }

    val onLibrary = current == Screen.Library && librarySection != LibrarySection.FOLDERS
    val onFolders = current == Screen.Library && librarySection == LibrarySection.FOLDERS
    val settingsGroup = (current as? Screen.SettingsGroup)?.group
    val onEqualizer = settingsGroup == SettingsGroupId.AUDIO
    val onStats = current == Screen.Insights
    // Audio and About have rows of their own above and below, so only the remaining
    // settings pages light Settings. Without that, walking in from Equalizer would leave
    // two rows filled at once and the panel would look like it had lost its place.
    val onSettings = current == Screen.SettingsHome ||
        current == Screen.SettingsSearch ||
        current is Screen.ManageList ||
        (settingsGroup != null &&
            settingsGroup != SettingsGroupId.AUDIO &&
            settingsGroup != SettingsGroupId.ABOUT)
    val onAbout = settingsGroup == SettingsGroupId.ABOUT

    Surface(
        modifier = modifier
            .width(DRAWER_WIDTH)
            .fillMaxHeight(),
        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "MIMI",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 20.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(8.dp))

                DrawerRow(
                    icon = Lucide.Library,
                    title = "Library",
                    count = songCount,
                    selected = onLibrary,
                    onClick = onOpenLibrary,
                )
                DrawerRow(
                    icon = Lucide.Folder,
                    title = "Folders",
                    count = folderCount,
                    selected = onFolders,
                    onClick = onOpenFolders,
                )
                DrawerRow(
                    icon = Lucide.ListMusic,
                    title = "Playing queue",
                    selected = queueOpen,
                    onClick = onOpenQueue,
                )
                DrawerRow(
                    icon = Lucide.SlidersHorizontal,
                    title = "Equalizer",
                    selected = onEqualizer,
                    onClick = onOpenEqualizer,
                )
                if (showInsights) {
                    DrawerRow(
                        icon = Lucide.ChartBar,
                        title = "Listening stats",
                        selected = onStats,
                        onClick = onOpenInsights,
                    )
                }
                DrawerRow(
                    icon = Lucide.RefreshCw,
                    title = "Scan media",
                    selected = false,
                    onClick = onScanMedia,
                )
                DrawerRow(
                    icon = Lucide.Settings,
                    title = "Settings",
                    selected = onSettings,
                    onClick = onOpenSettings,
                )
                DrawerRow(
                    icon = Lucide.Info,
                    title = "About",
                    selected = onAbout,
                    onClick = onOpenAbout,
                )
                DrawerRow(
                    icon = Lucide.Coffee,
                    title = "Donate",
                    selected = false,
                    onClick = { showDonate = true },
                )
            }
        }
    }

    if (showDonate) {
        AlertDialog(
            onDismissRequest = { showDonate = false },
            title = { Text("Donate") },
            text = {
                Text(
                    "MIMI takes no money and has no payment link. Nothing here sends you " +
                        "anywhere: the app plays the files already on your device."
                )
            },
            confirmButton = {
                TextButton(onClick = { showDonate = false }) { Text("Close") }
            },
        )
    }
}

/**
 * One line of the drawer: an icon, a name, and a count when the count is worth knowing.
 *
 * A full-width row rather than a list item with a divider because the panel is a page of
 * destinations. The pill is twenty-eight dp because that is round enough to read as a pill
 * at this height and still leaves the label on a straight line with the others.
 */
@Composable
private fun DrawerRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    selected: Boolean = false,
) {
    val tint = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            .height(56.dp)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
