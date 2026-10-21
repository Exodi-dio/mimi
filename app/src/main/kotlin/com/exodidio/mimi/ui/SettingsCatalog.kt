package com.exodidio.mimi.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Disc3
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Wrench

/**
 * The five places settings are filed under.
 *
 * These are the drawer's rows and the titles of its pages, so the two cannot drift: the
 * drawer is drawn from this enum rather than from a list written beside it. Each one
 * carries its own icon and a line saying what is inside, because a drawer is read at a
 * glance and "Appearance" alone does not say whether it holds the theme or the fonts.
 *
 * Public because the catalogue names a group for every row it knows about, and a second
 * list of group names would be a second thing to keep in step.
 */
enum class SettingsGroupId(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    LIBRARY(
        title = "Library",
        subtitle = "Finding music, names and grouping, and what to leave out.",
        icon = Lucide.Disc3,
    ),
    PLAYBACK(
        title = "Playback",
        subtitle = "Sound, behaviour and the words on screen.",
        icon = Lucide.Play,
    ),
    APPEARANCE(
        title = "Appearance",
        subtitle = "Theme, and how the rows and lists are laid out.",
        icon = Lucide.Palette,
    ),
    TOOLS(
        title = "Tools",
        subtitle = "The equaliser, statistics and library cleanup.",
        icon = Lucide.Wrench,
    ),
    ABOUT(
        title = "About",
        subtitle = "This app, its licence and the pieces it is built from.",
        icon = Lucide.Info,
    ),
}

/**
 * One row of a settings page, described rather than drawn.
 *
 * The catalogue is the only list of what settings exist, so search, the rows themselves
 * and the test that checks the grouping all read the same order. [title], [subtitle] and
 * [keywords] are the words a listener might search for, which is why a row about the
 * equaliser also carries "bass" and "treble": nobody types the name of the setting they
 * have not found yet.
 */
data class SettingEntry(
    val key: String,
    val group: SettingsGroupId,
    val section: String,
    val title: String,
    val subtitle: String,
    val keywords: List<String> = emptyList(),
)

/**
 * Every setting, in the order it is shown.
 *
 * Sections appear in contiguous runs within a group because the page renderer opens a
 * heading whenever the section changes; grouping rows by section here is what decides the
 * shape of every page. Keywords are lowercase because matching normalises the query and
 * not the catalogue.
 *
 * Tools holds the things that act rather than choose — the equaliser, the statistics, the
 * duplicate finder and the hidden names — because they are errands rather than prefs, and
 * a listener looking for one of them looks under "tools" long before "library".
 */
