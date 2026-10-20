package com.exodidio.mimi.ui

/**
 * Picking songs out of a list, as plain values.
 *
 * Held as a set of song ids rather than as a list of songs, so a selection survives the
 * list underneath it being rebuilt — the songs list re-sorts whenever the sort setting
 * changes, and a selection made by hand should not be lost because a row moved.
 *
 * These are pure so the rules can be checked without a device.
 */

/** Adds [id] to the picked set, or takes it away when it is already there. */
fun Set<Long>.toggling(id: Long): Set<Long> =
    if (id in this) this - id else this + id

/**
 * The songs in [this] that are picked, in the order this list shows them.
 *
 * Order matters: adding a selection to a playlist should add it in the order the listener
 * can see, not in the arbitrary order a set happens to iterate in.
 */
fun <T> List<T>.pickedBy(picked: Set<Long>, id: (T) -> Long): List<T> =
    if (picked.isEmpty()) emptyList() else filter { id(it) in picked }
