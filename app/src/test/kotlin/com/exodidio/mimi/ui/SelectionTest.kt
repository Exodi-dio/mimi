package com.exodidio.mimi.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionTest {

    @Test
    fun `picking a song that is not picked adds it`() {
        assertEquals(setOf(4L), emptySet<Long>().toggling(4L))
        assertEquals(setOf(4L, 9L), setOf(9L).toggling(4L))
    }

    @Test
    fun `picking a song that is already picked takes it back out`() {
        assertEquals(emptySet<Long>(), setOf(7L).toggling(7L))
        assertEquals(setOf(7L), setOf(7L, 9L).toggling(9L))
    }

    @Test
    fun `toggling twice is the same as never picking`() {
        val picked = emptySet<Long>().toggling(3L).toggling(3L)
        assertTrue(picked.isEmpty())
    }

    @Test
    fun `picked songs come back in the order the list shows them`() {
        val shown = listOf(9L, 3L, 4L, 1L)
        val picked = setOf(1L, 4L, 9L)

        assertEquals(listOf(9L, 4L, 1L), shown.pickedBy(picked) { it })
    }

    @Test
    fun `nothing picked means nothing to act on`() {
        val shown = listOf(9L, 3L)
        assertTrue(shown.pickedBy(emptySet()) { it }.isEmpty())
    }

    @Test
    fun `songs that have gone from the list are not carried along`() {
        val shown = listOf(9L, 3L)

        // 5 was picked while a different list was open; it is not in this one.
        assertEquals(listOf(3L), shown.pickedBy(setOf(3L, 5L)) { it })
    }
}
