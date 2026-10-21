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
            minSongLength = MinSongLength.THIRTY_SECONDS,
            resumeSession = false,
            pauseOnDisconnect = false,
            skipSilence = true,
            showRemainingTime = true,
            themeMode = ThemeMode.DARK,
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
    fun `the switched off default is off`() {
        val defaults = Settings()
        assertTrue(defaults.scanOnLaunch)
        assertTrue(defaults.watchForChanges)
        assertTrue(defaults.resumeSession)
        assertTrue(defaults.pauseOnDisconnect)
        assertFalse(defaults.skipSilence)
        assertFalse(defaults.showRemainingTime)
        assertEquals(MinSongLength.ANY, defaults.minSongLength)
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
    }

    @Test
    fun `a value that no longer makes sense falls back rather than throwing`() {
        val settings = Settings.from(
            listOf(
                MetaEntry("setting.scan_on_launch", "yes please"),
                MetaEntry("setting.min_song_length_ms", "1234"),
                MetaEntry("setting.theme_mode", "NEON"),
                MetaEntry("setting.skip_silence", "1"),
            )
        )

        assertTrue(settings.scanOnLaunch)
        assertEquals(MinSongLength.ANY, settings.minSongLength)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.skipSilence)
    }

    @Test
    fun `a stored length that is not one of the choices shows everything`() {
        assertEquals(MinSongLength.ANY, MinSongLength.of(0L))
        assertEquals(MinSongLength.ANY, MinSongLength.of(7L))
        assertEquals(MinSongLength.ONE_MINUTE, MinSongLength.of(60_000L))
    }

    @Test
    fun `settings live under their own prefix`() {
        assertTrue(Settings.rows(Settings()).all { it.key.startsWith(Settings.PREFIX) })
    }
}
