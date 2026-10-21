package com.exodidio.mimi.library

/**
 * Light, dark, or whatever the phone is doing.
 *
 * The mode is remembered even while the app wears no proper skin, because the phone's own
 * setting is not always what someone wants to read a long list of songs against.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
 *
 * The fields are grouped the way the settings pages group them, because the catalogue and
 * this class are read side by side: a row that exists on a page and a value that exists
 * here are meant to be found in the same order.
 */
data class Settings(
    /** Look for new files when the app is opened. */
    val scanOnLaunch: Boolean = true,
    /** Rescan shortly after audio files appear or disappear on the device. */
    val watchForChanges: Boolean = true,
    /**
     * The lower end of the length filter, or zero for no lower bound.
     *
     * This replaces the old five-option "shortest song" setting. A stored value from that
     * setting is read into this field once, so nobody loses the filter they had set.
     */
    val minDurationMs: Long = 0L,
    /** The upper end of the length filter, or zero for no upper bound. */
    val maxDurationMs: Long = 0L,
    /** Whether an artist tag naming several people is split into several artists. */
    val multipleArtists: Boolean = true,
    /** The marks that split an artist tag. Kept apart from the genre list on purpose. */
    val artistSeparators: List<String> = DEFAULT_CREDIT_SEPARATORS,
    /**
     * Whether "go to artist" opens the first name on a credit.
     *
     * On, which is how this behaved before it was a switch: a tag reading `A & B` opens A
     * rather than a page for a name nothing is filed under.
     */
    val alwaysFirstArtist: Boolean = true,
    /** Whether a genre tag naming several genres is split into several genres. */
    val multipleGenres: Boolean = true,
    /** The marks that split a genre tag. */
    val genreSeparators: List<String> = DEFAULT_CREDIT_SEPARATORS,
    /** Put the last song back on screen, paused where it was. */
    val resumeSession: Boolean = true,
    /** Stop when headphones are unplugged, rather than playing out of the speaker. */
    val pauseOnDisconnect: Boolean = true,
    /** Step over silent passages in a track. */
    val skipSilence: Boolean = false,
    /** Ramp the volume down over the last stretch before a sleep timer stops. */
    val fadeOutSleepTimer: Boolean = false,
    /** Count down the time left instead of counting up the time played. */
    val showRemainingTime: Boolean = false,
    /** How long the volume takes to fade in and out of a pause, or zero for no fade. */
    val fadeOnPlayPauseMs: Int = 0,
    /** How long two tracks overlap, or zero for no overlap. */
    val crossfadeMs: Int = 0,
    /** Ask the player to join tracks with no silence between them. */
    val gaplessPlayback: Boolean = true,
    /** Whether shuffle survives starting a new list of songs. */
    val keepShuffleMode: Boolean = true,
    /** The decoder in use. Only one engine exists, so this is a name rather than a choice. */
    val playbackEngine: String = SYSTEM_ENGINE,
    /** How many bands the equaliser is asked for. The device decides what it can give. */
    val eqBandCount: Int = 10,
    /** Which ReplayGain field the music is normalised by. */
    val replayGainMode: String = REPLAYGAIN_NONE,
    /** Preamp in decibels, applied only when ReplayGain is on. */
    val replayGainPreampDb: Float = 0f,
    /** Lower the music when another app takes audio focus, rather than stopping. */
    val lowerVolumeOnFocusLoss: Boolean = true,
    /** Start playing when a wired or bluetooth headset is connected. */
    val autoPlay: Boolean = false,
    /** Draw the cover art beside each song in a list. */
    val showArtwork: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** The field the songs list is ordered by. */
    val songSort: SongSort = SongSort.TITLE,
    /** Whether that order runs backwards, which is "newest first" for dates. */
    val sortDescending: Boolean = false,
    /** The field the albums grid is ordered by. */
    val albumSort: AlbumSort = AlbumSort.NAME,
    /** The field the artists list is ordered by. */
    val artistSort: ArtistSort = ArtistSort.NAME,
    /** Whether the equaliser shapes the sound. Off, so nothing is altered unasked. */
    val equalizerEnabled: Boolean = false,
    /** Which curve is in force, by key. A key that means nothing here falls back to flat. */
    val equalizerPresetKey: String = EQUALIZER_FLAT_KEY,
    /** The curve itself, which stops matching its preset as soon as a band is dragged. */
    val equalizerGains: List<Float> = EQUALIZER_FLAT_CURVE,
    /**
     * How much weight the bass boost adds, as a percentage.
     *
     * Nothing is added at zero, which is where it starts: the boost is asked for
     * explicitly rather than being part of the equaliser's curve.
     */
    val bassBoostStrength: Int = 0,
    /** Curves the listener named and keeps. */
    val equalizerProfiles: List<EqualizerProfile> = emptyList(),
    /** Write Chinese and Korean words out in latin letters beneath the characters. */
    val romanizeLyrics: Boolean = false,
    /** Folders whose songs are left out of the library entirely. */
    val excludedFolders: List<String> = emptyList(),
    /** Artist names hidden from every list, search and count. */
    val hiddenArtists: List<String> = emptyList(),
    /** Album names hidden from every list, search and count. */
    val hiddenAlbums: List<String> = emptyList(),
    /**
     * How lists inside a browse screen are ordered.
     *
     * Separate from [songSort] because an album, an artist and a playlist each arrive in a
     * meaningful order of their own. It defaults to leaving that order alone, so turning
     * the setting on is a choice rather than something an upgrade does behind the
     * listener's back.
     */
    val detailSort: DetailSort = DetailSort.AS_LISTED,
    /**
     * Whether the drawer offers the Insights screen.
     *
     * On by default because it is a view of the library rather than an experiment. Turning
     * it off hides the row and shortens the drawer; the setting itself stays where it can
     * be turned back on.
     */
    val showInsights: Boolean = true,
    /** Keep decoded artwork in memory so a list does not re-read the same picture. */
    val cacheImages: Boolean = true,
    /** Fall back to the device's album art index, which is quick but sometimes wrong. */
    val mediaStoreArt: Boolean = true,
    /** When artist images and biographies would be fetched, if that were built. */
    val autoDownloadMetadata: String = METADATA_ALWAYS,
    /** Whether the media notification carries the cover. */
    val lockscreenArt: Boolean = true,
    /** Whether the cover is blurred on the lock screen. */
    val blurAlbumCover: Boolean = false,
    /** The order the bottom navigation draws its destinations in. */
    val bottomTabOrder: List<String> = DefaultBottomTabOrder,
    /** Destinations left out of the bottom navigation. */
    val hiddenBottomTabs: List<String> = emptyList(),
    /** The order the home screen draws its shelves in. */
    val homeRowOrder: List<String> = DefaultHomeRowOrder,
    /** Shelves left off the home screen. */
    val hiddenHomeRows: List<String> = emptyList(),
    /** Whether a details screen offers See all for a long list of songs. */
    val showSeeAll: Boolean = true,
    /** The look of the now playing screen. Only the default exists in this build. */
    val nowPlayingTheme: String = DEFAULT_NOW_PLAYING_THEME,
    /** Show format, bitrate and sample rate under the scrubber. */
    val showSongInfo: Boolean = false,
    /** Show a volume slider in the main player. */
    val showVolumeSlider: Boolean = false,
    /** Put previous and next in the mini player rather than next alone. */
    val extraControls: Boolean = false,
    /** The folder exported playlists would be written to, which nothing has chosen yet. */
    val playlistExportPath: String = "",
    /** How far back the automatic "Last added" list reaches. */
    val lastAddedRange: RecentRange = RecentRange.PAST_THREE_MONTHS,
    /** How far back the automatic "Recently played" list reaches. */
    val recentlyPlayedRange: RecentRange = RecentRange.PAST_THREE_MONTHS,
    /** Whether a backup would be written on a schedule. */
    val automaticBackups: Boolean = false,
    /** How often an automatic backup would be written. */
    val backupFrequency: String = BACKUP_DAILY,
    /** The folder an automatic backup would be written to. */
    val backupFolder: String = "",
) {
    /** Every curve on offer: the ones that came with the app, then the listener's own. */
    val equalizerChoices: List<EqualizerProfile> get() = EqualizerPresets + equalizerProfiles

    /**
     * What an artist tag is split at, or nothing when the support is switched off.
     *
     * Everything that groups by artist asks this rather than the stored list, so turning
     * the switch off is one decision made in one place instead of a condition at every
     * call site.
     */
    val artistSplit: List<String> get() = if (multipleArtists) artistSeparators else emptyList()

    /** What a genre tag is split at, or nothing when the support is switched off. */
    val genreSplit: List<String> get() = if (multipleGenres) genreSeparators else emptyList()

    /**
     * The curve in force, as a profile.
     *
     * The engine wants one thing to apply rather than a key to look up, and the gains are
     * held apart from the key so an edited curve still knows which preset it came from --
     * which is what lets the screen offer to put it back.
     */
    val equalizerProfile: EqualizerProfile
        get() {
            val chosen = equalizerChoices.firstOrNull { it.key == equalizerPresetKey }
            return EqualizerProfile(
                key = equalizerPresetKey,
                name = chosen?.name ?: EqualizerPresets.first().name,
                gainsDb = equalizerGains,
                isBuiltIn = chosen?.isBuiltIn ?: true,
            )
        }

    companion object {
        /**
         * The separators a library starts with.
         *
         * A comma is deliberately absent. `Earth, Wind & Fire` is one band, and a comma
         * default would file it as three artists, which is the sort of damage that makes
         * a library look broken on the first launch. The list is editable precisely because
         * no default can be right for every library: the conservative one ships, and the
         * setting is the correction.
         */
        val DEFAULT_CREDIT_SEPARATORS: List<String> =
            listOf(";", "/", "&", "feat.", "ft.", "featuring")

        /** The only engine this build has, named as the row names it. */
        const val SYSTEM_ENGINE = "System (MediaPlayer)"

        /** ReplayGain switched off, which is the only mode that does nothing at all. */
        const val REPLAYGAIN_NONE = "None"

        /** ReplayGain by the value the file carries for the track on its own. */
        const val REPLAYGAIN_TRACK = "Track"

        /** ReplayGain by the value the file carries for the album. */
        const val REPLAYGAIN_ALBUM = "Album"

        /** The default look of the player, and the only one that exists. */
        const val DEFAULT_NOW_PLAYING_THEME = "Default"

        const val METADATA_ALWAYS = "Always"

        const val BACKUP_DAILY = "Daily"

        /**
         * Every key is under one prefix so the settings can be read with a single query
         * that cannot be disturbed by anything else stored in the same table — the playback
         * session is rewritten every few seconds, and the settings must not re-emit with it.
         */
        const val PREFIX = "setting."

        private const val SCAN_ON_LAUNCH = "setting.scan_on_launch"
        private const val WATCH_FOR_CHANGES = "setting.watch_for_changes"
        private const val MIN_DURATION = "setting.min_duration_ms"
        private const val MAX_DURATION = "setting.max_duration_ms"
        private const val MULTIPLE_ARTISTS = "setting.multiple_artists"
        private const val ARTIST_SEPARATORS = "setting.artist_separators"
        private const val ALWAYS_FIRST_ARTIST = "setting.always_first_artist"
        private const val MULTIPLE_GENRES = "setting.multiple_genres"
        private const val GENRE_SEPARATORS = "setting.genre_separators"
        private const val RESUME_SESSION = "setting.resume_session"
        private const val PAUSE_ON_DISCONNECT = "setting.pause_on_disconnect"
        private const val SKIP_SILENCE = "setting.skip_silence"
        private const val FADE_OUT_SLEEP_TIMER = "setting.fade_out_sleep_timer"
        private const val SHOW_REMAINING_TIME = "setting.show_remaining_time"
        private const val FADE_ON_PAUSE = "setting.fade_on_pause_ms"
        private const val CROSSFADE = "setting.crossfade_ms"
        private const val GAPLESS = "setting.gapless"
        private const val KEEP_SHUFFLE = "setting.keep_shuffle"
        private const val PLAYBACK_ENGINE = "setting.playback_engine"
        private const val EQ_BAND_COUNT = "setting.eq_band_count"
        private const val REPLAYGAIN_MODE = "setting.replaygain_mode"
        private const val REPLAYGAIN_PREAMP = "setting.replaygain_preamp_db"
        private const val LOWER_VOLUME_ON_FOCUS_LOSS = "setting.lower_volume_on_focus_loss"
        private const val AUTO_PLAY = "setting.auto_play"
        private const val SHOW_ARTWORK = "setting.show_artwork"
        private const val THEME_MODE = "setting.theme_mode"
        private const val SONG_SORT = "setting.song_sort"
        private const val SORT_DESCENDING = "setting.sort_descending"
        private const val ALBUM_SORT = "setting.album_sort"
        private const val ARTIST_SORT = "setting.artist_sort"
        private const val EQUALIZER_ENABLED = "setting.equalizer_enabled"
        private const val EQUALIZER_PRESET = "setting.equalizer_preset"
        private const val EQUALIZER_GAINS = "setting.equalizer_gains"
        private const val BASS_BOOST_STRENGTH = "setting.bass_boost_strength"
        private const val EQUALIZER_PROFILES = "setting.equalizer_profiles"
        private const val ROMANIZE_LYRICS = "setting.romanize_lyrics"
        private const val EXCLUDED_FOLDERS = "setting.excluded_folders"
        private const val HIDDEN_ARTISTS = "setting.hidden_artists"
        private const val HIDDEN_ALBUMS = "setting.hidden_albums"
        private const val DETAIL_SORT = "setting.detail_sort"
        private const val SHOW_INSIGHTS = "setting.show_insights"
        private const val CACHE_IMAGES = "setting.cache_images"
        private const val MEDIASTORE_ART = "setting.mediastore_art"
        private const val AUTO_DOWNLOAD_METADATA = "setting.auto_download_metadata"
        private const val LOCKSCREEN_ART = "setting.lockscreen_art"
        private const val BLUR_ALBUM_COVER = "setting.blur_album_cover"
        private const val BOTTOM_TABS = "setting.bottom_tabs"
        private const val HIDDEN_BOTTOM_TABS = "setting.hidden_bottom_tabs"
        private const val HOME_ROWS = "setting.home_rows"
        private const val HIDDEN_HOME_ROWS = "setting.hidden_home_rows"
        private const val SHOW_SEE_ALL = "setting.show_see_all"
        private const val NOW_PLAYING_THEME = "setting.now_playing_theme"
        private const val SHOW_SONG_INFO = "setting.show_song_info"
        private const val SHOW_VOLUME_SLIDER = "setting.show_volume_slider"
        private const val EXTRA_CONTROLS = "setting.extra_controls"
        private const val PLAYLIST_EXPORT_PATH = "setting.playlist_export_path"
        private const val LAST_ADDED_RANGE = "setting.last_added_range"
        private const val RECENTLY_PLAYED_RANGE = "setting.recently_played_range"
        private const val AUTOMATIC_BACKUPS = "setting.automatic_backups"
        private const val BACKUP_FREQUENCY = "setting.backup_frequency"
        private const val BACKUP_FOLDER = "setting.backup_folder"

        /**
         * The two keys the settings used to be stored under.
         *
         * Read once when the new key is absent, so a listener who had a shortest-song bound
         * or a separator list keeps it. Nothing writes them any more; they are only ever a
         * source for the first read after the upgrade.
         */
        private const val LEGACY_MIN_SONG_LENGTH = "setting.min_song_length_ms"
        private const val LEGACY_CREDIT_SEPARATORS = "setting.credit_separators"

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
            // An empty stored list is a real choice ("split nothing"), so it is kept rather
            // than falling back to the defaults; only an absent row is a missing answer.
            val legacySeparators = stored[LEGACY_CREDIT_SEPARATORS]?.let(::decodeStoredList)

            return Settings(
                scanOnLaunch = stored.boolean(SCAN_ON_LAUNCH, defaults.scanOnLaunch),
                watchForChanges = stored.boolean(WATCH_FOR_CHANGES, defaults.watchForChanges),
                // The old setting held the lower bound as milliseconds, so it can be read
                // straight across. Anything absent on both keys keeps the default.
                minDurationMs = (
                    stored[MIN_DURATION]?.toLongOrNull()
                        ?: stored[LEGACY_MIN_SONG_LENGTH]?.toLongOrNull()
                        ?: defaults.minDurationMs
                    ).coerceAtLeast(0L),
                maxDurationMs = (stored[MAX_DURATION]?.toLongOrNull() ?: defaults.maxDurationMs)
                    .coerceAtLeast(0L),
                multipleArtists = stored.boolean(MULTIPLE_ARTISTS, defaults.multipleArtists),
                artistSeparators = stored[ARTIST_SEPARATORS]?.let(::decodeStoredList)
                    ?: legacySeparators
                    ?: defaults.artistSeparators,
                alwaysFirstArtist = stored.boolean(ALWAYS_FIRST_ARTIST, defaults.alwaysFirstArtist),
                multipleGenres = stored.boolean(MULTIPLE_GENRES, defaults.multipleGenres),
                genreSeparators = stored[GENRE_SEPARATORS]?.let(::decodeStoredList)
                    ?: legacySeparators
                    ?: defaults.genreSeparators,
                resumeSession = stored.boolean(RESUME_SESSION, defaults.resumeSession),
                pauseOnDisconnect = stored.boolean(PAUSE_ON_DISCONNECT, defaults.pauseOnDisconnect),
                skipSilence = stored.boolean(SKIP_SILENCE, defaults.skipSilence),
                fadeOutSleepTimer = stored.boolean(FADE_OUT_SLEEP_TIMER, defaults.fadeOutSleepTimer),
                showRemainingTime = stored.boolean(SHOW_REMAINING_TIME, defaults.showRemainingTime),
                fadeOnPlayPauseMs = (stored[FADE_ON_PAUSE]?.toIntOrNull() ?: defaults.fadeOnPlayPauseMs)
                    .coerceAtLeast(0),
                crossfadeMs = (stored[CROSSFADE]?.toIntOrNull() ?: defaults.crossfadeMs)
                    .coerceAtLeast(0),
                gaplessPlayback = stored.boolean(GAPLESS, defaults.gaplessPlayback),
                keepShuffleMode = stored.boolean(KEEP_SHUFFLE, defaults.keepShuffleMode),
                playbackEngine = stored[PLAYBACK_ENGINE]?.takeIf { it.isNotBlank() }
                    ?: defaults.playbackEngine,
                eqBandCount = (stored[EQ_BAND_COUNT]?.toIntOrNull() ?: defaults.eqBandCount)
                    .coerceAtLeast(1),
                replayGainMode = stored[REPLAYGAIN_MODE]?.takeIf { it.isNotBlank() }
                    ?: defaults.replayGainMode,
                replayGainPreampDb = stored[REPLAYGAIN_PREAMP]?.toFloatOrNull()
                    ?: defaults.replayGainPreampDb,
                lowerVolumeOnFocusLoss = stored.boolean(
                    LOWER_VOLUME_ON_FOCUS_LOSS,
                    defaults.lowerVolumeOnFocusLoss,
                ),
                autoPlay = stored.boolean(AUTO_PLAY, defaults.autoPlay),
                showArtwork = stored.boolean(SHOW_ARTWORK, defaults.showArtwork),
                themeMode = stored[THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: defaults.themeMode,
                songSort = stored[SONG_SORT]
                    ?.let { name -> SongSort.entries.firstOrNull { it.name == name } }
                    ?: defaults.songSort,
                sortDescending = stored.boolean(SORT_DESCENDING, defaults.sortDescending),
                albumSort = stored[ALBUM_SORT]
                    ?.let { name -> AlbumSort.entries.firstOrNull { it.name == name } }
                    ?: defaults.albumSort,
                artistSort = stored[ARTIST_SORT]
                    ?.let { name -> ArtistSort.entries.firstOrNull { it.name == name } }
                    ?: defaults.artistSort,
                equalizerEnabled = stored.boolean(EQUALIZER_ENABLED, defaults.equalizerEnabled),
                equalizerPresetKey = stored[EQUALIZER_PRESET] ?: defaults.equalizerPresetKey,
                equalizerGains = decodeCurve(stored[EQUALIZER_GAINS]) ?: defaults.equalizerGains,
                bassBoostStrength = (stored[BASS_BOOST_STRENGTH]?.toIntOrNull()
                    ?: defaults.bassBoostStrength)
                    .coerceIn(0, 100),
                equalizerProfiles = decodeProfiles(stored[EQUALIZER_PROFILES]),
                romanizeLyrics = stored.boolean(ROMANIZE_LYRICS, defaults.romanizeLyrics),
                excludedFolders = decodeExcludedFolders(stored[EXCLUDED_FOLDERS]),
                hiddenArtists = decodeHiddenNames(stored[HIDDEN_ARTISTS]),
                hiddenAlbums = decodeHiddenNames(stored[HIDDEN_ALBUMS]),
                detailSort = stored[DETAIL_SORT]
                    ?.let { name -> DetailSort.entries.firstOrNull { it.name == name } }
                    ?: defaults.detailSort,
                showInsights = stored.boolean(SHOW_INSIGHTS, defaults.showInsights),
                cacheImages = stored.boolean(CACHE_IMAGES, defaults.cacheImages),
                mediaStoreArt = stored.boolean(MEDIASTORE_ART, defaults.mediaStoreArt),
                autoDownloadMetadata = stored[AUTO_DOWNLOAD_METADATA]?.takeIf { it.isNotBlank() }
                    ?: defaults.autoDownloadMetadata,
                lockscreenArt = stored.boolean(LOCKSCREEN_ART, defaults.lockscreenArt),
                blurAlbumCover = stored.boolean(BLUR_ALBUM_COVER, defaults.blurAlbumCover),
                bottomTabOrder = stored[BOTTOM_TABS]?.let(::decodeStoredList)
                    ?.let { normalisedOrder(it, DefaultBottomTabOrder) }
                    ?: defaults.bottomTabOrder,
                hiddenBottomTabs = decodeStoredList(stored[HIDDEN_BOTTOM_TABS]),
                homeRowOrder = stored[HOME_ROWS]?.let(::decodeStoredList)
                    ?.let { normalisedOrder(it, DefaultHomeRowOrder) }
                    ?: defaults.homeRowOrder,
                hiddenHomeRows = decodeStoredList(stored[HIDDEN_HOME_ROWS]),
                showSeeAll = stored.boolean(SHOW_SEE_ALL, defaults.showSeeAll),
                nowPlayingTheme = stored[NOW_PLAYING_THEME]?.takeIf { it.isNotBlank() }
                    ?: defaults.nowPlayingTheme,
                showSongInfo = stored.boolean(SHOW_SONG_INFO, defaults.showSongInfo),
                showVolumeSlider = stored.boolean(SHOW_VOLUME_SLIDER, defaults.showVolumeSlider),
                extraControls = stored.boolean(EXTRA_CONTROLS, defaults.extraControls),
                playlistExportPath = stored[PLAYLIST_EXPORT_PATH].orEmpty(),
                lastAddedRange = stored[LAST_ADDED_RANGE]
                    ?.let { name -> RecentRange.entries.firstOrNull { it.name == name } }
                    ?: defaults.lastAddedRange,
                recentlyPlayedRange = stored[RECENTLY_PLAYED_RANGE]
                    ?.let { name -> RecentRange.entries.firstOrNull { it.name == name } }
                    ?: defaults.recentlyPlayedRange,
                automaticBackups = stored.boolean(AUTOMATIC_BACKUPS, defaults.automaticBackups),
                backupFrequency = stored[BACKUP_FREQUENCY]?.takeIf { it.isNotBlank() }
                    ?: defaults.backupFrequency,
                backupFolder = stored[BACKUP_FOLDER].orEmpty(),
            )
        }

        /** The rows that store [settings], as they would be written. */
        fun rows(settings: Settings): List<MetaEntry> = listOf(
            MetaEntry(SCAN_ON_LAUNCH, if (settings.scanOnLaunch) "1" else "0"),
            MetaEntry(WATCH_FOR_CHANGES, if (settings.watchForChanges) "1" else "0"),
            MetaEntry(MIN_DURATION, settings.minDurationMs.toString()),
            MetaEntry(MAX_DURATION, settings.maxDurationMs.toString()),
            MetaEntry(MULTIPLE_ARTISTS, if (settings.multipleArtists) "1" else "0"),
            MetaEntry(ARTIST_SEPARATORS, encodeCreditSeparators(settings.artistSeparators)),
            MetaEntry(ALWAYS_FIRST_ARTIST, if (settings.alwaysFirstArtist) "1" else "0"),
            MetaEntry(MULTIPLE_GENRES, if (settings.multipleGenres) "1" else "0"),
            MetaEntry(GENRE_SEPARATORS, encodeCreditSeparators(settings.genreSeparators)),
            MetaEntry(RESUME_SESSION, if (settings.resumeSession) "1" else "0"),
            MetaEntry(PAUSE_ON_DISCONNECT, if (settings.pauseOnDisconnect) "1" else "0"),
            MetaEntry(SKIP_SILENCE, if (settings.skipSilence) "1" else "0"),
            MetaEntry(FADE_OUT_SLEEP_TIMER, if (settings.fadeOutSleepTimer) "1" else "0"),
            MetaEntry(SHOW_REMAINING_TIME, if (settings.showRemainingTime) "1" else "0"),
            MetaEntry(FADE_ON_PAUSE, settings.fadeOnPlayPauseMs.toString()),
            MetaEntry(CROSSFADE, settings.crossfadeMs.toString()),
            MetaEntry(GAPLESS, if (settings.gaplessPlayback) "1" else "0"),
            MetaEntry(KEEP_SHUFFLE, if (settings.keepShuffleMode) "1" else "0"),
            MetaEntry(PLAYBACK_ENGINE, settings.playbackEngine),
            MetaEntry(EQ_BAND_COUNT, settings.eqBandCount.toString()),
            MetaEntry(REPLAYGAIN_MODE, settings.replayGainMode),
            MetaEntry(REPLAYGAIN_PREAMP, settings.replayGainPreampDb.toString()),
            MetaEntry(
                LOWER_VOLUME_ON_FOCUS_LOSS,
                if (settings.lowerVolumeOnFocusLoss) "1" else "0",
            ),
            MetaEntry(AUTO_PLAY, if (settings.autoPlay) "1" else "0"),
            MetaEntry(SHOW_ARTWORK, if (settings.showArtwork) "1" else "0"),
            MetaEntry(THEME_MODE, settings.themeMode.name),
            MetaEntry(SONG_SORT, settings.songSort.name),
            MetaEntry(SORT_DESCENDING, if (settings.sortDescending) "1" else "0"),
            MetaEntry(ALBUM_SORT, settings.albumSort.name),
            MetaEntry(ARTIST_SORT, settings.artistSort.name),
            MetaEntry(EQUALIZER_ENABLED, if (settings.equalizerEnabled) "1" else "0"),
            MetaEntry(EQUALIZER_PRESET, settings.equalizerPresetKey),
            MetaEntry(EQUALIZER_GAINS, encodeCurve(settings.equalizerGains)),
            MetaEntry(BASS_BOOST_STRENGTH, settings.bassBoostStrength.toString()),
            MetaEntry(EQUALIZER_PROFILES, encodeProfiles(settings.equalizerProfiles)),
            MetaEntry(ROMANIZE_LYRICS, if (settings.romanizeLyrics) "1" else "0"),
            MetaEntry(EXCLUDED_FOLDERS, encodeExcludedFolders(settings.excludedFolders)),
            MetaEntry(HIDDEN_ARTISTS, encodeHiddenNames(settings.hiddenArtists)),
            MetaEntry(HIDDEN_ALBUMS, encodeHiddenNames(settings.hiddenAlbums)),
            MetaEntry(DETAIL_SORT, settings.detailSort.name),
            MetaEntry(SHOW_INSIGHTS, if (settings.showInsights) "1" else "0"),
            MetaEntry(CACHE_IMAGES, if (settings.cacheImages) "1" else "0"),
            MetaEntry(MEDIASTORE_ART, if (settings.mediaStoreArt) "1" else "0"),
            MetaEntry(AUTO_DOWNLOAD_METADATA, settings.autoDownloadMetadata),
            MetaEntry(LOCKSCREEN_ART, if (settings.lockscreenArt) "1" else "0"),
            MetaEntry(BLUR_ALBUM_COVER, if (settings.blurAlbumCover) "1" else "0"),
            MetaEntry(BOTTOM_TABS, encodeStoredList(settings.bottomTabOrder)),
            MetaEntry(HIDDEN_BOTTOM_TABS, encodeStoredList(settings.hiddenBottomTabs)),
            MetaEntry(HOME_ROWS, encodeStoredList(settings.homeRowOrder)),
            MetaEntry(HIDDEN_HOME_ROWS, encodeStoredList(settings.hiddenHomeRows)),
            MetaEntry(SHOW_SEE_ALL, if (settings.showSeeAll) "1" else "0"),
            MetaEntry(NOW_PLAYING_THEME, settings.nowPlayingTheme),
            MetaEntry(SHOW_SONG_INFO, if (settings.showSongInfo) "1" else "0"),
            MetaEntry(SHOW_VOLUME_SLIDER, if (settings.showVolumeSlider) "1" else "0"),
            MetaEntry(EXTRA_CONTROLS, if (settings.extraControls) "1" else "0"),
            MetaEntry(PLAYLIST_EXPORT_PATH, settings.playlistExportPath),
            MetaEntry(LAST_ADDED_RANGE, settings.lastAddedRange.name),
            MetaEntry(RECENTLY_PLAYED_RANGE, settings.recentlyPlayedRange.name),
            MetaEntry(AUTOMATIC_BACKUPS, if (settings.automaticBackups) "1" else "0"),
            MetaEntry(BACKUP_FREQUENCY, settings.backupFrequency),
            MetaEntry(BACKUP_FOLDER, settings.backupFolder),
        )

        private fun Map<String, String>.boolean(key: String, default: Boolean): Boolean =
            when (this[key]) {
                "1" -> true
                "0" -> false
                else -> default
            }
    }
}
