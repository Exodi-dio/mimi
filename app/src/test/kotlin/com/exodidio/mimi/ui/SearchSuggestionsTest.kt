package com.exodidio.mimi.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSuggestionsTest {

    @Test
    fun `the distance counts the edits that turn one word into another`() {
        assertEquals(0, levenshteinDistance("beatles", "beatles"))
        assertEquals(1, levenshteinDistance("beatles", "beatlesx"))
        assertEquals(1, levenshteinDistance("beatles", "beatles".dropLast(1)))
        assertEquals(1, levenshteinDistance("beatles", "beatlex"))
        assertEquals(3, levenshteinDistance("cat", "dog"))
    }

    @Test
    fun `an empty word is as far from a word as the word is long`() {
        assertEquals(0, levenshteinDistance("", ""))
        assertEquals(4, levenshteinDistance("", "song"))
        assertEquals(4, levenshteinDistance("song", ""))
    }

    @Test
    fun `a slip in a long word is still near it`() {
        val similarity = termSimilarity("yesterday", "yesterdays")

        assertTrue("A slip in a long word should still be near it.", similarity > 0.85)
    }

    @Test
    fun `the distance is measured against the longer of the two words`() {
        assertEquals(1.0, termSimilarity("song", "song"), 0.0001)
        assertEquals(0.75, termSimilarity("song", "sang"), 0.0001)
        assertEquals(0.0, termSimilarity("cat", "dog"), 0.0001)
    }

    @Test
    fun `a blank query offers nothing`() {
        assertTrue(suggestTerms("", listOf("beatles", "yellow submarine")).isEmpty())
        assertTrue(suggestTerms("   ", listOf("beatles")).isEmpty())
    }

    @Test
    fun `a word that is close enough is offered`() {
        val suggestions = suggestTerms("beatles", listOf("Yellow Submarine", "Beatles"))

        assertEquals(listOf("Beatles"), suggestions)
    }

    @Test
    fun `a word that is nothing like the query is not offered`() {
        val suggestions = suggestTerms("qwertyuiop", listOf("Yellow Submarine", "Hey Jude"))

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `a prefix beats a scattered match`() {
        val suggestions = suggestTerms("love", listOf("Lovely Weather Today", "Gold Dust Woman"))

        assertEquals("Lovely Weather Today", suggestions.first())
    }

    @Test
    fun `the shorter of two equally near candidates comes first`() {
        val suggestions = suggestTerms("roxanne", listOf("Roxanne (Rarities)", "Roxanne"))

        assertEquals("Roxanne", suggestions.first())
    }

    @Test
    fun `case is not a difference`() {
        val suggestions = suggestTerms("BEATLES", listOf("beatles", "Yellow Submarine"))

        assertEquals(listOf("beatles"), suggestions)
    }

    @Test
    fun `only a few candidates are offered`() {
        val candidates = listOf("song one", "song two", "song three", "song four", "song five", "song six")

        val suggestions = suggestTerms("song", candidates, limit = 3)

        assertEquals(3, suggestions.size)
    }

    @Test
    fun `the same word twice is offered once`() {
        val suggestions = suggestTerms("song", listOf("Song", "song", "Song "))

        assertEquals(1, suggestions.size)
    }
}
