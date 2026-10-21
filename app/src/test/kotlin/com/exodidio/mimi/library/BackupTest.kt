package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {

    private val backup = BackupData(
        createdAt = 1_700_000_000_000L,
        settings = listOf(
            MetaEntry("setting.theme_mode", "DARK"),
            MetaEntry("setting.artist_separators", "&\n/"),
        ),
        playlists = listOf(
            BackupPlaylist(
                name = "Road trip",
                entries = listOf(
                    BackupEntry("content://media/external/audio/media/1", 0, "one.mp3"),
                    BackupEntry("content://media/external/audio/media/2", 1, "two.mp3"),
                ),
            ),
            BackupPlaylist(
                name = "Quiet",
                entries = listOf(
                    BackupEntry("content://media/external/audio/media/3", 0, "three.mp3"),
                ),
            ),
        ),
        history = listOf(
            BackupHistory("content://media/external/audio/media/1", 3, 1, 1_700_000_000_000L),
        ),
        listenDays = listOf(
            BackupListenDay("content://media/external/audio/media/1", "2024-05-01", 60_000L, 2),
        ),
    )

    @Test
    fun `a backup is read back as it was written`() {
        val file = parseBackup(buildBackup(backup))

        assertTrue(file is BackupFile.Read)
        val read = (file as BackupFile.Read).backup
        assertEquals(backup.createdAt, read.createdAt)
        assertEquals(backup.settings, read.settings)
        assertEquals(backup.playlists, read.playlists)
        assertEquals(backup.history, read.history)
        assertEquals(backup.listenDays, read.listenDays)
    }

    @Test
    fun `a backup says which version it is on its first line`() {
        assertTrue(buildBackup(backup).startsWith("MIMI BACKUP 1\n"))
    }

    @Test
    fun `a setting holding a list keeps the line breaks it was stored with`() {
        val file = parseBackup(buildBackup(backup))

        val read = (file as BackupFile.Read).backup
        assertEquals("&\n/", read.settings.first { it.key == "setting.artist_separators" }.value)
    }

    @Test
    fun `a playlist name with a dash in it is read back whole`() {
        val named = backup.copy(
            playlists = listOf(BackupPlaylist(name = "Long - drive", entries = emptyList()))
        )

        val read = (parseBackup(buildBackup(named)) as BackupFile.Read).backup
        assertEquals("Long - drive", read.playlists.first().name)
    }

    @Test
    fun `a file from a newer version is refused rather than read`() {
        val newer = buildBackup(backup).replaceFirst("MIMI BACKUP 1", "MIMI BACKUP 2")

        val file = parseBackup(newer)

        assertTrue(file is BackupFile.UnsupportedVersion)
        assertEquals(2, (file as BackupFile.UnsupportedVersion).version)
    }

    @Test
    fun `a file that stops in the middle is refused rather than read`() {
        val truncated = buildBackup(backup).substringBefore("HISTORY")

        val file = parseBackup(truncated)

        assertTrue(file is BackupFile.Truncated)
    }

    @Test
    fun `a file that stops before its end is refused`() {
        val truncated = buildBackup(backup).removeSuffix("END\n")

        val file = parseBackup(truncated)

        assertTrue(file is BackupFile.Truncated)
    }

    @Test
    fun `a file that is not a backup is refused`() {
        val file = parseBackup("this is a shopping list\nmilk\n")

        assertTrue(file is BackupFile.Malformed)
    }

    @Test
    fun `an empty file is refused`() {
        assertTrue(parseBackup("") is BackupFile.Malformed)
    }

    @Test
    fun `the report of a restore says what was applied and what was not`() {
        val report = RestoreReport(
            playlistsCreated = 1,
            playlistsMerged = 2,
            songsRestored = 10,
            songsSkipped = 3,
            settingsApplied = 8,
            settingsSkipped = 2,
        )

        assertTrue(report.summary.contains("3 playlists were restored"))
        assertTrue(report.summary.contains("10 songs were added"))
        assertTrue(report.summary.contains("3 songs in the backup are not in this library"))
    }
}