val settingsCatalog: List<SettingEntry> = listOf(
    SettingEntry(
        key = "library.scan_on_launch",
        group = SettingsGroupId.LIBRARY,
        section = "Finding music",
        title = "Scan when the app opens",
        subtitle = "Looks for new music when you come back to it.",
        keywords = listOf("scan", "startup", "refresh", "open", "new music"),
    ),
    SettingEntry(
        key = "library.watch_for_changes",
        group = SettingsGroupId.LIBRARY,
        section = "Finding music",
        title = "Watch for changes",
        subtitle = "Rescans soon after files appear or disappear.",
        keywords = listOf("scan", "automatic", "files", "folders", "monitor"),
    ),
    SettingEntry(
        key = "library.min_song_length",
        group = SettingsGroupId.LIBRARY,
        section = "Finding music",
        title = "Shortest song shown",
        subtitle = "Keeps ringtones and voice notes out of the library.",
        keywords = listOf("length", "duration", "short", "ringtone", "voice note", "hide"),
    ),
    SettingEntry(
        key = "library.song_count",
        group = SettingsGroupId.LIBRARY,
        section = "This device",
        title = "Songs",
        subtitle = "How many songs the library holds.",
        keywords = listOf("count", "tracks", "size", "library"),
    ),
    SettingEntry(
        key = "library.rescan",
        group = SettingsGroupId.LIBRARY,
        section = "This device",
        title = "Rescan library",
        subtitle = "Look for new music now.",
        keywords = listOf("scan", "refresh", "update", "find", "new music"),
    ),
    SettingEntry(
        key = "library.played_from",
        group = SettingsGroupId.LIBRARY,
        section = "This device",
        title = "Played from",
        subtitle = "Files already on this device.",
        keywords = listOf("storage", "local", "source", "device", "files"),
    ),
    SettingEntry(
        key = "library.credit_separators",
        group = SettingsGroupId.LIBRARY,
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
        group = SettingsGroupId.LIBRARY,
        section = "Names and grouping",
        title = "Sort songs by",
        subtitle = "Orders the songs list.",
        keywords = listOf("order", "sorting", "alphabetical", "title", "list"),
    ),
    SettingEntry(
        key = "library.album_sort",
        group = SettingsGroupId.LIBRARY,
        section = "Names and grouping",
        title = "Sort albums by",
        subtitle = "Orders the albums grid.",
        keywords = listOf("order", "sorting", "grid", "name"),
    ),
    SettingEntry(
        key = "library.artist_sort",
        group = SettingsGroupId.LIBRARY,
        section = "Names and grouping",
        title = "Sort artists by",
        subtitle = "Orders the artists list.",
        keywords = listOf("order", "sorting", "name", "list"),
    ),
    SettingEntry(
        key = "library.sort_descending",
        group = SettingsGroupId.LIBRARY,
        section = "Names and grouping",
        title = "Reverse the chosen order",
        subtitle = "Songs, albums, artists and the lists inside them.",
        keywords = listOf("reverse", "direction", "order", "ascending", "descending"),
    ),
    SettingEntry(
        key = "library.detail_sort",
        group = SettingsGroupId.LIBRARY,
        section = "Names and grouping",
        title = "Sort lists inside a screen",
        subtitle = "Albums, artists, playlists and the browse screens. As listed unless asked.",
        keywords = listOf("order", "sorting", "detail", "album", "playlist", "browse", "list"),
    ),
    SettingEntry(
        key = "library.excluded_folders",
        group = SettingsGroupId.LIBRARY,
        section = "Songs to leave out",
        title = "Folders to leave out",
        subtitle = "Every folder on the device is read.",
        keywords = listOf("exclude", "ignore", "skip", "folder", "hide", "leave out"),
    ),
    SettingEntry(
        key = "playback.skip_silence",
        group = SettingsGroupId.PLAYBACK,
        section = "Sound",
        title = "Skip silent passages",
        subtitle = "Steps over long silences inside a track.",
        keywords = listOf("silence", "gap", "quiet", "pause"),
    ),
    SettingEntry(
        key = "playback.resume_session",
        group = SettingsGroupId.PLAYBACK,
        section = "Behaviour",
        title = "Resume where you left off",
        subtitle = "Opens on the last song you heard, paused where it stopped.",
        keywords = listOf("resume", "continue", "session", "last song", "start"),
    ),
    SettingEntry(
        key = "playback.pause_on_disconnect",
        group = SettingsGroupId.PLAYBACK,
        section = "Behaviour",
        title = "Pause when headphones unplug",
        subtitle = "Rather than carrying on through the speaker.",
        keywords = listOf("headphones", "bluetooth", "disconnect", "unplug", "speaker"),
    ),
    SettingEntry(
        key = "playback.fade_out_sleep_timer",
        group = SettingsGroupId.PLAYBACK,
        section = "Behaviour",
        title = "Fade out with the sleep timer",
        subtitle = "Turns the volume down over the last fifteen seconds.",
        keywords = listOf("sleep", "timer", "fade", "volume", "stop"),
    ),
    SettingEntry(
        key = "playback.show_remaining_time",
        group = SettingsGroupId.PLAYBACK,
        section = "Behaviour",
        title = "Count down instead of up",
        subtitle = "Shows the time remaining rather than the time played.",
        keywords = listOf("time", "remaining", "countdown", "progress", "clock"),
    ),
    SettingEntry(
        key = "playback.romanize_lyrics",
        group = SettingsGroupId.PLAYBACK,
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
        group = SettingsGroupId.APPEARANCE,
        section = "Theme",
        title = "Theme",
        subtitle = "Light, dark, or whatever the phone is doing.",
        keywords = listOf("dark", "light", "night", "appearance", "system", "colour"),
    ),
    SettingEntry(
        key = "appearance.show_artwork",
        group = SettingsGroupId.APPEARANCE,
        section = "Lists and layout",
        title = "Show cover art",
        subtitle = "Draws the cover beside each song in a list.",
        keywords = listOf("artwork", "cover", "album art", "picture", "list", "layout"),
    ),
    SettingEntry(
        key = "playback.equalizer_enabled",
        group = SettingsGroupId.TOOLS,
        section = "Equaliser",
        title = "Equaliser",
        subtitle = "Shapes the sound of what is playing. Off unless you ask for it.",
        keywords = listOf("sound", "bass", "treble", "preset", "audio", "eq", "shape"),
    ),
    SettingEntry(
        key = "playback.equalizer_preset",
        group = SettingsGroupId.TOOLS,
        section = "Equaliser",
        title = "Curves and bands",
        subtitle = "Pick a shape and drag the ten bands.",
        keywords = listOf(
            "equalizer", "equaliser", "bass", "treble", "preset", "band", "curve",
            "frequency", "sound",
        ),
    ),
    SettingEntry(
        key = "about.statistics",
        group = SettingsGroupId.TOOLS,
        section = "Statistics",
        title = "Statistics",
        subtitle = "How much there is, and how much you have played.",
        keywords = listOf("counts", "plays", "history", "listening", "numbers"),
    ),
    SettingEntry(
        key = "library.duplicates",
        group = SettingsGroupId.TOOLS,
        section = "Duplicate songs",
        title = "Find duplicates",
        subtitle = "Tracks sharing a title, artist and length.",
        keywords = listOf("duplicate", "same", "copies", "twice", "cleanup", "files"),
    ),
    SettingEntry(
        key = "library.hidden_names",
        group = SettingsGroupId.TOOLS,
        section = "Hidden names",
        title = "Hidden artists and albums",
        subtitle = "Names hidden from every list. Nothing is deleted.",
        keywords = listOf("hide", "hidden", "artist", "album", "unhide", "show", "leave out"),
    ),
    SettingEntry(
        key = "about.version",
        group = SettingsGroupId.ABOUT,
        section = "This app",
        title = "Version",
        subtitle = "The build installed on this device.",
        keywords = listOf("build", "release", "update", "number"),
    ),
    SettingEntry(
        key = "about.licence",
        group = SettingsGroupId.ABOUT,
        section = "Licences and credits",
        title = "Licence",
        subtitle = "GPL-3.0.",
        keywords = listOf("gpl", "legal", "open source", "copyright"),
    ),
    SettingEntry(
        key = "about.built_with",
        group = SettingsGroupId.ABOUT,
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
sealed interface SettingsGroupItem {
    data class Heading(val section: String) : SettingsGroupItem
    data class Row(val entry: SettingEntry) : SettingsGroupItem
}

/**
 * What a settings page should bring into view when it opens.
 *
 * Two things name a place on a page and they name it differently: a search result knows
 * the row it found, while a drawer row knows the section it leads to. They stay apart
 * rather than travelling as bare strings because the page does something different with
 * each — a found row is tinted for a moment, a section is only scrolled to.
 */
sealed interface SettingsReveal {

    /** The catalogue key of a row, which is what a search result carries. */
    data class Row(val key: String) : SettingsReveal

    /** A section heading, which is what a drawer row carries. */
    data class Section(val heading: String) : SettingsReveal
}

/**
 * The section headings of [group], in the order the page draws them.
 *
 * The drawer is a table of contents for the pages, so it takes its rows from the same
 * place the pages take their headings. That is the whole reason this is a function rather
 * than a list written in the drawer: a section added to the catalogue appears in both, and
 * a section renamed appears renamed in both.
 */
fun settingsSections(
    group: SettingsGroupId,
    entries: List<SettingEntry> = settingsCatalog,
): List<String> = settingsGroupItems(group, entries)
    .filterIsInstance<SettingsGroupItem.Heading>()
    .map { it.section }

/**
 * A group as the list it is drawn as.
 *
 * A heading is inserted each time the section changes, so the catalogue's order alone
 * decides where headings land and no page has to be described twice.
 */
fun settingsGroupItems(
    group: SettingsGroupId,
    entries: List<SettingEntry> = settingsCatalog,
): List<SettingsGroupItem> {
    val items = mutableListOf<SettingsGroupItem>()
    var current: String? = null
    entries.filter { it.group == group }.forEach { entry ->
        if (entry.section != current) {
            items += SettingsGroupItem.Heading(entry.section)
            current = entry.section
        }
        items += SettingsGroupItem.Row(entry)
    }
    return items
}

/**
 * Where [key] sits in [group]'s lazy list, or -1 when the group does not hold it.
 *
 * The index counts headings as well as rows because that is what a lazy list is scrolled
 * by; asking the catalogue for a row number would land one heading short on later sections.
 */
fun settingsItemIndex(
    group: SettingsGroupId,
    key: String,
    entries: List<SettingEntry> = settingsCatalog,
): Int = settingsGroupItems(group, entries)
    .indexOfFirst { it is SettingsGroupItem.Row && it.entry.key == key }

/**
 * Where the heading [section] sits in [group]'s lazy list, or -1 when the group has none.
 *
 * Drawer rows name sections rather than rows, so this is the index those rows scroll to.
 * Kept beside [settingsItemIndex] because both answer the same question — where does this
 * page start reading — and a second way of counting would disagree with the first.
 */
fun settingsSectionIndex(
    group: SettingsGroupId,
    section: String,
    entries: List<SettingEntry> = settingsCatalog,
): Int = settingsGroupItems(group, entries)
    .indexOfFirst { it is SettingsGroupItem.Heading && it.section == section }

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
