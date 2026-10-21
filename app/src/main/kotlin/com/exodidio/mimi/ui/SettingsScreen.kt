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
import com.exodidio.mimi.library.RecentRange
import com.exodidio.mimi.library.Settings
import com.exodidio.mimi.library.SongSort
import com.exodidio.mimi.library.ThemeMode
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.durationMaxMsFor
import com.exodidio.mimi.library.durationMaxStop
import com.exodidio.mimi.library.durationMinMsFor
import com.exodidio.mimi.library.durationMinStop
import com.exodidio.mimi.library.durationRangeLabel
import com.exodidio.mimi.library.duplicateGroups
import com.exodidio.mimi.library.removingHiddenName
import kotlinx.coroutines.delay

/** Which question, if any, is currently open in a dialog. */
private enum class SettingsChoice {
    THEME,
    SONG_SORT,
    ALBUM_SORT,
    ARTIST_SORT,
    DETAIL_SORT,
    EQUALIZER,
    EXCLUDED_FOLDERS,
    HIDDEN_NAMES,
    DUPLICATES,
    ARTIST_SEPARATORS,
    GENRE_SEPARATORS,
    ENGINE,
    EQ_BANDS,
    REPLAYGAIN,
    REPLAYGAIN_PREAMP,
    METADATA,
    NOW_PLAYING_THEME,
    EXPORT_PATH,
    LAST_ADDED_RANGE,
    RECENTLY_PLAYED_RANGE,
    BACKUP_CREATE,
    BACKUP_RESTORE,
    BACKUP_FREQUENCY,
    BACKUP_FOLDER,
    WHATS_NEW,
}

/** How long a row found through search stays tinted before it fades back. */
private const val HIGHLIGHT_MILLIS = 1_600

/** The fade lengths offered, in milliseconds, in the order the slider lays them out. */
private val FadeOptionsMs = listOf(0, 250, 500, 750, 1_000)

/** The crossfade lengths offered, in milliseconds. */
private val CrossfadeOptionsMs = listOf(0, 3_000, 6_000, 9_000, 12_000)

/** The band counts the equaliser row offers. The device decides what it can really give. */
private val EqBandOptions = listOf(5, 10, 15, 20)

/** How the metadata download row describes its choices. */
private val MetadataOptions = listOf("Always", "Wi-Fi only", "Never")

/** The ways ReplayGain can be applied, from the values the file may carry. */
private val ReplayGainOptions =
    listOf(Settings.REPLAYGAIN_NONE, Settings.REPLAYGAIN_TRACK, Settings.REPLAYGAIN_ALBUM)

/**
 * The preamps the ReplayGain row offers, in decibels.
 *
 * The same steps the equaliser uses, so the two are read the same way.
 */
private val ReplayGainPreampOptionsDb = listOf(-12f, -9f, -6f, -3f, 0f, 3f, 6f, 9f, 12f)

/** The schedules the automatic backup row offers. */
private val BackupFrequencyOptions = listOf("Daily", "Weekly", "Monthly")

