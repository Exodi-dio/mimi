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
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.AlbumSort
import com.exodidio.mimi.library.ArtistSort
import com.exodidio.mimi.library.DetailSort
import com.exodidio.mimi.library.FolderSummary
import com.exodidio.mimi.library.MinSongLength
import com.exodidio.mimi.library.Settings
import com.exodidio.mimi.library.SongSort
import com.exodidio.mimi.library.ThemeMode
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.duplicateGroups
import com.exodidio.mimi.library.removingHiddenName
import kotlinx.coroutines.delay

/** Which question, if any, is currently open in a dialog. */
private enum class SettingsChoice {
    MIN_LENGTH,
    THEME,
    SONG_SORT,
    ALBUM_SORT,
    ARTIST_SORT,
    DETAIL_SORT,
    EQUALIZER_PRESET,
    EXCLUDED_FOLDERS,
    HIDDEN_NAMES,
    DUPLICATES,
    CREDIT_SEPARATORS,
}

/** How long a row found through search stays tinted before it fades back. */
private const val HIGHLIGHT_MILLIS = 1_600

/**
 * One page of settings, drawn from the catalogue.
 *
 * A page rather than a tabbed screen because the drawer does the splitting: each group is
 * a place of its own with a back arrow, which is the shape a list of five groups implies
 * and the shape a page with a title can grow into. The rows are the catalogue's, so a
 * heading and the rows under it cannot disagree, and the only thing this screen decides is
 * what each row does.
 *
 * A page arrives either plain or with [reveal], the place the drawer or a search result
 * asked for. A named row is scrolled into view and tinted for a moment rather than opened
 * in a copy: what was found is the row that can then be changed. A named section is only
 * scrolled to, because the drawer already said what is on it. Each change is a function
 * from the current settings to the next, so this screen never has to know what the values
 * are — it only has to know what it is changing.
 */
