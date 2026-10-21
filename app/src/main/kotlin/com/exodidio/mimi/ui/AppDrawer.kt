package com.exodidio.mimi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.ListFilter
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MicVocal
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Search
import com.exodidio.mimi.library.Profile
import java.io.File

/** How tall the strip behind the avatar is, before the avatar hangs below it. */
private val BANNER_HEIGHT = 140.dp

/** How far the avatar hangs below that strip, which is also the room left for it. */
private val AVATAR_OVERHANG = 32.dp

/** The width the drawer is drawn at, which is wide enough for a title and a subtitle. */
private val DRAWER_WIDTH = 300.dp

/**
 * The drawer: who is listening, then every page of settings.
 *
 * The drawer replaced the fifth tab because settings are five pages now, and five pills
 * over a page inside one tab hides exactly the thing a drawer is good at: showing where
 * everything is at once, without leaving the music. The profile sits at the top for the
 * same reason a person's picture sits at the top of a profile — it says whose app this is
 * before it says what can be changed.
 *
 * Each group is a heading with its sections under it, taken from the catalogue in the order
 * the page draws them. That makes the drawer a table of contents rather than a menu: a row
 * says not just "library" but "the part of the library about leaving songs out", and lands
 * on that heading. Five groups stay five things because each one is introduced by its own
 * heading and set apart by a rule.
 */
@Composable
fun AppDrawer(
    profile: Profile,
    avatarFile: File?,
    bannerFile: File?,
    pictureRevision: Int,
    current: Screen,
    showInsights: Boolean,
    onOpenProfile: () -> Unit,
    onEditProfile: () -> Unit,
    onSearchSettings: () -> Unit,
    onOpenSection: (SettingsGroupId, String) -> Unit,
    onOpenInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatar = rememberProfileImage(avatarFile, DRAWER_AVATAR_PX, pictureRevision)
    val banner = rememberProfileImage(bannerFile, DRAWER_BANNER_PX, pictureRevision)
    val openGroup = (current as? Screen.SettingsGroup)?.group

    // The section the open page is showing, which is what a drawer row is lit for. A search
    // result does not name a section, so it is mapped back through the catalogue.
    val openSection = when (val reveal = (current as? Screen.SettingsGroup)?.reveal) {
        is SettingsReveal.Section -> reveal.heading
        is SettingsReveal.Row -> settingsCatalog.firstOrNull { it.key == reveal.key }?.section
        null -> null
    }

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
                    profile = profile,
                    avatar = avatar,
                    banner = banner,
                    onOpen = onOpenProfile,
                    onEdit = onEditProfile,
                )

                Spacer(modifier = Modifier.height(4.dp))

                DrawerRow(
                    icon = Lucide.Search,
                    title = "Search settings",
                    selected = current == Screen.SettingsSearch,
                    onClick = onSearchSettings,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                SettingsGroupId.entries.forEachIndexed { index, group ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    SettingsSectionHeading(text = group.title)
                    settingsSections(group).forEach { section ->
                        DrawerRow(
                            icon = sectionIcon(section) ?: group.icon,
                            title = section,
                            selected = openGroup == group && openSection == section,
                            onClick = { onOpenSection(group, section) },
                        )
                        // Insights is a screen rather than a page of settings, so it sits
                        // beside the Statistics heading it belongs with instead of being
                        // one more section that opens a page. The switch that hides it is
                        // the row under that heading on the page.
                        if (group == SettingsGroupId.TOOLS && section == "Statistics" && showInsights) {
                            DrawerRow(
                                icon = Lucide.Clock,
                                title = "Insights",
                                selected = current == Screen.Insights,
                                onClick = onOpenInsights,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Version ${appVersion()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}

/**
 * The icon a drawer row leads with.
 *
 * A section is a smaller idea than a group, so the group's own icon would say the same
 * thing four times in a row. A handful of sections get an icon of their own and the rest
 * fall back to their group's, which is also what keeps a section added later from having
 * to be taught an icon before it can be drawn.
 */
private fun sectionIcon(section: String): ImageVector? = when (section) {
    "Finding music" -> Lucide.Search
    "This device" -> Lucide.Folder
    "Names and grouping" -> Lucide.ListFilter
    "Songs to leave out" -> Lucide.EyeOff
    "Sound" -> Lucide.Gauge
    "Behaviour" -> Lucide.Play
    "Words" -> Lucide.MicVocal
    "Lists and layout" -> Lucide.ListFilter
    "Equaliser" -> Lucide.Gauge
    "Statistics" -> Lucide.Clock
    "Duplicate songs" -> Lucide.Copy
    "Hidden names" -> Lucide.EyeOff
    else -> null
}

/**
 * The banner, the face and the name.
 *
 * The whole block opens the profile page except the edit button, which does the same thing
 * by name so that the one action the header offers is not hidden behind knowing where to
 * tap. A banner that was never chosen leaves a muted strip rather than a blank one, so the
 * header keeps its shape and the avatar has an edge to sit on.
 */
@Composable
private fun DrawerHeader(
    profile: Profile,
    avatar: ImageBitmap?,
    banner: ImageBitmap?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BANNER_HEIGHT + AVATAR_OVERHANG),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BANNER_HEIGHT)
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
                    // The banner runs into the sheet rather than ending on a hard line,
                    // which is what lets the name below it read against either picture.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.45f to Color.Transparent,
                                        1f to MaterialTheme.colorScheme.surface,
                                    )
                                )
                            ),
                    )
                }

                ProfileAvatar(
                    image = avatar,
                    name = profile.name,
                    size = 64.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
        ) {
            Text(
                text = profile.name.ifBlank { "Your name" },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = onEdit,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("Edit profile")
            }
        }
    }
}

/**
 * One line of the drawer: an icon, a name, and where it goes.
 *
 * A full-width row rather than a list item with a divider because the drawer is a page of
 * destinations, and the chevron says "this opens" without needing a second colour or a
 * description. The selected row is filled rather than merely tinted, so the page that is
 * open is findable even while the drawer is being scanned from the bottom up.
 */
@Composable
private fun DrawerRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val tint = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Decoded sizes: the avatar is drawn at 64dp, the banner across 300dp. */
private const val DRAWER_AVATAR_PX = 192
private const val DRAWER_BANNER_PX = 900
