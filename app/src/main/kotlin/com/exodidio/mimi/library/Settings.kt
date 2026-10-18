package com.exodidio.mimi.library

/**
 * Light, dark, or whatever the phone is doing.
 *
 * The mode is remembered even while the app wears no proper skin, because the phone's own
 * setting is not always what someone wants to read a long list of songs against.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * The shortest song the library will show.
 *
 * MediaStore happily indexes ringtones, voicemail blips and two second voice notes, and
 * they clutter a library badly. The default shows everything, so nothing is hidden until
 * it is asked for.
 */
enum class MinSongLength(val label: String, val millis: Long) {
    ANY("Any length", 0L),
    FIVE_SECONDS("Over 5 seconds", 5_000L),
    FIFTEEN_SECONDS("Over 15 seconds", 15_000L),
    THIRTY_SECONDS("Over 30 seconds", 30_000L),
    ONE_MINUTE("Over a minute", 60_000L);

    companion object {
        fun of(millis: Long): MinSongLength = entries.firstOrNull { it.millis == millis } ?: ANY
    }
}

/**
 * How the songs list is ordered.
 *
 * The library always reads back in one fixed order and is sorted after the fact, so
 * changing this costs one pass over a list already in memory rather than a different
 * query — and it keeps one place responsible for what "sorted" means.
 */
enum class SongSort(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    YEAR("Year"),
    DATE_ADDED("Recently added"),
    DURATION("Duration"),
}

/**
 * Everything the listener has chosen.
 *
 * Every field has a default that matches how the app behaved before there was a screen to
 * change it, so an upgrade never changes behaviour on its own.
 */
data class Settings(
    /** Look for new files when the app is opened. */
    val scanOnLaunch: Boolean = true,
    /** Rescan shortly after audio files appear or disappear on the device. */
    val watchForChanges: Boolean = true,
    val minSongLength: MinSongLength = MinSongLength.ANY,
    /** Put the last song back on screen, paused where it was. */
    val resumeSession: Boolean = true,
    /** Stop when headphones are unplugged, rather than playing out of the speaker. */
    val pauseOnDisconnect: Boolean = true,
    /** Step over silent passages in a track. */
    val skipSilence: Boolean = false,
    /** Count down the time left instead of counting up the time played. */
    val showRemainingTime: Boolean = false,
    /** Draw the cover art beside each song in a list. */
    val showArtwork: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** The field the songs list is ordered by. */
    val songSort: SongSort = SongSort.TITLE,
    /** Whether that order runs backwards, which is "newest first" for dates. */
    val sortDescending: Boolean = false,
) {
    companion object {
        /**
         * Every key is under one prefix so the settings can be read with a single query
         * that cannot be disturbed by anything else stored in the same table — the playback
         * session is rewritten every few seconds, and the settings must not re-emit with it.
         */
        const val PREFIX = "setting."

        private const val SCAN_ON_LAUNCH = "setting.scan_on_launch"
        private const val WATCH_FOR_CHANGES = "setting.watch_for_changes"
        private const val MIN_SONG_LENGTH = "setting.min_song_length_ms"
        private const val RESUME_SESSION = "setting.resume_session"
        private const val PAUSE_ON_DISCONNECT = "setting.pause_on_disconnect"
        private const val SKIP_SILENCE = "setting.skip_silence"
        private const val SHOW_REMAINING_TIME = "setting.show_remaining_time"
        private const val SHOW_ARTWORK = "setting.show_artwork"
        private const val THEME_MODE = "setting.theme_mode"
        private const val SONG_SORT = "setting.song_sort"
        private const val SORT_DESCENDING = "setting.sort_descending"

        /**
         * Reads a set of stored rows into settings.
         *
         * Anything missing, unreadable or no longer understood falls back to its default,
         * so a damaged row costs one switch rather than the whole screen. Pure, so the
         * whole of this can be tested without a device.
         */
        fun from(entries: List<MetaEntry>): Settings {
            val stored = entries.associate { it.key to it.value }
            val defaults = Settings()
            return Settings(
                scanOnLaunch = stored.boolean(SCAN_ON_LAUNCH, defaults.scanOnLaunch),
                watchForChanges = stored.boolean(WATCH_FOR_CHANGES, defaults.watchForChanges),
                minSongLength = MinSongLength.of(
                    stored[MIN_SONG_LENGTH]?.toLongOrNull() ?: defaults.minSongLength.millis
                ),
                resumeSession = stored.boolean(RESUME_SESSION, defaults.resumeSession),
                pauseOnDisconnect = stored.boolean(PAUSE_ON_DISCONNECT, defaults.pauseOnDisconnect),
                skipSilence = stored.boolean(SKIP_SILENCE, defaults.skipSilence),
                showRemainingTime = stored.boolean(SHOW_REMAINING_TIME, defaults.showRemainingTime),
                showArtwork = stored.boolean(SHOW_ARTWORK, defaults.showArtwork),
                themeMode = stored[THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: defaults.themeMode,
                songSort = stored[SONG_SORT]
                    ?.let { name -> SongSort.entries.firstOrNull { it.name == name } }
                    ?: defaults.songSort,
                sortDescending = stored.boolean(SORT_DESCENDING, defaults.sortDescending),
            )
        }

        /** The rows that store [settings], as they would be written. */
        fun rows(settings: Settings): List<MetaEntry> = listOf(
            MetaEntry(SCAN_ON_LAUNCH, if (settings.scanOnLaunch) "1" else "0"),
            MetaEntry(WATCH_FOR_CHANGES, if (settings.watchForChanges) "1" else "0"),
            MetaEntry(MIN_SONG_LENGTH, settings.minSongLength.millis.toString()),
            MetaEntry(RESUME_SESSION, if (settings.resumeSession) "1" else "0"),
            MetaEntry(PAUSE_ON_DISCONNECT, if (settings.pauseOnDisconnect) "1" else "0"),
            MetaEntry(SKIP_SILENCE, if (settings.skipSilence) "1" else "0"),
            MetaEntry(SHOW_REMAINING_TIME, if (settings.showRemainingTime) "1" else "0"),
            MetaEntry(SHOW_ARTWORK, if (settings.showArtwork) "1" else "0"),
            MetaEntry(THEME_MODE, settings.themeMode.name),
            MetaEntry(SONG_SORT, settings.songSort.name),
            MetaEntry(SORT_DESCENDING, if (settings.sortDescending) "1" else "0"),
        )

        private fun Map<String, String>.boolean(key: String, default: Boolean): Boolean =
            when (this[key]) {
                "1" -> true
                "0" -> false
                else -> default
            }
    }
}
