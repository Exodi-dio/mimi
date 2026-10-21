package com.exodidio.mimi.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.AlbumSort
import com.exodidio.mimi.library.ArtistSort
import com.exodidio.mimi.library.FolderSummary
import com.exodidio.mimi.library.MinSongLength
import com.exodidio.mimi.library.Settings
import com.exodidio.mimi.library.SongSort
import com.exodidio.mimi.library.ThemeMode
import kotlinx.coroutines.delay

/** Which question, if any, is currently open in a dialog. */
private enum class SettingsChoice {
    MIN_LENGTH,
    THEME,
    SONG_SORT,
    ALBUM_SORT,
    ARTIST_SORT,
    EQUALIZER_PRESET,
    EXCLUDED_FOLDERS,
    CREDIT_SEPARATORS,
}

/** How long a row found through search stays tinted before it fades back. */
private const val HIGHLIGHT_MILLIS = 1_600

/**
 * Settings, split into pages.
 *
 * Split rather than listed because a settings screen that grows one row at a time becomes
 * unusable long before it becomes complete. Four pages is the shape where every row sits
 * under a heading somebody would look for it under, and it is the shape a skin can restyle
 * without reordering anything.
 *
 * The search field sits above the pages and covers all of them, because the listener knows
 * the word they are looking for and not which page it was filed under. A result switches to
 * its page and scrolls the row into view rather than opening a copy of the setting, so what
 * is found is the same row that can be changed afterwards.
 *
 * Each change is a function from the current settings to the next, so this screen never
 * has to know what the values are — it only has to know what it is changing.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    trackCount: Int,
    scanning: Boolean,
    onRescan: () -> Unit,
    onOpenStatistics: () -> Unit,
    folders: List<FolderSummary>,
    onToggleFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(SettingsPage.LIBRARY) }
    var query by rememberSaveable { mutableStateOf("") }
    var openChoice by remember { mutableStateOf<SettingsChoice?>(null) }
    var scrollTarget by remember { mutableStateOf<String?>(null) }
    var highlighted by remember { mutableStateOf<String?>(null) }

    val libraryState = rememberLazyListState()
    val playbackState = rememberLazyListState()
    val appearanceState = rememberLazyListState()
    val aboutState = rememberLazyListState()
    val listState = when (tab) {
        SettingsPage.LIBRARY -> libraryState
        SettingsPage.PLAYBACK -> playbackState
        SettingsPage.APPEARANCE -> appearanceState
        SettingsPage.ABOUT -> aboutState
    }

    val results = remember(query) { searchSettings(query) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Settings")
        SettingsSearchField(
            query = query,
            onQueryChange = { query = it },
        )
        PillRow(
            options = SettingsPage.entries,
            selected = tab,
            label = { it.label },
            onSelect = {
                tab = it
                query = ""
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (query.isNotBlank()) {
                SettingsSearchResults(
                    results = results,
                    query = query,
                    settings = settings,
                    onPick = { entry ->
                        query = ""
                        tab = entry.page
                        scrollTarget = entry.key
                    },
                )
            } else {
                LaunchedEffect(tab, scrollTarget) {
                    val target = scrollTarget ?: return@LaunchedEffect
                    val index = settingsItemIndex(tab, target)
                    if (index >= 0) {
                        listState.animateScrollToItem(index)
                        highlighted = target
                        delay(HIGHLIGHT_MILLIS.toLong())
                        highlighted = null
                    }
                    scrollTarget = null
                }

                SettingsPageList(
                    page = tab,
                    settings = settings,
                    onChange = onChange,
                    trackCount = trackCount,
                    scanning = scanning,
                    onRescan = onRescan,
                    onOpenStatistics = onOpenStatistics,
                    onChoose = { openChoice = it },
                    listState = listState,
                    highlighted = highlighted,
                )
            }
        }
    }

    when (openChoice) {
        SettingsChoice.MIN_LENGTH -> ChoiceDialog(
            title = "Shortest song to show",
            options = MinSongLength.entries.map { it.label },
            selectedIndex = settings.minSongLength.ordinal,
            onPick = { index ->
                onChange { it.copy(minSongLength = MinSongLength.entries[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.THEME -> ChoiceDialog(
            title = "Theme",
            options = ThemeMode.entries.map { it.label() },
            selectedIndex = settings.themeMode.ordinal,
            onPick = { index ->
                onChange { it.copy(themeMode = ThemeMode.entries[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.SONG_SORT -> ChoiceDialog(
            title = "Sort songs by",
            options = SongSort.entries.map { it.label },
            selectedIndex = settings.songSort.ordinal,
            onPick = { index ->
                onChange { it.copy(songSort = SongSort.entries[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.ALBUM_SORT -> ChoiceDialog(
            title = "Sort albums by",
            options = AlbumSort.entries.map { it.label },
            selectedIndex = settings.albumSort.ordinal,
            onPick = { index ->
                onChange { it.copy(albumSort = AlbumSort.entries[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.ARTIST_SORT -> ChoiceDialog(
            title = "Sort artists by",
            options = ArtistSort.entries.map { it.label },
            selectedIndex = settings.artistSort.ordinal,
            onPick = { index ->
                onChange { it.copy(artistSort = ArtistSort.entries[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.EQUALIZER_PRESET -> EqualizerDialog(
            settings = settings,
            onChange = onChange,
            onDismiss = { openChoice = null },
        )

        SettingsChoice.EXCLUDED_FOLDERS -> ExcludedFoldersDialog(
            folders = folders,
            excluded = settings.excludedFolders,
            onToggle = onToggleFolder,
            onDismiss = { openChoice = null },
        )

        SettingsChoice.CREDIT_SEPARATORS -> CreditSeparatorsDialog(
            separators = settings.creditSeparators,
            onSave = { chosen ->
                onChange { it.copy(creditSeparators = chosen) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        null -> Unit
    }
}

/**
 * One page, drawn from the catalogue so the rows and their headings cannot disagree.
 *
 * Each row is its own lazy item keyed by the catalogue key, which is what lets a search
 * result scroll to it by index. A divider is drawn under a row only when another row of the
 * same section follows, so headings and section boundaries stay unbroken.
 */
