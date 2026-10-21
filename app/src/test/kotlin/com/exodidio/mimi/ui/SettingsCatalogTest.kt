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
    fun `every group on the drawer has a page with something on it`() {
        SettingsGroupId.entries.forEach { group ->
            assertTrue(
                "${group.name} has no rows, so its drawer row would open an empty page.",
                settingsCatalog.any { it.group == group },
            )
            assertTrue(group.title.isNotBlank())
            assertTrue(group.subtitle.isNotBlank())
        }
    }

    @Test
    fun `sections on a group are contiguous so one heading covers each run`() {
        SettingsGroupId.entries.forEach { group ->
            val sections = settingsCatalog.filter { it.group == group }.map { it.section }
            val collapsed = sections.fold(mutableListOf<String>()) { seen, section ->
                if (seen.lastOrNull() != section) seen += section
                seen
            }
            assertEquals(
                "${group.name} repeats a section heading, which would draw it twice.",
                collapsed.size,
                collapsed.distinct().size,
            )
        }
    }

    @Test
    fun `the index of a row matches where the group list puts it`() {
        settingsCatalog.forEach { entry ->
            val index = settingsItemIndex(entry.group, entry.key)
            assertTrue(
                "${entry.key} is not in ${entry.group.name}'s list.",
                index >= 0,
            )
            val item = settingsGroupItems(entry.group)[index]
            assertEquals(entry.key, (item as SettingsGroupItem.Row).entry.key)
        }
    }

    @Test
    fun `the drawer rows are the headings the pages draw`() {
        SettingsGroupId.entries.forEach { group ->
            val headings = settingsGroupItems(group)
                .filterIsInstance<SettingsGroupItem.Heading>()
                .map { it.section }
            assertEquals("${group.name}'s drawer rows do not match its headings.", headings, settingsSections(group))

            headings.forEach { section ->
                val index = settingsSectionIndex(group, section)
                assertTrue("$section has no heading on ${group.name}'s page.", index >= 0)
                val item = settingsGroupItems(group)[index]
                assertEquals(section, (item as SettingsGroupItem.Heading).section)
            }
        }
    }

    @Test
    fun `every group opens with a heading and no section has an empty run`() {
        SettingsGroupId.entries.forEach { group ->
            val items = settingsGroupItems(group)
            assertTrue(
                "${group.name} does not open with a heading.",
                items.first() is SettingsGroupItem.Heading,
            )
            items.forEachIndexed { index, item ->
                if (item is SettingsGroupItem.Heading) {
                    assertTrue(
                        "${group.name} has a heading with no rows under it.",
                        items.getOrNull(index + 1) is SettingsGroupItem.Row,
                    )
                }
            }
        }
    }
}
