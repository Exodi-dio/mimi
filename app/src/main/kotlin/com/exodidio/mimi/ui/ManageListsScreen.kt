package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Lucide
import com.exodidio.mimi.library.BottomTabId
import com.exodidio.mimi.library.DefaultBottomTabOrder
import com.exodidio.mimi.library.DefaultHomeRowOrder
import com.exodidio.mimi.library.HomeRowId
import com.exodidio.mimi.library.Settings
import com.exodidio.mimi.library.canMoveDown
import com.exodidio.mimi.library.canMoveUp
import com.exodidio.mimi.library.movingDown
import com.exodidio.mimi.library.movingUp
import com.exodidio.mimi.library.normalisedOrder

/** Which list a manage screen is rearranging. */
enum class ManageListKind(
    val title: String,
    val subtitle: String,
) {
    BOTTOM_TABS(
        title = "Manage bottom tabs",
        subtitle = "Reorder and set visibility of bottom tabs.",
    ),
    HOME_ROWS(
        title = "Manage home rows",
        subtitle = "Reorder or hide sections shown on the home tab.",
    ),
}

/**
 * Rearranging one of the app's own lists.
 *
 * Up and down buttons rather than drag and drop: a drag needs a long press, a lift and a
 * target that stays still, and none of that is available to somebody using a screen
 * reader. A button is one tap and says exactly where it moves something.
 *
 * The order on screen is the normalised order, not the stored one, so a destination added
 * in a later version appears in its default place instead of being invisible until the row
 * is moved. Every write goes through the same normalisation, which keeps what is shown and
 * what is stored from drifting apart.
 */
@Composable
fun ManageListsScreen(
    kind: ManageListKind,
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val known = when (kind) {
        ManageListKind.BOTTOM_TABS -> DefaultBottomTabOrder
        ManageListKind.HOME_ROWS -> DefaultHomeRowOrder
    }
    val storedOrder = when (kind) {
        ManageListKind.BOTTOM_TABS -> settings.bottomTabOrder
        ManageListKind.HOME_ROWS -> settings.homeRowOrder
    }
    val hidden = when (kind) {
        ManageListKind.BOTTOM_TABS -> settings.hiddenBottomTabs
        ManageListKind.HOME_ROWS -> settings.hiddenHomeRows
    }
    val order = normalisedOrder(storedOrder, known)
    val visibleCount = order.count { it !in hidden }

    fun writeOrder(next: List<String>) {
        onChange { current ->
            when (kind) {
                ManageListKind.BOTTOM_TABS -> current.copy(bottomTabOrder = next)
                ManageListKind.HOME_ROWS -> current.copy(homeRowOrder = next)
            }
        }
    }

    fun writeHidden(key: String, show: Boolean) {
        onChange { current ->
            val currentHidden = when (kind) {
                ManageListKind.BOTTOM_TABS -> current.hiddenBottomTabs
                ManageListKind.HOME_ROWS -> current.hiddenHomeRows
            }
            val next = if (show) {
                currentHidden.filterNot { it == key }
            } else {
                (currentHidden + key).distinct()
            }
            when (kind) {
                ManageListKind.BOTTOM_TABS -> current.copy(hiddenBottomTabs = next)
                ManageListKind.HOME_ROWS -> current.copy(hiddenHomeRows = next)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SettingsPageHeader(
            title = kind.title,
            subtitle = kind.subtitle,
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "heading") {
                SettingsSectionHeading("ORDER")
            }
            item(key = "card") {
                SettingsRowBackground(first = true, last = true) {
                    order.forEachIndexed { index, key ->
                        ManagedListRow(
                            title = labelFor(kind, key),
                            visible = key !in hidden,
                            canMoveUp = order.canMoveUp(key),
                            canMoveDown = order.canMoveDown(key),
                            // The last visible tab cannot be hidden: a navigation bar with
                            // nothing in it is a dead end rather than a preference.
                            canToggle = key !in hidden || visibleCount > 1,
                            onMoveUp = { writeOrder(order.movingUp(key)) },
                            onMoveDown = { writeOrder(order.movingDown(key)) },
                            onVisibleChange = { show -> writeHidden(key, show) },
                        )
                        if (index != order.lastIndex) SettingsRowDivider()
                    }
                }
            }

            if (kind == ManageListKind.BOTTOM_TABS) {
                item(key = "note") {
                    SettingsNote("At least one tab stays visible, whatever else is hidden.")
                }
            }
        }
    }
}

/** The name a stored key is shown under, falling back to the key for anything unknown. */
private fun labelFor(kind: ManageListKind, key: String): String = when (kind) {
    ManageListKind.BOTTOM_TABS ->
        BottomTabId.entries.firstOrNull { it.name == key }?.label ?: key

    ManageListKind.HOME_ROWS ->
        HomeRowId.entries.firstOrNull { it.name == key }?.title ?: key
}

/** One entry: its name, the two ways to move it, and whether it is shown. */
@Composable
private fun ManagedListRow(
    title: String,
    visible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canToggle: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onVisibleChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            ),
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                imageVector = Lucide.ArrowUp,
                contentDescription = "Move $title up",
                tint = if (canMoveUp) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(
                imageVector = Lucide.ArrowDown,
                contentDescription = "Move $title down",
                tint = if (canMoveDown) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
            checked = visible,
            onCheckedChange = onVisibleChange,
            enabled = canToggle,
        )
    }
}