@Composable
private fun SettingsPageList(
    page: SettingsPage,
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    trackCount: Int,
    scanning: Boolean,
    onRescan: () -> Unit,
    onOpenStatistics: () -> Unit,
    onChoose: (SettingsChoice) -> Unit,
    listState: LazyListState,
    highlighted: String?,
) {
    val items = remember(page) { settingsPageItems(page) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items.forEachIndexed { index, line ->
            when (line) {
                is SettingsPageItem.Heading -> item(key = "heading:${page.name}:${line.section}") {
                    SettingsSectionHeading(line.section)
                }

                is SettingsPageItem.Row -> {
                    val entry = line.entry
                    item(key = entry.key) {
                        val tint by animateColorAsState(
                            targetValue = if (highlighted == entry.key) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            },
                            animationSpec = tween(durationMillis = 240),
                            label = "settings row highlight",
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(tint),
                        ) {
                            SettingsEntryRow(
                                entry = entry,
                                settings = settings,
                                trackCount = trackCount,
                                scanning = scanning,
                                onRescan = onRescan,
                                onOpenStatistics = onOpenStatistics,
                                onChange = onChange,
                                onChoose = onChoose,
                            )
                            if (items.getOrNull(index + 1) is SettingsPageItem.Row) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Drawing one catalogue row as the control it stands for.
 *
 * The title and subtitle always come from the catalogue entry, so a search result and the
 * row it points at say the same thing. Only values that genuinely change with the library
 * are read from [settings] here.
 */
@Composable
private fun SettingsEntryRow(
    entry: SettingEntry,
    settings: Settings,
    trackCount: Int,
    scanning: Boolean,
    onRescan: () -> Unit,
    onOpenStatistics: () -> Unit,
    onChange: ((Settings) -> Settings) -> Unit,
    onChoose: (SettingsChoice) -> Unit,
) {
    val subtitle = subtitleFor(entry, settings)

    when (entry.key) {
        "library.scan_on_launch" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.scanOnLaunch,
            onCheckedChange = { value -> onChange { it.copy(scanOnLaunch = value) } },
            subtitle = subtitle,
        )

        "library.watch_for_changes" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.watchForChanges,
            onCheckedChange = { value -> onChange { it.copy(watchForChanges = value) } },
            subtitle = subtitle,
        )

        "library.min_song_length" -> Column {
            SettingsChoiceRow(
                title = entry.title,
                subtitle = subtitle,
                value = settings.minSongLength.label,
                onClick = { onChoose(SettingsChoice.MIN_LENGTH) },
            )
            if (settings.minSongLength != MinSongLength.ANY) {
                SettingsNote(
                    "Changing this rebuilds the library, which takes a moment on a large collection."
                )
            }
        }

        "library.song_count" -> SettingsInfoRow(
            title = entry.title,
            subtitle = subtitle,
            value = songCountLabel(trackCount),
        )

        "library.rescan" -> SettingsActionRow(
            title = entry.title,
            subtitle = if (scanning) "Looking…" else subtitle,
            enabled = !scanning,
            onClick = onRescan,
        )

        "library.played_from" -> SettingsInfoRow(
            title = entry.title,
            subtitle = subtitle,
            value = "This device",
        )

        "library.credit_separators" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.CREDIT_SEPARATORS) },
        )

        "library.song_sort" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.songSort.label,
            onClick = { onChoose(SettingsChoice.SONG_SORT) },
        )

        "library.album_sort" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.albumSort.label,
            onClick = { onChoose(SettingsChoice.ALBUM_SORT) },
        )

        "library.artist_sort" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.artistSort.label,
            onClick = { onChoose(SettingsChoice.ARTIST_SORT) },
        )

        "library.sort_descending" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.sortDescending,
            onCheckedChange = { value -> onChange { it.copy(sortDescending = value) } },
            subtitle = subtitle,
        )

        "library.excluded_folders" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.EXCLUDED_FOLDERS) },
        )

        "playback.equalizer_enabled" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.equalizerEnabled,
            onCheckedChange = { value -> onChange { it.copy(equalizerEnabled = value) } },
            subtitle = subtitle,
        )

        // Always offered, even when the equaliser is off: the panel is where it is switched
        // on, and a row that disappears is a row you have to find twice.
        "playback.equalizer_preset" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.equalizerChoices
                .firstOrNull { it.key == settings.equalizerPresetKey }
                ?.let { chosen ->
                    if (chosen.gainsDb == settings.equalizerGains) {
                        chosen.name
                    } else {
                        "${chosen.name} (edited)"
                    }
                }
                ?: "Flat",
            onClick = { onChoose(SettingsChoice.EQUALIZER_PRESET) },
        )

        "playback.skip_silence" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.skipSilence,
            onCheckedChange = { value -> onChange { it.copy(skipSilence = value) } },
            subtitle = subtitle,
        )

        "playback.resume_session" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.resumeSession,
            onCheckedChange = { value -> onChange { it.copy(resumeSession = value) } },
            subtitle = subtitle,
        )

        "playback.pause_on_disconnect" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.pauseOnDisconnect,
            onCheckedChange = { value -> onChange { it.copy(pauseOnDisconnect = value) } },
            subtitle = subtitle,
        )

        "playback.fade_out_sleep_timer" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.fadeOutSleepTimer,
            onCheckedChange = { value -> onChange { it.copy(fadeOutSleepTimer = value) } },
            subtitle = subtitle,
        )

        "playback.show_remaining_time" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.showRemainingTime,
            onCheckedChange = { value -> onChange { it.copy(showRemainingTime = value) } },
            subtitle = subtitle,
        )

        "playback.romanize_lyrics" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.romanizeLyrics,
            onCheckedChange = { value -> onChange { it.copy(romanizeLyrics = value) } },
            subtitle = subtitle,
        )

        "appearance.theme" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.themeMode.label(),
            onClick = { onChoose(SettingsChoice.THEME) },
        )

        "appearance.show_artwork" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.showArtwork,
            onCheckedChange = { value -> onChange { it.copy(showArtwork = value) } },
            subtitle = subtitle,
        )

        "about.version" -> SettingsInfoRow(
            title = entry.title,
            subtitle = subtitle,
            value = appVersion(),
        )

        "about.statistics" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = onOpenStatistics,
        )

        "about.licence" -> SettingsInfoRow(
            title = entry.title,
            subtitle = subtitle,
            value = "GPL-3.0",
        )

        "about.built_with" -> Column {
            SettingsInfoRow(
                title = entry.title,
                subtitle = subtitle,
                value = "Media3, Compose and Room",
            )
            SettingsNote(
                "Plays the files already on this device. No account, no adverts, and " +
                    "nothing sent anywhere."
            )
        }
    }
}

