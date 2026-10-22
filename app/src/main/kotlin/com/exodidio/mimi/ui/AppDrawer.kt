package com.exodidio.mimi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
 * The drawer: the places in the app, under the face that owns them.
 *
 * The banner and the picture at the top are the profile, and tapping them opens the page
 * where the name and the pictures are changed. That is why the profile is no longer a row
 * at the bottom of the panel: it is the header rather than a destination beside the others.
 *
 * Below it the rows are grouped by what they are for — the library, the way the sound is
 * shaped, and the app itself — with each group spaced away from the next so nine rows read
 * as three answers to three questions. The selected row is the only one filled, and the fill
 * is a pill rather than a rectangle, so the open destination can be found without the panel
 * looking like a list of buttons.
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
    banner: ImageBitmap?,
    avatar: ImageBitmap?,
    profileName: String,
    onOpenProfile: () -> Unit,
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
                DrawerHeader(
                    banner = banner,
                    avatar = avatar,
                    name = profileName,
                    onOpenProfile = onOpenProfile,
                )

                Spacer(modifier = Modifier.height(12.dp))

                DrawerGroup {
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
                }

                DrawerGroup {
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
                }

                DrawerGroup {
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

/** The height of the banner at the top of the drawer. */
private val DRAWER_BANNER_HEIGHT = 140.dp

/** The edge of the profile picture, half of which hangs below that banner. */
private val DRAWER_AVATAR_SIZE = 76.dp

/**
 * The banner, the face and the name at the top of the drawer.
 *
 * The banner is a picture rather than a colour so the panel opens on something the listener
 * chose, and the gradient at its foot carries it into the panel instead of ending on a hard
 * edge. The picture is laid over that edge by half its height, which is what makes the two
 * read as one header. The whole header is the way into the profile, because that is what it
 * is a picture of.
 */
@Composable
private fun DrawerHeader(
    banner: ImageBitmap?,
    avatar: ImageBitmap?,
    name: String,
    onOpenProfile: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenProfile),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DRAWER_BANNER_HEIGHT)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (banner != null) {
                Image(
                    bitmap = banner,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.surface,
                        )
                    ),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -(DRAWER_AVATAR_SIZE / 2))
                .padding(horizontal = 16.dp),
        ) {
            ProfileAvatar(image = avatar, name = name, size = DRAWER_AVATAR_SIZE)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { "Your profile" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "View profile",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One group of drawer rows, spaced away from the group above and below it. */
@Composable
private fun DrawerGroup(content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        content()
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
