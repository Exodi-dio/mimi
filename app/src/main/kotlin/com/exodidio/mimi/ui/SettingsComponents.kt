package com.exodidio.mimi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import com.exodidio.mimi.library.DurationRangeStopsMs
import com.exodidio.mimi.library.DurationRangeUnlimitedStop
import kotlin.math.roundToInt

/**
 * The pieces the settings pages are built from.
 *
 * Every page is the same shape: a back arrow on its own, a large title, a sentence saying
 * what the page is about, then one filled card per section with the section's name written
 * above it rather than inside it. Keeping that shape in these components rather than in
 * each page is what makes a page added later look like the ones that were already there.
 *
 * Every row is tappable across its full width, including the ones that only report
 * something, because a row that looks like a button and is not one reads as a bug. A
 * switch is drawn but not wired on its own: the row handles the tap, so the target is the
 * whole row rather than a control a thumb has to find.
 */

/** The rounding a settings card has at its corners, which is what makes it read as one card. */
private val CardShape = RoundedCornerShape(24.dp)

/** How far a card is inset from the edges of the screen. */
private val CardInset = 16.dp

/**
 * The top of every settings page: a way back, the title, and what the page is about.
 *
 * The back arrow has the row to itself and no title beside it, which is what gives the
 * title below the full width to be large in. Tapping the arrow is the only way out offered
 * here; the system's own back gesture does the same thing.
 */
@Composable
fun SettingsPageHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Lucide.ChevronLeft,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        )
    }
}

/**
 * A section's name, above its card.
 *
 * Outside the card on purpose: a label inside would be one more row and would compete with
 * the settings it labels. Small, uppercase and letterspaced so it reads as a label rather
 * than as a heading to skim past.
 */
@Composable
fun SettingsSectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

/**
 * The background of one row inside a card.
 *
 * A row cannot know whether it is the first or last of its section, so the caller says:
 * the first gets the rounded top, the last the rounded bottom, and the ones between are
 * square so the section reads as a single sheet rather than a stack of tiles.
 */
@Composable
fun SettingsRowBackground(
    first: Boolean,
    last: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(
        topStart = if (first) 24.dp else 0.dp,
        topEnd = if (first) 24.dp else 0.dp,
        bottomStart = if (last) 24.dp else 0.dp,
        bottomEnd = if (last) 24.dp else 0.dp,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CardInset)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape),
    ) {
        content()
    }
}

/** The hairline between two rows of one card, inset from both ends of the card. */
@Composable
fun SettingsRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    enabled: Boolean = true,
) {
    SettingsRowFrame(
        title = title,
        subtitle = subtitle,
        badge = badge,
        enabled = enabled,
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
        modifier = modifier,
    ) {
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun SettingsChoiceRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    enabled: Boolean = true,
) {
    SettingsRowFrame(
        title = title,
        subtitle = subtitle,
        badge = badge,
        enabled = enabled,
        onClick = if (enabled) onClick else null,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun SettingsInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    SettingsRowFrame(title = title, subtitle = subtitle, onClick = null, modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** A settings row that acts rather than reports: it reads as one and does something. */
@Composable
fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    badge: String? = null,
    enabled: Boolean = true,
) {
    SettingsRowFrame(
        title = title,
        subtitle = subtitle,
        badge = badge,
        enabled = enabled,
        onClick = if (enabled) onClick else null,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * One row, with whatever the value side needs drawn beside it.
 *
 * [enabled] is false for a row that is waiting on another: the title and subtitle are
 * dimmed and nothing responds, which says "not now" without pretending the row is gone.
 */
@Composable
private fun SettingsRowFrame(
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    badge: String? = null,
    enabled: Boolean = true,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (enabled) 1f else 0.45f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    SettingsBadge(badge)
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        trailing()
    }
}

/** A tiny label beside a title, used to mark the rows that are still settling. */
@Composable
private fun SettingsBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/**
 * An explanation that is not a setting, as its own tinted card.
 *
 * Its own card because it is not something that can be changed: inside a section card it
 * would put a paragraph between two switches and read as a row that had lost its control.
 * The tint is the warm container rather than the surface the cards use, so the note is
 * visibly not part of the list above or below it.
 */
@Composable
fun SettingsInfoCard(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CardInset, vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                CardShape,
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Lucide.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).padding(top = 2.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A row whose value is one of a few stops on a slider, drawn under the row.
 *
 * Fade and crossfade are both "off, or one of four lengths", so the row and the slider are
 * one thing rather than a row that opens a dialog: the value can be changed while watching
 * the label that describes it.
 */
@Composable
fun SettingsSliderRow(
    title: String,
    subtitle: String,
    value: String,
    labels: List<String>,
    selectedIndex: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsRowFrame(
            title = title,
            subtitle = subtitle,
            badge = badge,
            onClick = null,
            modifier = Modifier,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { onPick(it.roundToInt().coerceIn(0, labels.lastIndex)) },
            valueRange = 0f..labels.lastIndex.toFloat(),
            steps = (labels.size - 2).coerceAtLeast(0),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Finding a setting by typing what it is about. */
@Composable
fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text("Search settings") },
        leadingIcon = { Icon(Lucide.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Lucide.X, contentDescription = "Clear search")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
    )
}

/**
 * Picking one of a few named things.
 *
 * A dialog rather than a menu because the options here are short lists with a description
 * each, and because a dialog can hold a heading that says which question is being asked.
 */
@Composable
fun ChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(index) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = index == selectedIndex, onClick = { onPick(index) })
                        Text(text = option, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (index != options.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

/** A one-paragraph answer, for a row that is honest about not being built yet. */
@Composable
fun MessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

/**
 * The duration range, as a row with two thumbs and the stops written under them.
 *
 * The thumbs snap to the five stops the labels name rather than to milliseconds, because a
 * filter on song length is chosen in minutes. The value on the right reads the bounds as
 * stored, so a bound that came from the old five-choice setting still says what it is.
 */
@Composable
fun SettingsDurationRangeRow(
    title: String,
    subtitle: String,
    value: String,
    startStop: Int,
    endStop: Int,
    onRangeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsRowFrame(
            title = title,
            subtitle = subtitle,
            onClick = null,
            modifier = Modifier,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
        RangeSlider(
            value = startStop.toFloat()..endStop.toFloat(),
            onValueChange = { range ->
                val start = range.start.roundToInt().coerceIn(0, DurationRangeStopsMs.lastIndex)
                // The upper thumb cannot pass the lower one, and cannot sit on "off": a
                // maximum of off is the same as no maximum, which is the other end.
                val end = range.endInclusive.roundToInt()
                    .coerceIn(start + 1, DurationRangeUnlimitedStop)
                onRangeChange(start, end)
            },
            valueRange = 0f..DurationRangeUnlimitedStop.toFloat(),
            steps = DurationRangeUnlimitedStop - 1,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DurationRangeTicks.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The stops the duration slider's labels name, in the order they sit along it. */
private val DurationRangeTicks = listOf("OFF", "1m", "5m", "1h", "\u221e")

/** A short note under a row, for the consequence that does not fit in its subtitle. */
@Composable
fun SettingsNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
    )
}

/** Spacing that keeps the last row of a scrolling page clear of the bar below it. */
@Composable
fun SettingsPageFooter() {
    Spacer(modifier = Modifier.height(24.dp))
}

/**
 * The installed version name, or "dev" for a build that did not stamp one.
 *
 * Read here rather than in each screen that shows it because two facts would otherwise have
 * two answers: the about page and the licence note both name the build.
 */
@Composable
fun appVersion(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "dev"
    }
}
