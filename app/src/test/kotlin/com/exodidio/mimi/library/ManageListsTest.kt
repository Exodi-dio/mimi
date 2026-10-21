package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageListsTest {

    @Test
    fun `the default orders hold every destination`() {
        assertEquals(
            listOf("HOME", "LIBRARY", "LOVED", "SEARCH"),
            DefaultBottomTabOrder,
        )
        assertEquals(
            listOf("RECENTLY_ADDED", "RECENTLY_PLAYED", "MOST_PLAYED", "LOVED"),
            DefaultHomeRowOrder,
        )
    }

    @Test
    fun `a stored order drops an unknown name and appends a missing one`() {
        assertEquals(
            listOf("SEARCH", "HOME", "LIBRARY", "LOVED"),
            normalisedOrder(listOf("SEARCH", "GONE"), DefaultBottomTabOrder),
        )
    }

    @Test
    fun `an empty stored order falls back to the default order`() {
        assertEquals(DefaultHomeRowOrder, normalisedOrder(emptyList(), DefaultHomeRowOrder))
    }

    @Test
    fun `an entry moves one place at a time`() {
        val order = listOf("HOME", "LIBRARY", "LOVED", "SEARCH")

        assertEquals(listOf("LIBRARY", "HOME", "LOVED", "SEARCH"), order.movingUp("LIBRARY"))
        assertEquals(listOf("HOME", "LOVED", "LIBRARY", "SEARCH"), order.movingDown("LIBRARY"))
    }

    @Test
    fun `moving past either end leaves the order alone`() {
        val order = listOf("HOME", "LIBRARY", "LOVED", "SEARCH")

        assertEquals(order, order.movingUp("HOME"))
        assertEquals(order, order.movingDown("SEARCH"))
        assertEquals(order, order.movingUp("MISSING"))
    }

    @Test
    fun `the buttons know when they would do nothing`() {
        val order = listOf("HOME", "LIBRARY", "LOVED", "SEARCH")

        assertFalse(order.canMoveUp("HOME"))
        assertTrue(order.canMoveUp("LIBRARY"))
        assertTrue(order.canMoveDown("LIBRARY"))
        assertFalse(order.canMoveDown("SEARCH"))
        assertFalse(order.canMoveUp("MISSING"))
    }

    @Test
    fun `the bottom bar shows the stored order without the hidden tabs`() {
        val visible = bottomTabsIn(
            order = listOf("SEARCH", "HOME", "LIBRARY", "LOVED"),
            hidden = listOf("LIBRARY"),
        )

        assertEquals(
            listOf(BottomTabId.SEARCH, BottomTabId.HOME, BottomTabId.LOVED),
            visible,
        )
    }

    @Test
    fun `hiding every tab still leaves one to navigate with`() {
        val visible = bottomTabsIn(
            order = listOf("HOME", "LIBRARY", "LOVED", "SEARCH"),
            hidden = BottomTabId.entries.map { it.name },
        )

        assertEquals(listOf(BottomTabId.HOME), visible)
    }

    @Test
    fun `the first tab in the chosen order is the one kept when all are hidden`() {
        val visible = bottomTabsIn(
            order = listOf("SEARCH", "HOME", "LIBRARY", "LOVED"),
            hidden = BottomTabId.entries.map { it.name },
        )

        assertEquals(listOf(BottomTabId.SEARCH), visible)
    }

    @Test
    fun `an unknown stored name cannot hide a real tab`() {
        val visible = bottomTabsIn(
            order = listOf("SEARCH", "GONE"),
            hidden = listOf("GONE"),
        )

        assertEquals(
            listOf(
                BottomTabId.SEARCH,
                BottomTabId.HOME,
                BottomTabId.LIBRARY,
                BottomTabId.LOVED,
            ),
            visible,
        )
    }

    @Test
    fun `home rows follow the stored order and skip the hidden ones`() {
        val rows = homeRowsIn(
            order = listOf("LOVED", "RECENTLY_ADDED", "MOST_PLAYED", "RECENTLY_PLAYED"),
            hidden = listOf("MOST_PLAYED"),
        )

        assertEquals(
            listOf(HomeRowId.LOVED, HomeRowId.RECENTLY_ADDED, HomeRowId.RECENTLY_PLAYED),
            rows,
        )
    }

    @Test
    fun `hiding every home row leaves an empty home rather than an exception`() {
        val rows = homeRowsIn(
            order = DefaultHomeRowOrder,
            hidden = HomeRowId.entries.map { it.name },
        )

        assertTrue(rows.isEmpty())
    }
}
