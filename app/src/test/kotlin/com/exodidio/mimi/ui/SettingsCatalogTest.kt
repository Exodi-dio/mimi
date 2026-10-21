package com.exodidio.mimi.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCatalogTest {

    @Test
    fun `a blank query finds nothing rather than everything`() {
        assertTrue(searchSettings("").isEmpty())
        assertTrue(searchSettings("   ").isEmpty())
    }

    @Test
    fun `matching ignores case`() {
        val found = searchSettings("BASS").map { it.key }
        assertTrue("playback.equalizer_enabled" in found)
        assertTrue("playback.equalizer_preset" in found)
    }

    @Test
    fun `a keyword finds a row whose title never says the word`() {
        assertEquals(
            listOf("playback.romanize_lyrics"),
            searchSettings("pinyin").map { it.key },
        )
    }

    @Test
    fun `spaces in the query and the field are forgiven`() {
        assertEquals(
            listOf("playback.romanize_lyrics"),
            searchSettings("  read   lyrics ").map { it.key },
        )
        assertEquals(
            listOf("appearance.show_artwork"),
            searchSettings("coverart").map { it.key },
        )
    }

    @Test
    fun `a title match is ranked above a keyword match`() {
        val found = searchSettings("folders").map { it.key }
        assertEquals("library.excluded_folders", found.first())
        assertTrue(found.indexOf("library.watch_for_changes") > 0)
    }

    @Test
    fun `a subtitle is searched as well as a title and keywords`() {
        assertEquals(
            listOf("playback.resume_session"),
            searchSettings("paused where it stopped").map { it.key },
        )
    }

    @Test
    fun `a query that matches nothing returns nothing`() {
        assertTrue(searchSettings("zzzz").isEmpty())
    }

    @Test
    fun `every key is unique`() {
        assertEquals(settingsCatalog.size, settingsCatalog.map { it.key }.distinct().size)
    }

    @Test
    fun `sections on a page are contiguous so one heading covers each run`() {
        SettingsPage.entries.forEach { page ->
            val sections = settingsCatalog.filter { it.page == page }.map { it.section }
            val collapsed = sections.fold(mutableListOf<String>()) { seen, section ->
                if (seen.lastOrNull() != section) seen += section
                seen
            }
            assertEquals(collapsed.size, collapsed.distinct().size)
        }
    }

    @Test
    fun `the index of a row matches where the page list puts it`() {
        settingsCatalog.forEach { entry ->
            val index = settingsItemIndex(entry.page, entry.key)
            assertTrue(index >= 0)
            val item = settingsPageItems(entry.page)[index]
            assertEquals(entry.key, (item as SettingsPageItem.Row).entry.key)
        }
    }

    @Test
    fun `every page opens with a heading and no section has an empty run`() {
        SettingsPage.entries.forEach { page ->
            val items = settingsPageItems(page)
            assertTrue(items.first() is SettingsPageItem.Heading)
            items.forEachIndexed { index, item ->
                if (item is SettingsPageItem.Heading) {
                    assertTrue(items.getOrNull(index + 1) is SettingsPageItem.Row)
                }
            }
        }
    }
}
