package com.exodidio.mimi.ui

/**
 * The four pages of settings.
 *
 * Public rather than private because the catalogue names a page for every row it knows
 * about, and a second list of page names would be a second thing to keep in step.
 */
enum class SettingsPage(val label: String) {
    LIBRARY("Library"),
    PLAYBACK("Playback"),
    APPEARANCE("Appearance"),
    ABOUT("About"),
}

/**
 * One row of the settings screen, described rather than drawn.
 *
 * The catalogue is the only list of what settings exist, so search, the rows themselves
 * and the test that checks the grouping all read the same order. [title], [subtitle] and
 * [keywords] are the words a listener might search for, which is why a row about the
 * equaliser also carries "bass" and "treble": nobody types the name of the setting they
 * have not found yet.
 */
data class SettingEntry(
    val key: String,
    val page: SettingsPage,
    val section: String,
    val title: String,
    val subtitle: String,
    val keywords: List<String> = emptyList(),
)

/**
 * Every setting, in the order it is shown.
 *
 * Sections appear in contiguous runs within a page because the page renderer opens a
 * heading whenever the section changes; grouping rows by section here is what decides the
 * shape of every page. Keywords are lowercase because matching normalises the query and
 * not the catalogue.
 */
val settingsCatalog: List<SettingEntry> = listOf(
    SettingEntry(
        key = "library.scan_on_launch",
        page = SettingsPage.LIBRARY,
        section = "Finding music",
        title = "Scan when the app opens",
        subtitle = "Looks for new music when you come back to it.",
        keywords = listOf("scan", "startup", "refresh", "open", "new music"),
    ),
    SettingEntry(
        key = "library.watch_for_changes",
        page = SettingsPage.LIBRARY,
        section = "Finding music",
        title = "Watch for changes",
        subtitle = "Rescans soon after files appear or disappear.",
        keywords = listOf("scan", "automatic", "files", "folders", "monitor"),
    ),
    SettingEntry(
        key = "library.min_song_length",
        page = SettingsPage.LIBRARY,
        section = "Finding music",
        title = "Shortest song shown",
        subtitle = "Keeps ringtones and voice notes out of the library.",
        keywords = listOf("length", "duration", "short", "ringtone", "voice note", "hide"),
    ),
    SettingEntry(
        key = "library.song_count",
        page = SettingsPage.LIBRARY,
        section = "This device",
        title = "Songs",
        subtitle = "How many songs the library holds.",
        keywords = listOf("count", "tracks", "size", "library"),
    ),
    SettingEntry(
        key = "library.rescan",
        page = SettingsPage.LIBRARY,
        section = "This device",
        title = "Rescan library",
        subtitle = "Look for new music now.",
        keywords = listOf("scan", "refresh", "update", "find", "new music"),
    ),
    SettingEntry(
        key = "library.played_from",
        page = SettingsPage.LIBRARY,
        section = "This device",
        title = "Played from",
        subtitle = "Files already on this device.",
        keywords = listOf("storage", "local", "source", "device", "files"),
    ),
    SettingEntry(
        key = "library.credit_separators",
        page = SettingsPage.LIBRARY,
        section = "Names and grouping",
        title = "Split names at",
        subtitle = "Marks and words that split one credit into several.",
        keywords = listOf(
            "artist", "genre", "split", "multiple", "separator", "comma", "feat",
            "featuring", "credits",
        ),
    ),
    SettingEntry(
        key = "library.song_sort",
        page = SettingsPage.LIBRARY,
        section = "Names and grouping",
        title = "Sort songs by",
        subtitle = "Orders the songs list.",
        keywords = listOf("order", "sorting", "alphabetical", "title", "list"),
    ),
    SettingEntry(
        key = "library.album_sort",
        page = SettingsPage.LIBRARY,
        section = "Names and grouping",
        title = "Sort albums by",
        subtitle = "Orders the albums grid.",
        keywords = listOf("order", "sorting", "grid", "name"),
    ),
    SettingEntry(
        key = "library.artist_sort",
        page = SettingsPage.LIBRARY,
        section = "Names and grouping",
        title = "Sort artists by",
        subtitle = "Orders the artists list.",
        keywords = listOf("order", "sorting", "name", "list"),
    ),
    SettingEntry(
        key = "library.sort_descending",
        page = SettingsPage.LIBRARY,
        section = "Names and grouping",
        title = "Sort songs descending",
        subtitle = "Newest first, longest first, Z to A, songs only.",
        keywords = listOf("reverse", "direction", "order", "ascending", "descending"),
    ),
    SettingEntry(
        key = "library.excluded_folders",
        page = SettingsPage.LIBRARY,
        section = "Songs to leave out",
        title = "Folders to leave out",
        subtitle = "Every folder on the device is read.",
        keywords = listOf("exclude", "ignore", "skip", "folder", "hide", "leave out"),
    ),
    SettingEntry(
        key = "playback.equalizer_enabled",
        page = SettingsPage.PLAYBACK,
        section = "Sound",
        title = "Equaliser",
        subtitle = "Shapes the sound of what is playing. Off unless you ask for it.",
        keywords = listOf("sound", "bass", "treble", "preset", "audio", "eq", "shape"),
    ),
    SettingEntry(
        key = "playback.equalizer_preset",
        page = SettingsPage.PLAYBACK,
        section = "Sound",
        title = "Curves and bands",
        subtitle = "Pick a shape and drag the ten bands.",
        keywords = listOf(
            "equalizer", "equaliser", "bass", "treble", "preset", "band", "curve",
            "frequency", "sound",
        ),
    ),
    SettingEntry(
        key = "playback.skip_silence",
        page = SettingsPage.PLAYBACK,
        section = "Sound",
        title = "Skip silent passages",
        subtitle = "Steps over long silences inside a track.",
        keywords = listOf("silence", "gap", "quiet", "pause"),
    ),
    SettingEntry(
        key = "playback.resume_session",
        page = SettingsPage.PLAYBACK,
        section = "Behaviour",
        title = "Resume where you left off",
        subtitle = "Opens on the last song you heard, paused where it stopped.",
        keywords = listOf("resume", "continue", "session", "last song", "start"),
    ),
    SettingEntry(
        key = "playback.pause_on_disconnect",
        page = SettingsPage.PLAYBACK,
        section = "Behaviour",
        title = "Pause when headphones unplug",
        subtitle = "Rather than carrying on through the speaker.",
        keywords = listOf("headphones", "bluetooth", "disconnect", "unplug", "speaker"),
    ),
    SettingEntry(
        key = "playback.fade_out_sleep_timer",
        page = SettingsPage.PLAYBACK,
        section = "Behaviour",
        title = "Fade out with the sleep timer",
        subtitle = "Turns the volume down over the last ten seconds.",
        keywords = listOf("sleep", "timer", "fade", "volume", "stop"),
    ),
    SettingEntry(
        key = "playback.show_remaining_time",
        page = SettingsPage.PLAYBACK,
        section = "Behaviour",
        title = "Count down instead of up",
        subtitle = "Shows the time remaining rather than the time played.",
        keywords = listOf("time", "remaining", "countdown", "progress", "clock"),
    ),
    SettingEntry(
        key = "playback.romanize_lyrics",
        page = SettingsPage.PLAYBACK,
        section = "Words",
        title = "Read lyrics in latin letters",
        subtitle = "Writes Chinese and Korean words in latin letters.",
        keywords = listOf(
            "pinyin", "romanise", "romanize", "transliterate", "transliteration",
            "lyrics", "chinese", "korean", "latin",
        ),
    ),
    SettingEntry(
        key = "appearance.theme",
        page = SettingsPage.APPEARANCE,
        section = "Theme",
        title = "Theme",
        subtitle = "Light, dark, or whatever the phone is doing.",
        keywords = listOf("dark", "light", "night", "appearance", "system", "colour"),
    ),
    SettingEntry(
        key = "appearance.show_artwork",
        page = SettingsPage.APPEARANCE,
        section = "Lists and layout",
        title = "Show cover art",
        subtitle = "Draws the cover beside each song in a list.",
        keywords = listOf("artwork", "cover", "album art", "picture", "list", "layout"),
    ),
    SettingEntry(
        key = "about.version",
        page = SettingsPage.ABOUT,
        section = "This app",
        title = "Version",
        subtitle = "The build installed on this device.",
        keywords = listOf("build", "release", "update", "number"),
    ),
    SettingEntry(
        key = "about.statistics",
        page = SettingsPage.ABOUT,
        section = "This app",
        title = "Statistics",
        subtitle = "How much there is, and how much you have played.",
        keywords = listOf("counts", "plays", "history", "listening", "numbers"),
    ),
    SettingEntry(
        key = "about.licence",
        page = SettingsPage.ABOUT,
        section = "Licences and credits",
        title = "Licence",
        subtitle = "GPL-3.0.",
        keywords = listOf("gpl", "legal", "open source", "copyright"),
    ),
    SettingEntry(
        key = "about.built_with",
        page = SettingsPage.ABOUT,
        section = "Licences and credits",
        title = "Built with",
        subtitle = "Media3, Compose and Room.",
        keywords = listOf("libraries", "credits", "compose", "room", "media3"),
    ),
)

