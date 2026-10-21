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
        assertEquals(
            listOf("playback.equalizer"),
            searchSettings("BASS").map { it.key },
        )
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
            listOf("customization.show_artwork"),
            searchSettings("coverart").map { it.key },
        )
    }

    @Test
    fun `a title match is ranked above a keyword match`() {
        assertEquals("library.excluded_folders", searchSettings("blacklist").first().key)
        assertEquals("playback.keep_shuffle", searchSettings("shuffle").first().key)
        assertEquals("playback.crossfade", searchSettings("crossfade").first().key)
        assertEquals("playback.gapless", searchSettings("gapless").first().key)
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
    fun `every row can be found by its own title`() {
        settingsCatalog.forEach { entry ->
            assertEquals(
                "${entry.key} is not the first result for its own title.",
                entry.key,
                searchSettings(entry.title).firstOrNull()?.key,
            )
        }
    }

    @Test
    fun `search reaches the pages that were added`() {
        val expectations = mapOf(
            "blacklist" to "library.excluded_folders",
            "duration" to "library.duration_range",
            "crossfade" to "playback.crossfade",
            "gapless" to "playback.gapless",
            "focal" to "playback.duck_on_focus_loss",
            "headset" to "playback.auto_play",
            "band" to "playback.eq_band_count",
            "replaygain" to "playback.replaygain_mode",
            "metadata" to "images.auto_download_metadata",
            "blur album" to "images.blur_album_cover",
            "tabs" to "customization.bottom_tabs",
            "see all" to "customization.see_all",
            "volume slider" to "now_playing.volume_slider",
            "recently played" to "playlists.recently_played_range",
            "restore" to "backup.restore",
        )

        expectations.forEach { (query, key) ->
            assertEquals(
                "Searching \u201c$query\u201d does not reach $key.",
                key,
                searchSettings(query).firstOrNull()?.key,
            )
        }
    }

    @Test
    fun `every catalogue row names the page it lives on`() {
        settingsCatalog.forEach { entry ->
            assertTrue("${entry.key} has no title.", entry.title.isNotBlank())
            assertTrue("${entry.key} has no subtitle.", entry.subtitle.isNotBlank())
            val page = settingsGroupItems(entry.group)
                .filterIsInstance<SettingsGroupItem.Row>()
                .firstOrNull { it.entry.key == entry.key }
            assertTrue("${entry.key} is not on ${entry.group.name}'s page.", page != null)
        }
    }

    @Test
    fun `every group has a page with something on it`() {
        SettingsGroupId.entries.forEach { group ->
            assertTrue(
                "${group.name} has no rows, so its index row would open an empty page.",
                settingsCatalog.any { it.group == group },
            )
            assertTrue(group.title.isNotBlank())
            assertTrue(group.subtitle.isNotBlank())
        }
    }

    @Test
    fun `each page opens with the row the reference names`() {
        val expected = mapOf(
            SettingsGroupId.LIBRARY to "Multiple artists support",
            SettingsGroupId.AUDIO to "Fade music on play / pause",
            SettingsGroupId.IMAGES to "Cache images",
            SettingsGroupId.CUSTOMIZATION to "Manage bottom tabs",
            SettingsGroupId.NOW_PLAYING to "Now Playing Theme",
            SettingsGroupId.PLAYLISTS to "Playlist export path",
            SettingsGroupId.BACKUP to "What a backup holds",
            SettingsGroupId.ABOUT to "Version",
        )

        expected.forEach { (group, title) ->
            val first = settingsCatalog.first { it.group == group }
            assertEquals("${group.name} opens on the wrong row.", title, first.title)
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
    fun `a page with a blank section opens on its card without a heading`() {
        listOf(
            SettingsGroupId.NOW_PLAYING,
            SettingsGroupId.PLAYLISTS,
            SettingsGroupId.BACKUP,
        ).forEach { group ->
            assertTrue(
                "${group.name} should start with an unlabelled card.",
                settingsGroupItems(group).first() is SettingsGroupItem.Row,
            )
        }

        assertTrue("Now playing should draw no heading at all.", settingsSections(SettingsGroupId.NOW_PLAYING).isEmpty())
        assertTrue("Playlists should draw no heading at all.", settingsSections(SettingsGroupId.PLAYLISTS).isEmpty())
    }

    @Test
    fun `every heading is followed by at least one row and no page ends on a heading`() {
        SettingsGroupId.entries.forEach { group ->
            val items = settingsGroupItems(group)
            items.forEachIndexed { index, item ->
                if (item is SettingsGroupItem.Heading) {
                    assertTrue(
                        "${group.name} has a heading with no rows under it.",
                        items.getOrNull(index + 1) is SettingsGroupItem.Row,
                    )
                }
            }
            assertTrue(
                "${group.name} ends on a heading.",
                items.lastOrNull() is SettingsGroupItem.Row,
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
    fun `the section index matches the heading it names`() {
        SettingsGroupId.entries.forEach { group ->
            val headings = settingsGroupItems(group)
                .filterIsInstance<SettingsGroupItem.Heading>()
                .map { it.section }
            assertEquals(
                "${group.name}'s section list disagrees with its items.",
                headings,
                settingsSections(group),
            )

            headings.forEach { section ->
                val index = settingsSectionIndex(group, section)
                assertTrue("$section has no heading on ${group.name}'s page.", index >= 0)
                val item = settingsGroupItems(group)[index]
                assertEquals(section, (item as SettingsGroupItem.Heading).section)
            }
        }
    }
}
