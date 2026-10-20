package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentSearchesTest {

    @Test
    fun `a new search goes to the front`() {
        assertEquals(listOf("Nina", "Miles"), listOf("Miles").remembering("Nina"))
    }

    @Test
    fun `searching for the same thing again moves it rather than repeating it`() {
        val searches = listOf("Nina", "Miles", "Coltrane")

        assertEquals(listOf("Miles", "Nina", "Coltrane"), searches.remembering("Miles"))
    }

    @Test
    fun `case does not make a different search`() {
        val searches = listOf("Nina")

        assertEquals(listOf("nina"), searches.remembering("nina"))
    }

    @Test
    fun `surrounding space is not part of the search`() {
        assertEquals(listOf("Nina"), emptyList<String>().remembering("  Nina  "))
    }

    @Test
    fun `a blank search is not remembered at all`() {
        val searches = listOf("Nina")

        assertEquals(searches, searches.remembering(""))
        assertEquals(searches, searches.remembering("   "))
    }

    @Test
    fun `only so many are kept, and the oldest is the one that goes`() {
        // Built newest first, the way the list is held: "recent 1" is the newest.
        val full = (1..RECENT_SEARCH_LIMIT).map { "recent $it" }

        val after = full.remembering("newest")

        assertEquals(RECENT_SEARCH_LIMIT, after.size)
        assertEquals("newest", after.first())
        assertTrue("the newest of the old ones should still be there", "recent 1" in after)
        assertTrue("the oldest should have dropped off", "recent $RECENT_SEARCH_LIMIT" !in after)
    }

    @Test
    fun `searches come back in the order they were kept`() {
        val searches = listOf("newest", "middle", "oldest")

        val rows = recentSearchRows(searches)

        assertEquals(searches, recentSearchesFrom(rows))
    }

    @Test
    fun `the order comes from the key, not from the query`() {
        val rows = listOf(
            MetaEntry(RECENT_SEARCH_PREFIX + 2, "oldest"),
            MetaEntry(RECENT_SEARCH_PREFIX + 0, "newest"),
            MetaEntry(RECENT_SEARCH_PREFIX + 1, "middle"),
        )

        assertEquals(listOf("newest", "middle", "oldest"), recentSearchesFrom(rows))
    }

    @Test
    fun `a row that says nothing usable is skipped`() {
        val rows = listOf(
            MetaEntry(RECENT_SEARCH_PREFIX + 0, "kept"),
            MetaEntry(RECENT_SEARCH_PREFIX + 1, "   "),
            MetaEntry("search.something.else", "not a search"),
            MetaEntry(RECENT_SEARCH_PREFIX + "x", "no number"),
        )

        assertEquals(listOf("kept"), recentSearchesFrom(rows))
    }

    @Test
    fun `nothing stored means nothing to offer`() {
        assertTrue(recentSearchesFrom(emptyList()).isEmpty())
    }
}
