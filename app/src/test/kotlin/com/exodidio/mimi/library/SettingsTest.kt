package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    @Test
    fun `an empty store reads as the defaults`() {
        assertEquals(Settings(), Settings.from(emptyList()))
    }

    @Test
    fun `settings survive being written and read back`() {
        val chosen = Settings(
            scanOnLaunch = false,
            watchForChanges = false,
            minDurationMs = 60_000L,
            maxDurationMs = 3_600_000L,
            multipleArtists = false,
            artistSeparators = listOf("&", "feat."),
            alwaysFirstArtist = false,
            multipleGenres = false,
            genreSeparators = listOf("/"),
            resumeSession = false,
            pauseOnDisconnect = false,
            skipSilence = true,
            fadeOutSleepTimer = true,
            showRemainingTime = true,
            fadeOnPlayPauseMs = 500,
            crossfadeMs = 6_000,
            gaplessPlayback = false,
            keepShuffleMode = false,
            eqBandCount = 15,
            replayGainMode = "Track",
            replayGainPreampDb = -3.5f,
            lowerVolumeOnFocusLoss = false,
            autoPlay = true,
            themeMode = ThemeMode.DARK,
            cacheImages = false,
            mediaStoreArt = false,
            autoDownloadMetadata = "Never",
            lockscreenArt = false,
            blurAlbumCover = true,
            bottomTabOrder = listOf("SEARCH", "HOME", "LIBRARY", "LOVED"),
            hiddenBottomTabs = listOf("LOVED"),
            homeRowOrder = listOf("LOVED", "RECENTLY_ADDED", "MOST_PLAYED", "RECENTLY_PLAYED"),
            hiddenHomeRows = listOf("MOST_PLAYED"),
            showSeeAll = false,
            nowPlayingTheme = "Default",
            showSongInfo = true,
            showVolumeSlider = true,
            extraControls = true,
            playlistExportPath = "Music/Playlists",
            lastAddedRange = RecentRange.PAST_MONTH,
            recentlyPlayedRange = RecentRange.ALL_TIME,
            automaticBackups = true,
            backupFrequency = "Weekly",
            backupFolder = "Backups",
        )

        assertEquals(chosen, Settings.from(Settings.rows(chosen)))
    }

    @Test
    fun `the defaults survive being written and read back`() {
        val defaults = Settings()
        assertEquals(defaults, Settings.from(Settings.rows(defaults)))
    }

    @Test
    fun `hidden names and detail order survive being written and read back`() {
        val chosen = Settings(
            hiddenArtists = listOf("Ada", "Bea"),
            hiddenAlbums = listOf("Blue"),
            detailSort = DetailSort.DURATION,
        )

        assertEquals(chosen, Settings.from(Settings.rows(chosen)))
    }

    @Test
    fun `an unknown detail order falls back to as listed`() {
        val settings = Settings.from(listOf(MetaEntry("setting.detail_sort", "SIDEWAYS")))

        assertEquals(DetailSort.AS_LISTED, settings.detailSort)
    }

    @Test
    fun `the old shortest song bound is read into the bottom of the range`() {
        val settings = Settings.from(
            listOf(MetaEntry("setting.min_song_length_ms", "30000"))
        )

        assertEquals(30_000L, settings.minDurationMs)
        assertEquals(0L, settings.maxDurationMs)
    }

    @Test
    fun `a stored duration wins over the old shortest song bound`() {
        val settings = Settings.from(
            listOf(
                MetaEntry("setting.min_song_length_ms", "30000"),
                MetaEntry("setting.min_duration_ms", "60000"),
            )
        )

        assertEquals(60_000L, settings.minDurationMs)
    }

    @Test
    fun `the old credit separators are read into both lists`() {
        val settings = Settings.from(
            listOf(MetaEntry("setting.credit_separators", "&\n/"))
        )

        assertEquals(listOf("&", "/"), settings.artistSeparators)
        assertEquals(listOf("&", "/"), settings.genreSeparators)
    }

    @Test
    fun `turning both split lists off survives the read`() {
        val settings = Settings.from(
            listOf(MetaEntry("setting.credit_separators", ""))
        )

        assertTrue(settings.artistSeparators.isEmpty())
        assertTrue(settings.genreSeparators.isEmpty())
    }

    @Test
    fun `a genre list can differ from the artist list`() {
        val stored = listOf(
            MetaEntry("setting.credit_separators", "&"),
            MetaEntry("setting.genre_separators", "/"),
        )

        val settings = Settings.from(stored)

        assertEquals(listOf("&"), settings.artistSeparators)
        assertEquals(listOf("/"), settings.genreSeparators)
    }

    @Test
    fun `switching multiple artists off empties the split without losing the stored list`() {
        val settings = Settings(multipleArtists = false, artistSeparators = listOf("&"))

        assertTrue(settings.artistSplit.isEmpty())
        assertEquals(listOf("&"), settings.artistSeparators)
    }

    @Test
    fun `switching multiple genres off empties the split without losing the stored list`() {
        val settings = Settings(multipleGenres = false, genreSeparators = listOf("/"))

        assertTrue(settings.genreSplit.isEmpty())
        assertEquals(listOf("/"), settings.genreSeparators)
    }

    @Test
    fun `the switched off default is off`() {
        val defaults = Settings()
        assertTrue(defaults.scanOnLaunch)
        assertTrue(defaults.watchForChanges)
        assertTrue(defaults.resumeSession)
        assertTrue(defaults.pauseOnDisconnect)
        assertFalse(defaults.skipSilence)
        assertFalse(defaults.showRemainingTime)
        assertEquals(0L, defaults.minDurationMs)
        assertEquals(0L, defaults.maxDurationMs)
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertTrue(defaults.multipleArtists)
        assertTrue(defaults.multipleGenres)
        assertTrue(defaults.alwaysFirstArtist)
        assertTrue(defaults.keepShuffleMode)
        assertTrue(defaults.gaplessPlayback)
        assertTrue(defaults.lowerVolumeOnFocusLoss)
        assertFalse(defaults.autoPlay)
    }

    @Test
    fun `a value that no longer makes sense falls back rather than throwing`() {
        val settings = Settings.from(
            listOf(
                MetaEntry("setting.scan_on_launch", "yes please"),
                MetaEntry("setting.min_duration_ms", "1234"),
                MetaEntry("setting.theme_mode", "NEON"),
                MetaEntry("setting.skip_silence", "1"),
                MetaEntry("setting.last_added_range", "FORTNIGHT"),
                MetaEntry("setting.eq_band_count", "0"),
            )
        )

        assertTrue(settings.scanOnLaunch)
        assertEquals(1_234L, settings.minDurationMs)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.skipSilence)
        assertEquals(RecentRange.PAST_THREE_MONTHS, settings.lastAddedRange)
        assertEquals(1, settings.eqBandCount)
    }

    @Test
    fun `a negative duration is treated as no bound rather than a bound below zero`() {
        val settings = Settings.from(
            listOf(
                MetaEntry("setting.min_duration_ms", "-5"),
                MetaEntry("setting.max_duration_ms", "-1"),
            )
        )

        assertEquals(0L, settings.minDurationMs)
        assertEquals(0L, settings.maxDurationMs)
    }

    @Test
    fun `a stored order gains a destination added later and drops an unknown one`() {
        val settings = Settings.from(
            listOf(MetaEntry("setting.bottom_tabs", "SEARCH\nGONE"))
        )

        assertEquals(
            listOf("SEARCH", "HOME", "LIBRARY", "LOVED"),
            settings.bottomTabOrder,
        )
    }

    @Test
    fun `hiding insights is remembered and defaults to showing them`() {
        assertTrue(Settings().showInsights)

        val hidden = Settings(showInsights = false)
        assertEquals(hidden, Settings.from(Settings.rows(hidden)))
    }

    @Test
    fun `settings live under their own prefix`() {
        assertTrue(Settings.rows(Settings()).all { it.key.startsWith(Settings.PREFIX) })
    }
}