/**
 * One line of a settings page: either a section heading or the row under it.
 *
 * The page renderer and the scroll-to-a-result helper both walk this list, which is what
 * keeps the item a search result points at and the item a page draws from drifting apart.
 */
sealed interface SettingsPageItem {
    data class Heading(val section: String) : SettingsPageItem
    data class Row(val entry: SettingEntry) : SettingsPageItem
}

/**
 * A page as the list it is drawn as.
 *
 * A heading is inserted each time the section changes, so the catalogue's order alone
 * decides where headings land and no page has to be described twice.
 */
fun settingsPageItems(
    page: SettingsPage,
    entries: List<SettingEntry> = settingsCatalog,
): List<SettingsPageItem> {
    val items = mutableListOf<SettingsPageItem>()
    var current: String? = null
    entries.filter { it.page == page }.forEach { entry ->
        if (entry.section != current) {
            items += SettingsPageItem.Heading(entry.section)
            current = entry.section
        }
        items += SettingsPageItem.Row(entry)
    }
    return items
}

/**
 * Where [key] sits in [page]'s lazy list, or -1 when the page does not hold it.
 *
 * The index counts headings as well as rows because that is what a lazy list is scrolled
 * by; asking the catalogue for a row number would land one heading short on later sections.
 */
fun settingsItemIndex(
    page: SettingsPage,
    key: String,
    entries: List<SettingEntry> = settingsCatalog,
): Int = settingsPageItems(page, entries)
    .indexOfFirst { it is SettingsPageItem.Row && it.entry.key == key }

