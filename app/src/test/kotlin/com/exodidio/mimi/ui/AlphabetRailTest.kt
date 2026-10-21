package com.exodidio.mimi.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetRailTest {

    @Test
    fun `each letter points at its first item`() {
        val stops = alphabetStops(listOf("Apple", "Avocado", "Banana"))

        assertEquals(listOf('A', 'B'), stops.map { it.letter })
        assertEquals(listOf(0, 2), stops.map { it.index })
    }

    @Test
    fun `anything that does not start with a letter shares the last stop`() {
        val stops = alphabetStops(listOf("42", "Apple", "Zed"))

        assertEquals(listOf('A', 'Z', '#'), stops.map { it.letter })
        assertEquals(listOf(1, 2, 0), stops.map { it.index })
    }

    @Test
    fun `a lowercase label is filed under its capital`() {
        assertEquals('A', alphabetLetter("apple"))
        assertEquals('#', alphabetLetter("  "))
    }

    @Test
    fun `the active letter is the last stop at or above the viewport`() {
        val stops = alphabetStops(listOf("Apple", "Avocado", "Banana", "Cherry"))

        assertEquals('A', activeAlphabetLetter(stops, 1))
        assertEquals('B', activeAlphabetLetter(stops, 2))
        assertEquals('C', activeAlphabetLetter(stops, 3))
        assertEquals('C', activeAlphabetLetter(stops, 99))
    }

    @Test
    fun `no stops means no active letter`() {
        assertNull(activeAlphabetLetter(emptyList(), 0))
    }

    @Test
    fun `the rail only appears once the list is long enough`() {
        assertFalse(alphabetRailVisible(ALPHABET_RAIL_MIN_ITEMS - 1))
        assertTrue(alphabetRailVisible(ALPHABET_RAIL_MIN_ITEMS))
    }
}
