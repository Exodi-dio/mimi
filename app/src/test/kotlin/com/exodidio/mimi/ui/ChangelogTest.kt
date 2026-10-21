package com.exodidio.mimi.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogTest {

    @Test
    fun `every section has a heading and something under it`() {
        changelogSections.forEach { section ->
            assertTrue("${section.label} has no heading.", section.label.isNotBlank())
            assertTrue("${section.label} has no lines.", section.entries.isNotEmpty())
        }
    }

    @Test
    fun `the sheet names the three kinds of change`() {
        assertEquals(
            listOf("New", "Tweaks", "Fixed"),
            changelogSections.map { it.label },
        )
    }

    @Test
    fun `every line is a plain sentence`() {
        changelogSections.forEach { section ->
            section.entries.forEach { entry ->
                assertTrue("\"$entry\" is empty.", entry.isNotBlank())
                assertTrue("\"$entry\" does not end in a full stop.", entry.endsWith("."))
                assertFalse("\"$entry\" is shouting.", entry.contains("!"))
            }
        }
    }

    @Test
    fun `a closed section keeps its first lines and an open one keeps them all`() {
        val entries = listOf("one", "two", "three", "four")

        assertEquals(listOf("one", "two", "three"), visibleChangelogEntries(entries, expanded = false))
        assertEquals(entries, visibleChangelogEntries(entries, expanded = true))
    }

    @Test
    fun `the pill is only offered when there is something behind it`() {
        assertTrue(changelogHasMore(listOf("one", "two", "three", "four")))
        assertFalse(changelogHasMore(listOf("one", "two", "three")))
    }

    @Test
    fun `the pill says what pressing it will do`() {
        assertEquals("Show more", changelogToggleLabel(expanded = false))
        assertEquals("Show less ^", changelogToggleLabel(expanded = true))
    }
}
