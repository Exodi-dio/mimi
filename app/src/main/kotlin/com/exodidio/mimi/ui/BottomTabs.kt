package com.exodidio.mimi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Search
import com.exodidio.mimi.library.BottomTabId

/**
 * The bar along the bottom: the four places, then the face.
 *
 * The bar is translucent and its top edge is a hairline rather than a shadow, so the list
 * underneath stays in view behind it and the screen is not cut in two. The selected tab is
 * drawn in the accent, which is the only colour in the bar; the labels are ten sp, small
 * enough to sit under the icon without competing with it.
 *
 * The face is the fifth item because the profile is somewhere a listener goes, not only
 * something they change. It is drawn as the picture itself rather than as an icon, so it
 * is recognisable as theirs at a glance.
 */
@Composable
fun BottomTabs(
    selected: Screen,
    tabs: List<BottomTabId>,
    avatar: ImageBitmap?,
    profileName: String,
    onSelect: (Screen) -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val destination = tabScreen(tab)
                TabItem(
                    icon = tabIcon(tab),
                    label = tab.label,
                    selected = selected == destination,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onProfile),
            ) {
                ProfileAvatar(
                    image = avatar,
                    name = profileName,
                    size = 24.dp,
                )
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/**
 * One tab.
 *
 * The icon is the only thing that changes colour on selection; the label stays the
 * secondary grey so the eye is drawn to one mark rather than to two.
 */
@Composable
private fun TabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Where a bottom tab goes. */
internal fun tabScreen(tab: BottomTabId): Screen = when (tab) {
    BottomTabId.HOME -> Screen.Home
    BottomTabId.LIBRARY -> Screen.Library
    BottomTabId.LOVED -> Screen.Loved
    BottomTabId.SEARCH -> Screen.Search
}

/** What a bottom tab is drawn with. */
internal fun tabIcon(tab: BottomTabId): ImageVector = when (tab) {
    BottomTabId.HOME -> Lucide.House
    BottomTabId.LIBRARY -> Lucide.Music
    BottomTabId.LOVED -> Lucide.Heart
    BottomTabId.SEARCH -> Lucide.Search
}