/**
 * One page of settings, drawn from the catalogue.
 *
 * A page rather than a tabbed screen because the drawer lists destinations and Settings is
 * one of them: [SettingsHomeScreen] is the index, and each group here is a place of its own
 * with a back arrow. The rows are the catalogue's, so a heading and the rows under it
 * cannot disagree, and the only thing this screen decides is what each row does.
 *
 * A page arrives either plain or with [reveal], the place a search result asked for. A
 * named row is scrolled into view and tinted for a moment rather than opened in a copy:
 * what was found is the row that can then be changed. Each change is a function from the
 * current settings to the next, so this screen never has to know what the values are — it
 * only has to know what it is changing.
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
    onOpenManageTabs: () -> Unit,
    onOpenManageRows: () -> Unit,
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
        SettingsPageHeader(
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
            onOpenManageTabs = onOpenManageTabs,
            onOpenManageRows = onOpenManageRows,
            onChoose = { openChoice = it },
            listState = listState,
            highlighted = highlighted,
        )
    }

    when (openChoice) {
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

        SettingsChoice.EQUALIZER -> EqualizerDialog(
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

        SettingsChoice.ARTIST_SEPARATORS -> CreditSeparatorsDialog(
            separators = settings.artistSeparators,
            title = "Split artist names at",
            description = "Marks and words that separate one artist from the next.",
            preview = "Earth, Wind & Fire feat. Someone",
            onSave = { chosen ->
                onChange { it.copy(artistSeparators = chosen) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.GENRE_SEPARATORS -> CreditSeparatorsDialog(
            separators = settings.genreSeparators,
            title = "Split genre names at",
            description = "Marks and words that separate one genre from the next.",
            preview = "Rock, Pop & Soul feat. Jazz",
            onSave = { chosen ->
                onChange { it.copy(genreSeparators = chosen) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.ENGINE -> MessageDialog(
            title = "Playback engine",
            message = "This build has one engine: the system decoder behind " +
                "\u201c${Settings.SYSTEM_ENGINE}\u201d. The enhanced engine named in the " +
                "row's description is not part of the app yet, so there is nothing to " +
                "switch to.",
            onDismiss = { openChoice = null },
        )

        SettingsChoice.EQ_BANDS -> ChoiceDialog(
            title = "EQ Band Count",
            options = EqBandOptions.map { "$it Bands" },
            selectedIndex = EqBandOptions.indexOf(settings.eqBandCount).coerceAtLeast(0),
            onPick = { index ->
                onChange { it.copy(eqBandCount = EqBandOptions[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.REPLAYGAIN -> ChoiceDialog(
            title = "ReplayGain source mode",
            options = ReplayGainOptions,
            selectedIndex = ReplayGainOptions.indexOf(settings.replayGainMode).coerceAtLeast(0),
            onPick = { index ->
                onChange { it.copy(replayGainMode = ReplayGainOptions[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.REPLAYGAIN_PREAMP -> ChoiceDialog(
            title = "ReplayGain preamp",
            options = ReplayGainPreampOptionsDb.map { preampLabel(it) },
            selectedIndex = nearestIndex(
                ReplayGainPreampOptionsDb.map { it.toInt() },
                settings.replayGainPreampDb.toInt(),
            ),
            onPick = { index ->
                onChange { it.copy(replayGainPreampDb = ReplayGainPreampOptionsDb[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.METADATA -> ChoiceDialog(
            title = "Auto download metadata",
            options = MetadataOptions,
            selectedIndex = MetadataOptions.indexOf(settings.autoDownloadMetadata).coerceAtLeast(0),
            onPick = { index ->
                onChange { it.copy(autoDownloadMetadata = MetadataOptions[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.LAST_ADDED_RANGE -> ChoiceDialog(
            title = "Last added playlist",
            options = RecentRange.entries.map { it.label },
            selectedIndex = settings.lastAddedRange.ordinal,
            onPick = { index ->
                onChange { it.copy(lastAddedRange = RecentRange.entries[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.RECENTLY_PLAYED_RANGE -> ChoiceDialog(
            title = "Recently played",
            options = RecentRange.entries.map { it.label },
            selectedIndex = settings.recentlyPlayedRange.ordinal,
            onPick = { index ->
                onChange { it.copy(recentlyPlayedRange = RecentRange.entries[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.BACKUP_FREQUENCY -> ChoiceDialog(
            title = "Backup frequency",
            options = BackupFrequencyOptions,
            selectedIndex = BackupFrequencyOptions.indexOf(settings.backupFrequency).coerceAtLeast(0),
            onPick = { index ->
                onChange { it.copy(backupFrequency = BackupFrequencyOptions[index]) }
                openChoice = null
            },
            onDismiss = { openChoice = null },
        )

        SettingsChoice.NOW_PLAYING_THEME -> MessageDialog(
            title = "Now Playing Theme",
            message = "Only the default player is drawn in this build. Other themes are " +
                "not built yet, so this row remembers the choice and changes nothing.",
            onDismiss = { openChoice = null },
        )

        SettingsChoice.EXPORT_PATH -> MessageDialog(
            title = "Playlist export path",
            message = "Choosing an export folder is not built yet. Playlists stay inside " +
                "the app, and nothing is written to a file.",
            onDismiss = { openChoice = null },
        )

        SettingsChoice.BACKUP_CREATE -> MessageDialog(
            title = "Create backup",
            message = "Backup files are not built yet. Your library stays on this device, " +
                "and nothing has been written.",
            onDismiss = { openChoice = null },
        )

        SettingsChoice.BACKUP_RESTORE -> MessageDialog(
            title = "Restore from backup",
            message = "Restoring is not built yet, so there is nothing to read. No data " +
                "has been changed.",
            onDismiss = { openChoice = null },
        )

        SettingsChoice.BACKUP_FOLDER -> MessageDialog(
            title = "Backup folder",
            message = "Choosing a backup folder is not built yet. The choice is remembered, " +
                "but no automatic backup is written.",
            onDismiss = { openChoice = null },
        )

        // The sheet rather than a dialog: a changelog is longer than a question, and it
        // is read rather than answered.
        SettingsChoice.WHATS_NEW -> ChangelogSheet(
            version = appVersion(),
            onDismiss = { openChoice = null },
        )

        null -> Unit
    }
}

/**
 * One group, drawn from the catalogue so the rows and their headings cannot disagree.
 *
 * Each row is its own lazy item keyed by the catalogue key, which is what lets a search
 * result scroll to it by index. A section's rows share one filled card: the first row
 * rounds the top, the last rounds the bottom, and a hairline sits between neighbours.
 *
 * A note row is drawn as its own tinted card instead, because it explains rather than
 * changes and should not read as a switch that lost its control.
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
    onOpenManageTabs: () -> Unit,
    onOpenManageRows: () -> Unit,
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
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = tween(durationMillis = 240),
                            label = "settings row highlight",
                        )

                        if (entry.note) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(tint),
                            ) {
                                SettingsInfoCard(text = entry.subtitle)
                            }
                            return@item
                        }

                        val previous = items.getOrNull(index - 1) as? SettingsGroupItem.Row
                        val next = items.getOrNull(index + 1) as? SettingsGroupItem.Row
                        // A note is its own card, so it also ends whatever card came before
                        // it: a section that opens with a note would otherwise draw its first
                        // row with a square top and read as a card that had been cut off.
                        val first = previous == null || previous.entry.note ||
                            previous.entry.section != entry.section
                        val last = next == null || next.entry.note ||
                            next.entry.section != entry.section

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(tint),
                        ) {
                            SettingsRowBackground(first = first, last = last) {
                                SettingsEntryRow(
                                    entry = entry,
                                    settings = settings,
                                    trackCount = trackCount,
                                    scanning = scanning,
                                    lastScanDropped = lastScanDropped,
                                    onRescan = onRescan,
                                    onOpenStatistics = onOpenStatistics,
                                    onOpenManageTabs = onOpenManageTabs,
                                    onOpenManageRows = onOpenManageRows,
                                    onChange = onChange,
                                    onChoose = onChoose,
                                )
                                if (!last) SettingsRowDivider()
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
 * row it points at say the same thing. Only values that genuinely change with the choice
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
    onOpenManageTabs: () -> Unit,
    onOpenManageRows: () -> Unit,
    onChange: ((Settings) -> Settings) -> Unit,
    onChoose: (SettingsChoice) -> Unit,
) {
    val subtitle = entry.subtitle

    when (entry.key) {
        "library.multiple_artists" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.multipleArtists,
            onCheckedChange = { value -> onChange { it.copy(multipleArtists = value) } },
            subtitle = subtitle,
        )

        "library.artist_separators" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.ARTIST_SEPARATORS) },
        )

        // Greyed out because a credit with several names is ambiguous until the split
        // switch says which names there are; turning the switch on makes this row live.
        "library.always_first_artist" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.alwaysFirstArtist,
            onCheckedChange = { value -> onChange { it.copy(alwaysFirstArtist = value) } },
            subtitle = subtitle,
            enabled = settings.multipleArtists,
        )

        "library.multiple_genres" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.multipleGenres,
            onCheckedChange = { value -> onChange { it.copy(multipleGenres = value) } },
            subtitle = subtitle,
        )

        "library.genre_separators" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.GENRE_SEPARATORS) },
        )

        "library.excluded_folders" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.EXCLUDED_FOLDERS) },
        )

        "library.duration_range" -> SettingsDurationRangeRow(
            title = entry.title,
            subtitle = subtitle,
            value = durationRangeLabel(settings.minDurationMs, settings.maxDurationMs),
            startStop = durationMinStop(settings.minDurationMs),
            endStop = durationMaxStop(settings.maxDurationMs),
            onRangeChange = { startStop, endStop ->
                // A bound that was stored off the stops is kept as it is while the other
                // thumb moves: a library filtered at five seconds should not quietly become
                // "everything" because somebody only touched the upper end.
                val minMs = if (startStop == durationMinStop(settings.minDurationMs)) {
                    settings.minDurationMs
                } else {
                    durationMinMsFor(startStop)
                }
                val maxMs = if (endStop == durationMaxStop(settings.maxDurationMs)) {
                    settings.maxDurationMs
                } else {
                    durationMaxMsFor(endStop)
                }
                onChange { it.copy(minDurationMs = minMs, maxDurationMs = maxMs) }
            },
        )

        "library.duplicates" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.DUPLICATES) },
        )

        "library.hidden_names" -> SettingsActionRow(
            title = entry.title,
            subtitle = hiddenNamesSubtitle(settings),
            onClick = { onChoose(SettingsChoice.HIDDEN_NAMES) },
        )

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

        "library.song_count" -> SettingsInfoRow(
            title = entry.title,
            subtitle = subtitle,
            value = songCountLabel(trackCount),
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

        "playback.fade_on_pause" -> SettingsSliderRow(
            title = entry.title,
            subtitle = subtitle,
            badge = entry.badge,
            value = fadeLabel(settings.fadeOnPlayPauseMs),
            labels = listOf("OFF", "250", "500", "750", "1000"),
            selectedIndex = nearestIndex(FadeOptionsMs, settings.fadeOnPlayPauseMs),
            onPick = { index -> onChange { it.copy(fadeOnPlayPauseMs = FadeOptionsMs[index]) } },
        )

        "playback.crossfade" -> SettingsSliderRow(
            title = entry.title,
            subtitle = subtitle,
            badge = entry.badge,
            value = crossfadeLabel(settings.crossfadeMs),
            labels = listOf("OFF", "3s", "6s", "9s", "12s"),
            selectedIndex = nearestIndex(CrossfadeOptionsMs, settings.crossfadeMs),
            onPick = { index -> onChange { it.copy(crossfadeMs = CrossfadeOptionsMs[index]) } },
        )

        "playback.gapless" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.gaplessPlayback,
            onCheckedChange = { value -> onChange { it.copy(gaplessPlayback = value) } },
            subtitle = subtitle,
        )

        "playback.keep_shuffle" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.keepShuffleMode,
            onCheckedChange = { value -> onChange { it.copy(keepShuffleMode = value) } },
            subtitle = subtitle,
        )

        "playback.equalizer" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            value = if (settings.equalizerEnabled) settings.equalizerProfile.name else "Off",
            onClick = { onChoose(SettingsChoice.EQUALIZER) },
        )

        "playback.skip_silence" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.skipSilence,
            onCheckedChange = { value -> onChange { it.copy(skipSilence = value) } },
            subtitle = subtitle,
        )

        "playback.engine" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.playbackEngine,
            onClick = { onChoose(SettingsChoice.ENGINE) },
        )

        "playback.eq_band_count" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = "${settings.eqBandCount} Bands",
            onClick = { onChoose(SettingsChoice.EQ_BANDS) },
        )

        "playback.replaygain_mode" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.replayGainMode,
            onClick = { onChoose(SettingsChoice.REPLAYGAIN) },
        )

        "playback.replaygain_preamp" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            value = preampLabel(settings.replayGainPreampDb),
            onClick = { onChoose(SettingsChoice.REPLAYGAIN_PREAMP) },
        )

        "playback.duck_on_focus_loss" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.lowerVolumeOnFocusLoss,
            onCheckedChange = { value -> onChange { it.copy(lowerVolumeOnFocusLoss = value) } },
            subtitle = subtitle,
        )

        "playback.auto_play" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.autoPlay,
            onCheckedChange = { value -> onChange { it.copy(autoPlay = value) } },
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

        "playback.romanize_lyrics" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.romanizeLyrics,
            onCheckedChange = { value -> onChange { it.copy(romanizeLyrics = value) } },
            subtitle = subtitle,
        )

        "images.cache_images" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.cacheImages,
            onCheckedChange = { value -> onChange { it.copy(cacheImages = value) } },
            subtitle = subtitle,
        )

        "images.mediastore_art" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.mediaStoreArt,
            onCheckedChange = { value -> onChange { it.copy(mediaStoreArt = value) } },
            subtitle = subtitle,
        )

        "images.auto_download_metadata" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.autoDownloadMetadata,
            onClick = { onChoose(SettingsChoice.METADATA) },
        )

        "images.lockscreen_art" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.lockscreenArt,
            onCheckedChange = { value -> onChange { it.copy(lockscreenArt = value) } },
            subtitle = subtitle,
        )

        "images.blur_album_cover" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.blurAlbumCover,
            onCheckedChange = { value -> onChange { it.copy(blurAlbumCover = value) } },
            subtitle = subtitle,
        )

        "customization.theme" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.themeMode.label(),
            onClick = { onChoose(SettingsChoice.THEME) },
        )

        "customization.show_artwork" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.showArtwork,
            onCheckedChange = { value -> onChange { it.copy(showArtwork = value) } },
            subtitle = subtitle,
        )

        "customization.bottom_tabs" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = onOpenManageTabs,
        )

        "customization.home_rows" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = onOpenManageRows,
        )

        "customization.see_all" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.showSeeAll,
            onCheckedChange = { value -> onChange { it.copy(showSeeAll = value) } },
            subtitle = subtitle,
        )

        "now_playing.theme" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.nowPlayingTheme,
            onClick = { onChoose(SettingsChoice.NOW_PLAYING_THEME) },
        )

        "now_playing.song_info" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.showSongInfo,
            onCheckedChange = { value -> onChange { it.copy(showSongInfo = value) } },
            subtitle = subtitle,
        )

        "now_playing.volume_slider" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.showVolumeSlider,
            onCheckedChange = { value -> onChange { it.copy(showVolumeSlider = value) } },
            subtitle = subtitle,
        )

        "now_playing.extra_controls" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.extraControls,
            onCheckedChange = { value -> onChange { it.copy(extraControls = value) } },
            subtitle = subtitle,
        )

        "now_playing.remaining_time" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.showRemainingTime,
            onCheckedChange = { value -> onChange { it.copy(showRemainingTime = value) } },
            subtitle = subtitle,
        )

        "playlists.export_path" -> SettingsActionRow(
            title = entry.title,
            subtitle = settings.playlistExportPath.ifBlank { subtitle },
            onClick = { onChoose(SettingsChoice.EXPORT_PATH) },
        )

        "playlists.last_added_range" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.lastAddedRange.label,
            onClick = { onChoose(SettingsChoice.LAST_ADDED_RANGE) },
        )

        "playlists.recently_played_range" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.recentlyPlayedRange.label,
            onClick = { onChoose(SettingsChoice.RECENTLY_PLAYED_RANGE) },
        )

        "backup.create" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.BACKUP_CREATE) },
        )

        "backup.restore" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            onClick = { onChoose(SettingsChoice.BACKUP_RESTORE) },
        )

        "backup.automatic" -> SettingsToggleRow(
            title = entry.title,
            checked = settings.automaticBackups,
            onCheckedChange = { value -> onChange { it.copy(automaticBackups = value) } },
            subtitle = subtitle,
        )

        "backup.frequency" -> SettingsChoiceRow(
            title = entry.title,
            subtitle = subtitle,
            value = settings.backupFrequency,
            enabled = settings.automaticBackups,
            onClick = { onChoose(SettingsChoice.BACKUP_FREQUENCY) },
        )

        "backup.folder" -> SettingsActionRow(
            title = entry.title,
            subtitle = settings.backupFolder.ifBlank { subtitle },
            enabled = settings.automaticBackups,
            onClick = { onChoose(SettingsChoice.BACKUP_FOLDER) },
        )

        "about.version" -> SettingsInfoRow(
            title = entry.title,
            subtitle = subtitle,
            value = appVersion(),
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

        "about.whats_new" -> SettingsActionRow(
            title = entry.title,
            subtitle = subtitle,
            value = appVersion(),
            onClick = { onChoose(SettingsChoice.WHATS_NEW) },
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
    }
}

/** The hidden-name row counts what is hidden, which the catalogue alone cannot know. */
private fun hiddenNamesSubtitle(settings: Settings): String {
    val artists = settings.hiddenArtists.size
    val albums = settings.hiddenAlbums.size
    return when {
        artists == 0 && albums == 0 -> "No artist or album is hidden."
        artists == 1 && albums == 0 -> "One artist is hidden from the library."
        artists == 0 && albums == 1 -> "One album is hidden from the library."
        else -> "$artists artists and $albums albums are hidden from the library."
    }
}