/**
 * The settings matching [query], best match first.
 *
 * A title match outranks a keyword match and a keyword match outranks a subtitle match,
 * because somebody typing "bass" wants the equaliser before a row that merely mentions
 * bass in passing. Order inside a rank is the catalogue's order, so results are stable
 * between keystrokes and the list does not appear to shuffle while it is being read.
 *
 * Spaces are forgiven in both directions: runs of them are collapsed, and a match also
 * counts when they are removed entirely, so "cover art" and "coverart" find the same row.
 * A blank query matches nothing rather than everything, so clearing the field cannot be
 * mistaken for a search that found the whole catalogue.
 */
fun searchSettings(
    query: String,
    entries: List<SettingEntry> = settingsCatalog,
): List<SettingEntry> {
    val needle = query.normalisedForSearch()
    if (needle.isEmpty()) return emptyList()

    return entries
        .mapNotNull { entry ->
            val rank = when {
                entry.title.containsForgiving(needle) -> 0
                entry.keywords.any { it.containsForgiving(needle) } -> 1
                entry.subtitle.containsForgiving(needle) -> 2
                else -> null
            }
            rank?.let { entry to it }
        }
        .sortedBy { it.second }
        .map { it.first }
}

/** Lowercased, trimmed and with runs of whitespace folded, which is how both sides match. */
private fun String.normalisedForSearch(): String =
    trim().lowercase().replace(Regex("\\s+"), " ")

/** Whether this field holds [needle], with or without the spaces between its words. */
private fun String.containsForgiving(needle: String): Boolean {
    val field = normalisedForSearch()
    if (field.contains(needle)) return true
    return field.replace(" ", "").contains(needle.replace(" ", ""))
}
