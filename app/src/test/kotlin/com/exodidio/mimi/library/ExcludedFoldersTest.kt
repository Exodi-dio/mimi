package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcludedFoldersTest {

    @Test
    fun `a folder is excluded by naming it`() {
        assertTrue(isExcludedFolder("Music/Ringtones", listOf("Music/Ringtones")))
    }

    @Test
    fun `everything inside an excluded folder goes with it`() {
        assertTrue(isExcludedFolder("Music/Ringtones/Old", listOf("Music/Ringtones")))
    }

    @Test
    fun `a folder whose name merely starts the same is not excluded`() {
        assertFalse(isExcludedFolder("Music/Ringtone", listOf("Music/Ringtones")))
        assertFalse(isExcludedFolder("Music/Ringtones", listOf("Music/Ringtone")))
    }

    @Test
    fun `a folder that is not named is not excluded`() {
        assertFalse(isExcludedFolder("Music/Albums", listOf("Music/Ringtones")))
    }

    @Test
    fun `nothing excluded means nothing is excluded`() {
        assertFalse(isExcludedFolder("Music/Ringtones", emptyList()))
    }

    @Test
    fun `a song with no folder is never excluded`() {
        assertFalse(isExcludedFolder(null, listOf("Music/Ringtones")))
        assertFalse(isExcludedFolder("   ", listOf("Music/Ringtones")))
    }

    @Test
    fun `case and surrounding slashes do not matter`() {
        assertTrue(isExcludedFolder("/music/ringtones/", listOf("Music/Ringtones")))
        assertTrue(isExcludedFolder("Music/Ringtones", listOf("/MUSIC/ringtones/")))
    }

    @Test
    fun `folders survive being stored and read back`() {
        val folders = listOf("Music/Ringtones", "Download/Podcasts")

        assertEquals(folders, decodeExcludedFolders(encodeExcludedFolders(folders)))
    }

    @Test
    fun `storing tidies what it was given`() {
        val stored = encodeExcludedFolders(
            listOf(" Music/Ringtones/ ", "", "music/ringtones", "Download/Podcasts"),
        )

        assertEquals(listOf("Download/Podcasts", "Music/Ringtones"), decodeExcludedFolders(stored))
    }

    @Test
    fun `nothing stored means nothing excluded`() {
        assertTrue(decodeExcludedFolders(null).isEmpty())
        assertTrue(decodeExcludedFolders("").isEmpty())
    }
}