/** "off", "250 ms" or "1 s", whichever the stored fade really is. */
private fun fadeLabel(millis: Int): String = when {
    millis <= 0 -> "off"
    millis % 1_000 == 0 -> "${millis / 1_000}s"
    else -> "$millis ms"
}

/** "off", "3s": a crossfade is always chosen in whole seconds. */
private fun crossfadeLabel(millis: Int): String = when {
    millis <= 0 -> "off"
    millis % 1_000 == 0 -> "${millis / 1_000}s"
    else -> "$millis ms"
}

/**
 * A preamp as it is read out.
 *
 * The sign is always shown, so a row of them lines up and a preamp of nothing does not
 * look like one that has been forgotten.
 */
private fun preampLabel(decibels: Float): String {
    val value = decibels.toInt()
    return if (value > 0) "+$value dB" else "$value dB"
}

/**
 * The option closest to a stored value.
 *
 * A value written before the choices changed still lands on the nearest stop rather than
 * jumping to the first, and the row's value text keeps saying what was stored.
 */
private fun nearestIndex(options: List<Int>, value: Int): Int =
    options.indices.minByOrNull { kotlin.math.abs(options[it] - value) } ?: 0

/**
 * Every setting, found by typing what it is about.
 *
 * A screen of its own rather than a field over each page, because the listener knows the
 * word and not which of eight pages it was filed under. A result is a signpost rather than
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
        SettingsPageHeader(
            title = "Search settings",
            subtitle = "Find a row by its name, what it does, or the word you would use for it.",
            onBack = onBack,
        )
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
            text = if (entry.key == "library.hidden_names") {
                hiddenNamesSubtitle(settings)
            } else {
                entry.subtitle
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
        Text(
            text = if (entry.section.isBlank()) {
                entry.group.title
            } else {
                "${entry.group.title} · ${entry.section}"
            },
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