@Composable
fun SettingsGroupScreen(
    group: SettingsGroupId,
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    trackCount: Int,
    tracks: List<Track>,
    scanning: Boolean,
    lastScanDropped: Int?,
    onRescan: () -> Unit,
    onOpenStatistics: () -> Unit,
    folders: List<FolderSummary>,
    onToggleFolder: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    reveal: SettingsReveal? = null,
) {
    var openChoice by remember { mutableStateOf<SettingsChoice?>(null) }
    var highlighted by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Worked out once per visit rather than on every keystroke elsewhere: the duplicate
    // finder is a whole-library pass and this page may be open for a while.
    val duplicates = remember(tracks) { tracks.duplicateGroups() }

    LaunchedEffect(group, reveal) {
        val target = reveal ?: return@LaunchedEffect
        val index = when (target) {
            is SettingsReveal.Row -> settingsItemIndex(group, target.key)
            is SettingsReveal.Section -> settingsSectionIndex(group, target.heading)
        }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            if (target is SettingsReveal.Row) {
                highlighted = target.key
                delay(HIGHLIGHT_MILLIS.toLong())
                highlighted = null
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = group.title,
            subtitle = group.subtitle,
            onBack = onBack,
        )

        SettingsGroupList(
            group = group,
            settings = settings,
            onChange = onChange,
            trackCount = trackCount,
            scanning = scanning,
            lastScanDropped = lastScanDropped,
            onRescan = onRescan,
            onOpenStatistics = onOpenStatistics,
            onChoose = { openChoice = it },
            listState = listState,
            highlighted = highlighted,
        )
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

        SettingsChoice.DETAIL_SORT -> SortSheet(
            title = "Sort lists inside a screen",
            fields = DetailSort.entries.map { it.label },
            selectedIndex = settings.detailSort.ordinal,
            descending = settings.sortDescending,
            onField = { index ->
                onChange { it.copy(detailSort = DetailSort.entries[index]) }
            },
            onDescending = { value -> onChange { it.copy(sortDescending = value) } },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.HIDDEN_NAMES -> HiddenNamesDialog(
            artists = settings.hiddenArtists,
            albums = settings.hiddenAlbums,
            onUnhideArtist = { name ->
                onChange { it.copy(hiddenArtists = it.hiddenArtists.removingHiddenName(name)) }
            },
            onUnhideAlbum = { name ->
                onChange { it.copy(hiddenAlbums = it.hiddenAlbums.removingHiddenName(name)) }
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.DUPLICATES -> DuplicateFinderDialog(
            groups = duplicates,
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
 * One group, drawn from the catalogue so the rows and their headings cannot disagree.
 *
 * Each row is its own lazy item keyed by the catalogue key, which is what lets a search
 * result scroll to it by index. A divider is drawn under a row only when another row of the
 * same section follows, so headings and section boundaries stay unbroken.
 *
 * The horizontal inset comes from the rows and headings themselves, which all use the same
 * twenty dp. That is deliberate: every settings page is inset the same way whether or not
 * it has a row that draws its own full-width control.
 */
@Composable
private fun SettingsGroupList(
    group: SettingsGroupId,
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    trackCount: Int,
    scanning: Boolean,
    lastScanDropped: Int?,
    onRescan: () -> Unit,
    onOpenStatistics: () -> Unit,
    onChoose: (SettingsChoice) -> Unit,
    listState: LazyListState,
    highlighted: String?,
) {
    val items = remember(group) { settingsGroupItems(group) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items.forEachIndexed { index, line ->
            when (line) {
                is SettingsGroupItem.Heading -> item(key = "heading:${group.name}:${line.section}") {
                    SettingsSectionHeading(line.section)
                }

                is SettingsGroupItem.Row -> {
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
                                lastScanDropped = lastScanDropped,
                                onRescan = onRescan,
                                onOpenStatistics = onOpenStatistics,
                                onChange = onChange,
                                onChoose = onChoose,
                            )
                            if (items.getOrNull(index + 1) is SettingsGroupItem.Row) {
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
    lastScanDropped: Int?,
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

        "library.rescan" -> Column {
            SettingsActionRow(
                title = entry.title,
                subtitle = if (scanning) "Looking…" else subtitle,
                enabled = !scanning,
                onClick = onRescan,
            )
            if (!scanning && lastScanDropped != null && lastScanDropped > 0) {
                SettingsNote(
                    "${countLabel(lastScanDropped, "missing file")} " +
                        "dropped from the cache on the last scan."
                )
            }
        }

        "library.played_from" -> SettingsInfoRow(
            title = entry.title,
            subtitle = subtitle,
            value = "This device",
        )

        "library.duplicates" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.DUPLICATES) },
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

        "library.detail_sort" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.detailSort.label,
            onClick = { onChoose(SettingsChoice.DETAIL_SORT) },
        )

        "library.excluded_folders" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.EXCLUDED_FOLDERS) },
        )

        "library.hidden_names" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.HIDDEN_NAMES) },
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

        "about.insights" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.showInsights,
            onCheckedChange = { value -> onChange { it.copy(showInsights = value) } },
            subtitle = subtitle,
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

    "library.hidden_names" -> {
        val artists = settings.hiddenArtists.size
        val albums = settings.hiddenAlbums.size
        when {
            artists == 0 && albums == 0 -> "No artist or album is hidden."
            artists == 1 && albums == 0 -> "One artist is hidden from the library."
            artists == 0 && albums == 1 -> "One album is hidden from the library."
            else -> "$artists artists and $albums albums are hidden from the library."
        }
    }

    "library.credit_separators" -> settings.creditSeparators.joinToString(" · ")
        .ifEmpty { "Names are read exactly as they are tagged." }

    else -> entry.subtitle
}

/**
 * Every setting, found by typing what it is about.
 *
 * A screen of its own rather than a field over each page, because the listener knows the
 * word and not which of five pages it was filed under. A result is a signpost rather than
 * a second copy of the setting: tapping it opens the owning page scrolled to the real row
 * and briefly tinted, so the change is made where the listener will look next time.
 */
@Composable
fun SettingsSearchScreen(
    onPick: (SettingEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { searchSettings(query) }
    val settings = LocalSettings.current

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Search settings", onBack = onBack)
        SettingsSearchField(
            query = query,
            onQueryChange = { query = it },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                query.isBlank() -> Text(
                    text = "Search by the name of a setting, by what it does, or by the " +
                        "word you would use for it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
                )

                results.isEmpty() -> Text(
                    text = "Nothing in settings matches \u201c$query\u201d.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(results, key = { it.key }) { entry ->
                        SettingsResultRow(
                            entry = entry,
                            settings = settings,
                            onClick = { onPick(entry) },
                        )
                    }
                }
            }
        }
    }
}

/** One search result: the row it stands for, and where that row lives. */
@Composable
private fun SettingsResultRow(
    entry: SettingEntry,
    settings: Settings,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            text = "${entry.group.title} · ${entry.section}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Follow the phone"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
