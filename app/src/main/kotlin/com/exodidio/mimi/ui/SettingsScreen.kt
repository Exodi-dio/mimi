package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.AlbumSort
import com.exodidio.mimi.library.ArtistSort
import com.exodidio.mimi.library.FolderSummary
import com.exodidio.mimi.library.MinSongLength
import com.exodidio.mimi.library.Settings
import com.exodidio.mimi.library.SongSort
import com.exodidio.mimi.library.ThemeMode

/** The pages of the settings screen. */
private enum class SettingsTab(val label: String) {
    LIBRARY("Library"),
    PLAYBACK("Playback"),
    APPEARANCE("Appearance"),
    ABOUT("About"),
}

/** Which question, if any, is currently open in a dialog. */
private enum class SettingsChoice {
    MIN_LENGTH,
    THEME,
    SONG_SORT,
    ALBUM_SORT,
    ARTIST_SORT,
    EQUALIZER_PRESET,
    EXCLUDED_FOLDERS,
}

/**
 * Settings, split into pages.
 *
 * Split rather than listed because a settings screen that grows one row at a time becomes
 * unusable long before it becomes complete. Four pages is the shape where every row sits
 * under a heading somebody would look for it under, and it is the shape a skin can restyle
 * without reordering anything.
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
    var tab by rememberSaveable { mutableStateOf(SettingsTab.LIBRARY) }
    var openChoice by remember { mutableStateOf<SettingsChoice?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Settings")
        PillRow(
            options = SettingsTab.entries,
            selected = tab,
            label = { it.label },
            onSelect = { tab = it },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            when (tab) {
                SettingsTab.LIBRARY -> LibraryPage(
                    settings = settings,
                    onChange = onChange,
                    trackCount = trackCount,
                    scanning = scanning,
                    onRescan = onRescan,
                    onChooseLength = { openChoice = SettingsChoice.MIN_LENGTH },
                    onChooseSort = { openChoice = SettingsChoice.SONG_SORT },
                    onChooseAlbumSort = { openChoice = SettingsChoice.ALBUM_SORT },
                    onChooseArtistSort = { openChoice = SettingsChoice.ARTIST_SORT },
                    onChooseFolders = { openChoice = SettingsChoice.EXCLUDED_FOLDERS },
                )

                SettingsTab.PLAYBACK -> PlaybackPage(
                    settings = settings,
                    onChange = onChange,
                    onChooseEqualizerPreset = { openChoice = SettingsChoice.EQUALIZER_PRESET },
                )

                SettingsTab.APPEARANCE -> AppearancePage(
                    settings = settings,
                    onChange = onChange,
                    onChooseTheme = { openChoice = SettingsChoice.THEME },
                )

                SettingsTab.ABOUT -> AboutPage(onOpenStatistics = onOpenStatistics)
            }
            SettingsPageFooter()
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

        null -> Unit
    }
}

@Composable
private fun LibraryPage(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    trackCount: Int,
    scanning: Boolean,
    onRescan: () -> Unit,
    onChooseLength: () -> Unit,
    onChooseSort: () -> Unit,
    onChooseAlbumSort: () -> Unit,
    onChooseArtistSort: () -> Unit,
    onChooseFolders: () -> Unit,
) {
    SettingsSectionHeading("Finding music")
    SettingsToggleRow(
        title = "Scan when the app opens",
        subtitle = "Looks for new music when you come back to it.",
        checked = settings.scanOnLaunch,
        onCheckedChange = { value -> onChange { it.copy(scanOnLaunch = value) } },
    )
    SettingsToggleRow(
        title = "Watch for changes",
        subtitle = "Rescans shortly after files appear or disappear on the device.",
        checked = settings.watchForChanges,
        onCheckedChange = { value -> onChange { it.copy(watchForChanges = value) } },
    )
    SettingsChoiceRow(
        title = "Shortest song shown",
        subtitle = "Keeps ringtones and voice notes out of the library.",
        value = settings.minSongLength.label,
        onClick = onChooseLength,
    )
    if (settings.minSongLength != MinSongLength.ANY) {
        SettingsNote(
            "Changing this rebuilds the library, which takes a moment on a large collection."
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    SettingsSectionHeading("On this device")
    SettingsInfoRow(title = "Songs", value = songCountLabel(trackCount))
    SettingsActionRow(
        title = "Rescan library",
        subtitle = if (scanning) "Looking…" else "Look for new music now.",
        enabled = !scanning,
        onClick = onRescan,
    )
    SettingsInfoRow(title = "Played from", value = "This device")

    HorizontalDivider(
        modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    SettingsSectionHeading("Order")
    SettingsChoiceRow(
        title = "Sort songs by",
        subtitle = "Orders the songs list.",
        value = settings.songSort.label,
        onClick = onChooseSort,
    )
    SettingsChoiceRow(
        title = "Sort albums by",
        subtitle = "Orders the albums grid.",
        value = settings.albumSort.label,
        onClick = onChooseAlbumSort,
    )
    SettingsChoiceRow(
        title = "Sort artists by",
        subtitle = "Orders the artists list.",
        value = settings.artistSort.label,
        onClick = onChooseArtistSort,
    )
    SettingsToggleRow(
        title = "Sort songs descending",
        subtitle = "Newest first, longest first, Z to A. Albums and artists keep their own direction.",
        checked = settings.sortDescending,
        onCheckedChange = { value -> onChange { it.copy(sortDescending = value) } },
    )

    SettingsSectionHeading("Folders")
    SettingsActionRow(
        title = "Folders to leave out",
        subtitle = when (settings.excludedFolders.size) {
            0 -> "Every folder on the device is read."
            1 -> "One folder is left out of the library."
            else -> "${settings.excludedFolders.size} folders are left out of the library."
        },
        onClick = onChooseFolders,
    )
}

@Composable
private fun PlaybackPage(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onChooseEqualizerPreset: () -> Unit,
) {
    SettingsSectionHeading("Behaviour")
    SettingsToggleRow(
        title = "Resume where you left off",
        subtitle = "Opens on the last song you heard, paused where it stopped.",
        checked = settings.resumeSession,
        onCheckedChange = { value -> onChange { it.copy(resumeSession = value) } },
    )
    SettingsToggleRow(
        title = "Pause when headphones unplug",
        subtitle = "Rather than carrying on through the speaker.",
        checked = settings.pauseOnDisconnect,
        onCheckedChange = { value -> onChange { it.copy(pauseOnDisconnect = value) } },
    )
    SettingsToggleRow(
        title = "Skip silent passages",
        subtitle = "Steps over long silences inside a track.",
        checked = settings.skipSilence,
        onCheckedChange = { value -> onChange { it.copy(skipSilence = value) } },
    )
    SettingsToggleRow(
        title = "Fade out with the sleep timer",
        subtitle = "Turns the volume down over the last ten seconds instead of cutting off.",
        checked = settings.fadeOutSleepTimer,
        onCheckedChange = { value -> onChange { it.copy(fadeOutSleepTimer = value) } },
    )

    HorizontalDivider(
        modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    SettingsSectionHeading("The player")
    SettingsToggleRow(
        title = "Count down instead of up",
        subtitle = "Shows the time remaining rather than the time played.",
        checked = settings.showRemainingTime,
        onCheckedChange = { value -> onChange { it.copy(showRemainingTime = value) } },
    )
    SettingsToggleRow(
        title = "Equaliser",
        subtitle = "Shapes the sound of what is playing. Off unless you ask for it.",
        checked = settings.equalizerEnabled,
        onCheckedChange = { value -> onChange { it.copy(equalizerEnabled = value) } },
    )
    // Always offered, even when the equaliser is off: the panel is where it is switched
    // on, and a row that disappears is a row you have to find twice.
    SettingsChoiceRow(
        title = "Curves and bands",
        subtitle = "Pick a shape and drag the ten bands.",
        value = settings.equalizerChoices
            .firstOrNull { it.key == settings.equalizerPresetKey }
            ?.let { chosen ->
                if (chosen.gainsDb == settings.equalizerGains) chosen.name else "${chosen.name} (edited)"
            }
            ?: "Flat",
        onClick = onChooseEqualizerPreset,
    )
}

@Composable
private fun AppearancePage(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onChooseTheme: () -> Unit,
) {
    SettingsSectionHeading("Theme")
    SettingsChoiceRow(
        title = "Theme",
        value = settings.themeMode.label(),
        onClick = onChooseTheme,
    )

    HorizontalDivider(
        modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    SettingsSectionHeading("Lists")
    SettingsToggleRow(
        title = "Show cover art",
        subtitle = "Draws the cover beside each song in a list.",
        checked = settings.showArtwork,
        onCheckedChange = { value -> onChange { it.copy(showArtwork = value) } },
    )
    SettingsNote("These are the choices the new look will read from.")
}

@Composable
private fun AboutPage(onOpenStatistics: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "dev"
    }

    SettingsSectionHeading("Mimi")
    SettingsInfoRow(title = "Version", value = version)
    SettingsInfoRow(title = "Licence", value = "GPL-3.0")
    SettingsInfoRow(title = "Built with", value = "Media3, Compose and Room")
    SettingsNote(
        "Plays the files already on this device. No account, no adverts, and nothing sent " +
            "anywhere. Lyrics are read out of the files themselves."
    )

    SettingsSectionHeading("Numbers")
    SettingsActionRow(
        title = "Statistics",
        subtitle = "How much there is, and how much of it you have played",
        onClick = onOpenStatistics,
    )
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Follow the phone"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