/** The subtitle a row shows, which is the catalogue's unless the library changes the answer. */
private fun subtitleFor(entry: SettingEntry, settings: Settings): String = when (entry.key) {
    "library.excluded_folders" -> when (settings.excludedFolders.size) {
        0 -> "Every folder on the device is read."
        1 -> "One folder is left out of the library."
        else -> "${settings.excludedFolders.size} folders are left out of the library."
    }

    "library.credit_separators" -> settings.creditSeparators.joinToString(" · ")
        .ifEmpty { "Names are read exactly as they are tagged." }

    else -> entry.subtitle
}

/**
 * Search results for a query, each naming the page and section it lives on.
 *
 * A result is a signpost rather than a second copy of the setting: tapping it lands on the
 * real row, already scrolled to and briefly tinted, so the change is made in the place the
 * listener will look next time.
 */
@Composable
private fun SettingsSearchResults(
    results: List<SettingEntry>,
    query: String,
    settings: Settings,
    onPick: (SettingEntry) -> Unit,
) {
    if (results.isEmpty()) {
        Text(
            text = "Nothing in settings matches \u201c$query\u201d.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(results, key = { it.key }) { entry ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(entry) }
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Text(text = entry.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitleFor(entry, settings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Text(
                    text = "${entry.page.label} · ${entry.section}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** The installed version name, or "dev" for a build that did not stamp one. */
@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "dev"
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Follow the phone"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
