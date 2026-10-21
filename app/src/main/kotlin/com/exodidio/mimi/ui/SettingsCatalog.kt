package com.exodidio.mimi.ui

/**
 * The places settings are filed under.
 *
 * These are the titles of the pages, so a page cannot be called one thing by the index and
 * another by itself. The subtitle is the line under the big title, worded once here rather
 * than repeated by whoever draws the page.
 *
 * The drawer no longer lists these: it lists destinations, and Settings is one of them.
 * [SettingsHomeScreen] is the index that lists these pages, which is why this enum is still
 * the single list of pages rather than a table written beside it.
 */
enum class SettingsGroupId(
    val title: String,
    val subtitle: String,
) {
    LIBRARY(
        title = "Library",
        subtitle = "What surfaces in your library, and how artists and genres are grouped.",
    ),
    AUDIO(
        title = "Audio",
        subtitle = "How tracks crossfade, duck for system sounds, and which engine decodes them.",
    ),
    IMAGES(
        title = "Images",
        subtitle = "Album artwork, metadata downloads, and what shows on the lock screen.",
    ),
    CUSTOMIZATION(
        title = "Customization",
        subtitle = "Tailor what you see on the home and details screens.",
    ),
    NOW_PLAYING(
        title = "Now playing",
        subtitle = "How the now-playing screen looks and what controls it shows.",
    ),
    PLAYLISTS(
        title = "Playlists",
        subtitle = "Where exported playlists are saved, and how recent counts are sliced.",
    ),
    BACKUP(
        title = "Backup & restore",
        subtitle = "Save your library to a file, and bring it back any time.",
    ),
    ABOUT(
        title = "About",
        subtitle = "This app, its licence and the pieces it is built from.",
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
 *
 * [badge] is the small label beside a title that says a row is not finished — "BETA" on
 * the fade and crossfade rows. [note] marks a row that is drawn as a tinted card of its
 * own rather than as a line inside a card, which is how an explanation that is not a
 * setting stays out of the rows it explains.
 */
data class SettingEntry(
    val key: String,
    val group: SettingsGroupId,
    val section: String,
    val title: String,
    val subtitle: String,
    val keywords: List<String> = emptyList(),
    val badge: String? = null,
    val note: Boolean = false,
)

/**
 * Every setting, in the order it is shown.
 *
 * Sections appear in contiguous runs within a group because the page renderer opens a
 * heading whenever the section changes; grouping rows by section here is what decides the
 * shape of every page. A blank section means the card has no heading above it, which is
 * what the now playing, playlists and backup pages ask for: one card, no label.
 *
 * Keywords are lowercase because matching normalises the query and not the catalogue.
 */
val settingsCatalog: List<SettingEntry> = listOf(
    // Library
    SettingEntry(
        key = "library.multiple_artists",
        group = SettingsGroupId.LIBRARY,
        section = "MULTIPLE ARTISTS",
        title = "Multiple artists support",
        subtitle = "Split artist and album-artist tags with ( , & ; ) separators.",
        keywords = listOf(
            "artist", "multiple", "split", "separator", "album artist", "tag", "credit",
        ),
    ),
    SettingEntry(
        key = "library.artist_separators",
        group = SettingsGroupId.LIBRARY,
        section = "MULTIPLE ARTISTS",
        title = "Symbols to split artists",
        subtitle = "Add separators based on which artists will be split",
        keywords = listOf(
            "artist", "split", "separator", "comma", "ampersand", "feat", "symbol", "mark",
        ),
    ),
    SettingEntry(
        key = "library.always_first_artist",
        group = SettingsGroupId.LIBRARY,
        section = "MULTIPLE ARTISTS",
        title = "Always navigate to first artist",
        subtitle = "When go to artist option is clicked, always navigate to the first artist",
        keywords = listOf("artist", "navigate", "first", "go to artist", "open"),
    ),
    SettingEntry(
        key = "library.multiple_genres",
        group = SettingsGroupId.LIBRARY,
        section = "MULTIPLE GENRES",
        title = "Multiple genres support",
        subtitle = "Split genre tags with ( , & ; ) separators.",
        keywords = listOf("genre", "multiple", "split", "separator", "tag"),
    ),
    SettingEntry(
        key = "library.genre_separators",
        group = SettingsGroupId.LIBRARY,
        section = "MULTIPLE GENRES",
        title = "Symbols to split genres",
        subtitle = "Add separators based on which genres will be split",
        keywords = listOf("genre", "split", "separator", "symbol", "mark", "slash"),
    ),
    SettingEntry(
        key = "library.excluded_folders",
        group = SettingsGroupId.LIBRARY,
        section = "FILTER",
        title = "Blacklist",
        subtitle = "Songs from the blacklisted folders will be hidden from your library",
        keywords = listOf(
            "blacklist", "exclude", "ignore", "skip", "folder", "hide", "leave out", "block",
        ),
    ),
    SettingEntry(
        key = "library.duration_range",
        group = SettingsGroupId.LIBRARY,
        section = "FILTER",
        title = "Duration range",
        subtitle = "Hide songs outside this duration. Drag either thumb; pull the right one " +
            "to \u221e for no upper limit.",
        keywords = listOf(
            "duration", "length", "short", "long", "ringtone", "voice note", "hide", "range",
            "minimum", "maximum",
        ),
    ),
    SettingEntry(
        key = "library.duplicates",
        group = SettingsGroupId.LIBRARY,
        section = "CLEANUP",
        title = "Find duplicates",
        subtitle = "Tracks sharing a title, artist and length.",
        keywords = listOf("duplicate", "same", "copies", "twice", "cleanup", "files"),
    ),
    SettingEntry(
        key = "library.hidden_names",
        group = SettingsGroupId.LIBRARY,
        section = "CLEANUP",
        title = "Hidden artists and albums",
        subtitle = "Names hidden from every list. Nothing is deleted.",
        keywords = listOf("hide", "hidden", "artist", "album", "unhide", "show", "leave out"),
    ),
    SettingEntry(
        key = "library.scan_on_launch",
        group = SettingsGroupId.LIBRARY,
        section = "SCANNING",
        title = "Scan when the app opens",
        subtitle = "Looks for new music when you come back to it.",
        keywords = listOf("scan", "startup", "refresh", "open", "new music"),
    ),
    SettingEntry(
        key = "library.watch_for_changes",
        group = SettingsGroupId.LIBRARY,
        section = "SCANNING",
        title = "Watch for changes",
        subtitle = "Rescans soon after files appear or disappear.",
        keywords = listOf("scan", "automatic", "files", "folders", "monitor"),
    ),
    SettingEntry(
        key = "library.rescan",
        group = SettingsGroupId.LIBRARY,
        section = "SCANNING",
        title = "Rescan library",
        subtitle = "Look for new music now.",
        keywords = listOf("scan", "refresh", "update", "find", "new music"),
    ),
    SettingEntry(
        key = "library.song_count",
        group = SettingsGroupId.LIBRARY,
        section = "SCANNING",
        title = "Songs",
        subtitle = "How many songs the library holds.",
        keywords = listOf("count", "tracks", "size", "library"),
    ),
    SettingEntry(
        key = "library.song_sort",
        group = SettingsGroupId.LIBRARY,
        section = "SORTING",
        title = "Sort songs by",
        subtitle = "Orders the songs list.",
        keywords = listOf("order", "sorting", "alphabetical", "title", "list"),
    ),
    SettingEntry(
        key = "library.album_sort",
        group = SettingsGroupId.LIBRARY,
        section = "SORTING",
        title = "Sort albums by",
        subtitle = "Orders the albums grid.",
        keywords = listOf("order", "sorting", "grid", "name"),
    ),
    SettingEntry(
        key = "library.artist_sort",
        group = SettingsGroupId.LIBRARY,
        section = "SORTING",
        title = "Sort artists by",
        subtitle = "Orders the artists list.",
        keywords = listOf("order", "sorting", "name", "list"),
    ),
    SettingEntry(
        key = "library.sort_descending",
        group = SettingsGroupId.LIBRARY,
        section = "SORTING",
        title = "Reverse the chosen order",
        subtitle = "Songs, albums, artists and the lists inside them.",
        keywords = listOf("reverse", "direction", "order", "ascending", "descending"),
    ),
    SettingEntry(
        key = "library.detail_sort",
        group = SettingsGroupId.LIBRARY,
        section = "SORTING",
        title = "Sort lists inside a screen",
        subtitle = "Albums, artists, playlists and the browse screens. As listed unless asked.",
        keywords = listOf("order", "sorting", "detail", "album", "playlist", "browse", "list"),
    ),

    // Audio
    SettingEntry(
        key = "playback.fade_on_pause",
        group = SettingsGroupId.AUDIO,
        section = "PLAYBACK",
        title = "Fade music on play / pause",
        subtitle = "Smooth fade in/out instead of cutting abruptly.",
        keywords = listOf("fade", "pause", "play", "volume", "smooth", "ramp"),
        badge = "BETA",
    ),
    SettingEntry(
        key = "playback.crossfade",
        group = SettingsGroupId.AUDIO,
        section = "PLAYBACK",
        title = "Crossfade",
        subtitle = "Overlap the end of one track with the start of the next.",
        keywords = listOf("crossfade", "overlap", "gapless", "transition", "blend"),
        badge = "BETA",
    ),
    SettingEntry(
        key = "playback.fade_note",
        group = SettingsGroupId.AUDIO,
        section = "PLAYBACK",
        title = "About fade and crossfade",
        subtitle = "The crossfade really overlaps: the track that is ending is faded down " +
            "while the next one comes up over it. The fade on play and pause is still " +
            "remembered but not applied.",
        keywords = listOf("fade", "crossfade", "not applied", "beta", "gap"),
        note = true,
    ),
    SettingEntry(
        key = "playback.gapless",
        group = SettingsGroupId.AUDIO,
        section = "PLAYBACK",
        title = "Gapless playback",
        subtitle = "Remove gaps of silence between the tracks. Might cause playback issues " +
            "on some devices.",
        keywords = listOf("gapless", "gap", "silence", "join", "continuous"),
    ),
    SettingEntry(
        key = "playback.gapless_note",
        group = SettingsGroupId.AUDIO,
        section = "PLAYBACK",
        title = "About gapless playback",
        subtitle = "The decoder joins tracks that carry gapless metadata. With this off, a " +
            "short silence is left between the tracks so the join cannot close the gap.",
        keywords = listOf("gapless", "gap", "silence", "decoder", "join"),
        note = true,
    ),
    SettingEntry(
        key = "playback.keep_shuffle",
        group = SettingsGroupId.AUDIO,
        section = "PLAYBACK",
        title = "Keep shuffle mode",
        subtitle = "Shuffle mode won't turn off when playing a new list of songs.",
        keywords = listOf("shuffle", "random", "keep", "new list", "mode"),
    ),
    SettingEntry(
        key = "playback.equalizer",
        group = SettingsGroupId.AUDIO,
        section = "PLAYBACK",
        title = "Equalizer",
        subtitle = "Shape EQ bands, bass boost, reverb, and virtualizer.",
        keywords = listOf(
            "equalizer", "equaliser", "bass", "treble", "preset", "band", "curve", "sound",
            "eq",
        ),
    ),
    SettingEntry(
        key = "playback.skip_silence",
        group = SettingsGroupId.AUDIO,
        section = "PLAYBACK",
        title = "Skip silent passages",
        subtitle = "Steps over long silences inside a track.",
        keywords = listOf("silence", "gap", "quiet", "pause"),
    ),
    SettingEntry(
        key = "playback.engine",
        group = SettingsGroupId.AUDIO,
        section = "ENGINE",
        title = "Playback engine",
        subtitle = "Select the Enhanced engine for best quality and broadest format support.",
        keywords = listOf("engine", "decoder", "mediaplayer", "exoplayer", "format", "quality"),
    ),
    SettingEntry(
        key = "playback.eq_band_count",
        group = SettingsGroupId.AUDIO,
        section = "ENGINE",
        title = "EQ Band Count",
        subtitle = "More bands = finer tone control, slightly more CPU.",
        keywords = listOf("eq", "band", "count", "equaliser", "equalizer", "cpu"),
    ),
    SettingEntry(
        key = "playback.eq_band_note",
        group = SettingsGroupId.AUDIO,
        section = "ENGINE",
        title = "About EQ band count",
        subtitle = "The equaliser uses the band count your device reports. This preference " +
            "is remembered, but a build cannot ask for a different count yet.",
        keywords = listOf("eq", "band", "count", "device", "not applied"),
        note = true,
    ),
    SettingEntry(
        key = "playback.replaygain_mode",
        group = SettingsGroupId.AUDIO,
        section = "REPLAYGAIN",
        title = "ReplayGain source mode",
        subtitle = "Normalize track-to-track loudness from the tags in the file.",
        keywords = listOf("replaygain", "loudness", "volume", "normalize", "gain", "track"),
    ),
    SettingEntry(
        key = "playback.replaygain_preamp",
        group = SettingsGroupId.AUDIO,
        section = "REPLAYGAIN",
        title = "ReplayGain preamp",
        subtitle = "Set preamp when ReplayGain is enabled",
        keywords = listOf("replaygain", "preamp", "gain", "decibel", "loudness"),
    ),
    SettingEntry(
        key = "playback.replaygain_note",
        group = SettingsGroupId.AUDIO,
        section = "REPLAYGAIN",
        title = "About ReplayGain",
        subtitle = "The gain in the file is read from its own tags and applied to the " +
            "volume, with the preamp added to it. A file that carries no tags plays as " +
            "it was recorded.",
        keywords = listOf("replaygain", "loudness", "normalize", "tags", "volume"),
        note = true,
    ),
    SettingEntry(
        key = "playback.duck_on_focus_loss",
        group = SettingsGroupId.AUDIO,
        section = "MISC",
        title = "Lower volume on focal loss",
        subtitle = "Lower the volume when a system sound is played or a notification is received",
        keywords = listOf(
            "focus", "focal", "duck", "notification", "volume", "lower", "system sound",
        ),
    ),
    SettingEntry(
        key = "playback.auto_play",
        group = SettingsGroupId.AUDIO,
        section = "MISC",
        title = "Auto play",
        subtitle = "Start playing immediately when a wired headset or bluetooth audio " +
            "device is plugged in.",
        keywords = listOf("auto", "headset", "bluetooth", "plug", "connect", "resume"),
    ),
    SettingEntry(
        key = "playback.resume_session",
        group = SettingsGroupId.AUDIO,
        section = "MISC",
        title = "Resume where you left off",
        subtitle = "Opens on the last song you heard, paused where it stopped.",
        keywords = listOf("resume", "continue", "session", "last song", "start"),
    ),
    SettingEntry(
        key = "playback.pause_on_disconnect",
        group = SettingsGroupId.AUDIO,
        section = "MISC",
        title = "Pause when headphones unplug",
        subtitle = "Rather than carrying on through the speaker.",
        keywords = listOf("headphones", "bluetooth", "disconnect", "unplug", "speaker"),
    ),
    SettingEntry(
        key = "playback.fade_out_sleep_timer",
        group = SettingsGroupId.AUDIO,
        section = "MISC",
        title = "Fade out with the sleep timer",
        subtitle = "Turns the volume down over the last fifteen seconds.",
        keywords = listOf("sleep", "timer", "fade", "volume", "stop"),
    ),
    SettingEntry(
        key = "playback.romanize_lyrics",
        group = SettingsGroupId.AUDIO,
        section = "WORDS",
        title = "Read lyrics in latin letters",
        subtitle = "Writes Chinese and Korean words in latin letters.",
        keywords = listOf(
            "pinyin", "romanise", "romanize", "transliterate", "transliteration",
            "lyrics", "chinese", "korean", "latin",
        ),
    ),

    // Images
    SettingEntry(
        key = "images.cache_images",
        group = SettingsGroupId.IMAGES,
        section = "STORAGE",
        title = "Cache images",
        subtitle = "Faster loading of images but will increase app size due to caching.",
        keywords = listOf("cache", "images", "artwork", "storage", "size", "memory"),
    ),
    SettingEntry(
        key = "images.mediastore_art",
        group = SettingsGroupId.IMAGES,
        section = "METADATA",
        title = "Use MediaStore provided art",
        subtitle = "Faster artwork loading time, but can show incorrect artwork",
        keywords = listOf("mediastore", "album art", "cover", "artwork", "incorrect"),
    ),
    SettingEntry(
        key = "images.auto_download_metadata",
        group = SettingsGroupId.IMAGES,
        section = "METADATA",
        title = "Auto download metadata",
        subtitle = "Auto download metadata(artist images, artist bio etc)",
        keywords = listOf("download", "metadata", "artist image", "bio", "network", "always"),
    ),
    SettingEntry(
        key = "images.lockscreen_note",
        group = SettingsGroupId.IMAGES,
        section = "LOCKSCREEN",
        title = "About lockscreen art",
        subtitle = "Adds album art to the media notification and controls. It may not appear " +
            "on the lock screen itself on Android 11 and newer.",
        keywords = listOf("lockscreen", "lock screen", "notification", "album art", "android 11"),
        note = true,
    ),
    SettingEntry(
        key = "images.lockscreen_art",
        group = SettingsGroupId.IMAGES,
        section = "LOCKSCREEN",
        title = "Lockscreen album art",
        subtitle = "Show album art on the lockscreen",
        keywords = listOf("lockscreen", "lock screen", "album art", "cover", "notification"),
    ),
    SettingEntry(
        key = "images.blur_album_cover",
        group = SettingsGroupId.IMAGES,
        section = "LOCKSCREEN",
        title = "Blur album cover",
        subtitle = "Blur album art on the lockscreen",
        keywords = listOf("blur", "lockscreen", "album art", "cover"),
    ),

    // Customization
    SettingEntry(
        key = "customization.bottom_tabs",
        group = SettingsGroupId.CUSTOMIZATION,
        section = "HOME",
        title = "Manage bottom tabs",
        subtitle = "Reorder and set visibility of bottom tabs",
        keywords = listOf("bottom", "tabs", "navigation", "reorder", "hide", "home"),
    ),
    SettingEntry(
        key = "customization.home_rows",
        group = SettingsGroupId.CUSTOMIZATION,
        section = "HOME",
        title = "Manage home rows",
        subtitle = "Reorder or hide sections shown on the home tab",
        keywords = listOf("home", "rows", "sections", "reorder", "hide", "shelves"),
    ),
    SettingEntry(
        key = "customization.see_all",
        group = SettingsGroupId.CUSTOMIZATION,
        section = "DETAILS",
        title = "Show See all button",
        subtitle = "Show or hide See all button in details page.",
        keywords = listOf("see all", "details", "button", "album", "artist", "list"),
    ),
    // Theme and cover art are the two appearance choices MIMI already had. They are kept
    // here rather than in a page of their own because a skin with no way to change the
    // theme would be a regression, even though the reference page names only two sections.
    SettingEntry(
        key = "customization.theme",
        group = SettingsGroupId.CUSTOMIZATION,
        section = "THEME",
        title = "Theme",
        subtitle = "Light, dark, or whatever the phone is doing.",
        keywords = listOf("dark", "light", "night", "appearance", "system", "colour"),
    ),
    SettingEntry(
        key = "customization.show_artwork",
        group = SettingsGroupId.CUSTOMIZATION,
        section = "THEME",
        title = "Show cover art",
        subtitle = "Draws the cover beside each song in a list.",
        keywords = listOf("artwork", "cover", "album art", "picture", "list", "layout"),
    ),

    // Now playing
    SettingEntry(
        key = "now_playing.theme",
        group = SettingsGroupId.NOW_PLAYING,
        section = "",
        title = "Now Playing Theme",
        subtitle = "The look of the main player.",
        keywords = listOf("theme", "player", "look", "default", "now playing"),
    ),
    SettingEntry(
        key = "now_playing.song_info",
        group = SettingsGroupId.NOW_PLAYING,
        section = "",
        title = "Show song info",
        subtitle = "Show song information such as file format, bitrate and frequency in the " +
            "main player",
        keywords = listOf("song info", "format", "bitrate", "frequency", "sample rate", "player"),
    ),
    SettingEntry(
        key = "now_playing.volume_slider",
        group = SettingsGroupId.NOW_PLAYING,
        section = "",
        title = "Show volume slider",
        subtitle = "Show volume slider in the main player",
        keywords = listOf("volume", "slider", "player"),
    ),
    SettingEntry(
        key = "now_playing.extra_controls",
        group = SettingsGroupId.NOW_PLAYING,
        section = "",
        title = "Extra controls",
        subtitle = "Add previous and forward button in the mini player",
        keywords = listOf("extra", "controls", "previous", "forward", "mini player", "buttons"),
    ),
    SettingEntry(
        key = "now_playing.remaining_time",
        group = SettingsGroupId.NOW_PLAYING,
        section = "",
        title = "Count down instead of up",
        subtitle = "Shows the time remaining rather than the time played.",
        keywords = listOf("time", "remaining", "countdown", "progress", "clock"),
    ),

    // Playlists
    SettingEntry(
        key = "playlists.export_path",
        group = SettingsGroupId.PLAYLISTS,
        section = "",
        title = "Playlist export path",
        subtitle = "Not set",
        keywords = listOf("export", "playlist", "path", "folder", "save", "file"),
    ),
    SettingEntry(
        key = "playlists.last_added_range",
        group = SettingsGroupId.PLAYLISTS,
        section = "",
        title = "Last added playlist",
        subtitle = "How far back the automatic list of new songs reaches.",
        keywords = listOf("last added", "new", "range", "months", "playlist", "recent"),
    ),
    SettingEntry(
        key = "playlists.recently_played_range",
        group = SettingsGroupId.PLAYLISTS,
        section = "",
        title = "Recently played",
        subtitle = "How far back the automatic list of played songs reaches.",
        keywords = listOf("recently played", "history", "range", "months", "playlist"),
    ),

    // Backup and restore
    SettingEntry(
        key = "backup.note",
        group = SettingsGroupId.BACKUP,
        section = "",
        title = "What a backup holds",
        subtitle = "Audio files aren't backed up, only your library data. Songs stay on " +
            "your device.",
        keywords = listOf("backup", "restore", "audio files", "library data"),
        note = true,
    ),
    SettingEntry(
        key = "backup.create",
        group = SettingsGroupId.BACKUP,
        section = "MANUAL",
        title = "Create backup",
        subtitle = "Save a backup file to a location you choose",
        keywords = listOf("backup", "create", "save", "export", "file"),
    ),
    SettingEntry(
        key = "backup.restore",
        group = SettingsGroupId.BACKUP,
        section = "MANUAL",
        title = "Restore from backup",
        subtitle = "Merge data from a backup file into the app",
        keywords = listOf("restore", "backup", "import", "merge", "file"),
    ),
    SettingEntry(
        key = "backup.automatic",
        group = SettingsGroupId.BACKUP,
        section = "AUTOMATIC",
        title = "Automatic backups",
        subtitle = "Periodically save a backup to a folder",
        keywords = listOf("automatic", "backup", "schedule", "folder", "periodic"),
    ),
    SettingEntry(
        key = "backup.frequency",
        group = SettingsGroupId.BACKUP,
        section = "AUTOMATIC",
        title = "Backup frequency",
        subtitle = "How often an automatic backup is written.",
        keywords = listOf("frequency", "daily", "weekly", "monthly", "backup"),
    ),
    SettingEntry(
        key = "backup.folder",
        group = SettingsGroupId.BACKUP,
        section = "AUTOMATIC",
        title = "Backup folder",
        subtitle = "Not set",
        keywords = listOf("folder", "backup", "where", "location", "path"),
    ),
    SettingEntry(
        key = "backup.automatic_note",
        group = SettingsGroupId.BACKUP,
        section = "AUTOMATIC",
        title = "About automatic backups",
        subtitle = "A backup is written when the app opens and the chosen interval has " +
            "passed. Nothing is written until a folder has been chosen, and the folder " +
            "is the only place a backup goes.",
        keywords = listOf("automatic", "backup", "schedule", "folder", "interval"),
        note = true,
    ),

    // About
    SettingEntry(
        key = "about.version",
        group = SettingsGroupId.ABOUT,
        section = "THIS APP",
        title = "Version",
        subtitle = "The build installed on this device.",
        keywords = listOf("build", "release", "update", "number"),
    ),
    SettingEntry(
        key = "about.licence",
        group = SettingsGroupId.ABOUT,
        section = "THIS APP",
        title = "Licence",
        subtitle = "GPL-3.0.",
        keywords = listOf("gpl", "legal", "open source", "copyright"),
    ),
    SettingEntry(
        key = "about.built_with",
        group = SettingsGroupId.ABOUT,
        section = "THIS APP",
        title = "Built with",
        subtitle = "Media3, Compose and Room.",
        keywords = listOf("libraries", "credits", "compose", "room", "media3"),
    ),
    SettingEntry(
        key = "about.whats_new",
        group = SettingsGroupId.ABOUT,
        section = "THIS APP",
        title = "What's new",
        subtitle = "What changed in this build.",
        keywords = listOf(
            "changelog", "changes", "release", "notes", "new", "what's new", "update",
        ),
    ),
    SettingEntry(
        key = "about.statistics",
        group = SettingsGroupId.ABOUT,
        section = "STATISTICS",
        title = "Statistics",
        subtitle = "How much there is, and how much you have played.",
        keywords = listOf("counts", "plays", "history", "listening", "numbers"),
    ),
    SettingEntry(
        key = "about.insights",
        group = SettingsGroupId.ABOUT,
        section = "STATISTICS",
        title = "Insights",
        subtitle = "Charts of what the library holds and what you have listened to. " +
            "Turn this off to keep the drawer shorter.",
        keywords = listOf(
            "charts", "graphs", "listening", "history", "genres", "artists", "formats",
            "growth", "drawer",
        ),
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
 * the row it found, while an index row knows the page it leads to. They stay apart rather
 * than travelling as bare strings because the page does something different with each — a
 * found row is tinted for a moment, a section is only scrolled to.
 */
sealed interface SettingsReveal {

    /** The catalogue key of a row, which is what a search result carries. */
    data class Row(val key: String) : SettingsReveal

    /** A section heading, which is what an index row carries. */
    data class Section(val heading: String) : SettingsReveal
}

/**
 * The section headings of [group], in the order the page draws them.
 *
 * A section with no name draws no heading, so it is left out here too: this list is what
 * the index and the tests read to know which headings exist.
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
 * decides where headings land and no page has to be described twice. A blank section is
 * the one exception: it is a card that is not labelled, so no heading item is produced.
 */
fun settingsGroupItems(
    group: SettingsGroupId,
    entries: List<SettingEntry> = settingsCatalog,
): List<SettingsGroupItem> {
    val items = mutableListOf<SettingsGroupItem>()
    var current: String? = null
    entries.filter { it.group == group }.forEach { entry ->
        if (entry.section != current) {
            if (entry.section.isNotBlank()) items += SettingsGroupItem.Heading(entry.section)
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
