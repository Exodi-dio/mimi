package com.exodidio.mimi.library

/**
 * The lists the listener is allowed to rearrange.
 *
 * Both screens that manage a list do the same thing to it — move an entry up or down, or
 * hide it — so the ordering lives here once and both screens only decide what to draw. The
 * stored form is a plain list of names in the order they are shown, with a separate list of
 * the ones that are hidden, which is why adding a destination later does not need a new
 * setting: an entry missing from the stored order is simply appended in its default place.
 */

/** The destinations the bottom navigation can show. */
enum class BottomTabId(val label: String) {
    HOME("Home"),
    LIBRARY("Library"),
    LOVED("Loved"),
    SEARCH("Search"),
}

/** The shelves the home screen can show. */
enum class HomeRowId(val title: String) {
    RECENTLY_ADDED("Recently added"),
    RECENTLY_PLAYED("Recently played"),
    MOST_PLAYED("Most played"),
    LOVED("Loved"),
}

/** Every tab, in the order a library that has never rearranged them sees. */
val DefaultBottomTabOrder: List<String> = BottomTabId.entries.map { it.name }

/** Every home shelf, in its default order. */
val DefaultHomeRowOrder: List<String> = HomeRowId.entries.map { it.name }

/**
 * [order] with anything unknown dropped and anything missing appended.
 *
 * A stored order that predates a new destination would otherwise hide it for ever, which is
 * the one way a list of names can lose a feature. Saved orders are always written through
 * this, so what is stored and what is shown agree.
 */
fun normalisedOrder(order: List<String>, known: List<String>): List<String> =
    order.map { it.trim() }.filter { it in known }.distinct() +
        known.filterNot { it in order }

/** The tabs to show, in the stored order, with the hidden ones left out. */
fun bottomTabsIn(order: List<String>, hidden: List<String>): List<BottomTabId> {
    val known = DefaultBottomTabOrder
    val visible = normalisedOrder(order, known)
        .filterNot { it in hidden }
        .mapNotNull { name -> BottomTabId.entries.firstOrNull { it.name == name } }
    // A navigation bar with nothing in it is a dead end rather than a preference, so the
    // first tab in the chosen order is always kept.
    return visible.ifEmpty {
        listOfNotNull(BottomTabId.entries.firstOrNull { it.name == normalisedOrder(order, known).first() })
    }
}

/** The home shelves to show, in the stored order, with the hidden ones left out. */
fun homeRowsIn(order: List<String>, hidden: List<String>): List<HomeRowId> =
    normalisedOrder(order, DefaultHomeRowOrder)
        .filterNot { it in hidden }
        .mapNotNull { name -> HomeRowId.entries.firstOrNull { it.name == name } }

/** [order] with the entry named [key] moved one place towards the front. */
fun List<String>.movingUp(key: String): List<String> = moveBy(key, -1)

/** [order] with the entry named [key] moved one place towards the back. */
fun List<String>.movingDown(key: String): List<String> = moveBy(key, 1)

/** False when [key] is already first, which is what greys out the up button. */
fun List<String>.canMoveUp(key: String): Boolean = indexOf(key) > 0

/** False when [key] is already last, which is what greys out the down button. */
fun List<String>.canMoveDown(key: String): Boolean = indexOf(key) in 0 until (size - 1)

/**
 * Moves one entry by [delta] places, keeping the rest in order.
 *
 * Index arithmetic rather than a swap, because a swap of two entries is only the same thing
 * as a move when the two are adjacent — and the buttons here will always be adjacent, but
 * the helper should still mean what it says.
 */
private fun List<String>.moveBy(key: String, delta: Int): List<String> {
    val from = indexOf(key)
    if (from < 0) return this
    val to = (from + delta).coerceIn(0, lastIndex)
    if (to == from) return this

    val next = toMutableList()
    next.removeAt(from)
    next.add(to, key)
    return next
}
