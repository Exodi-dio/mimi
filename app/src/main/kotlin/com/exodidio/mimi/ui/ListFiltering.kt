package com.exodidio.mimi.ui

import com.exodidio.mimi.library.Track

/**
 * The songs in [this] containing [query] in their title, artist or album.
 *
 * This is a filter over what is already on screen, so it deliberately searches the same
 * three fields as the global search while staying inside the current list. A blank query
 * returns the list unchanged rather than nothing: closing the field should put the list
 * back exactly as it was.
 *
 * Pure, so the matching can be tested without a device.
 */
fun List<Track>.matchingListFilter(query: String): List<Track> {
    val needle = query.trim()
    if (needle.isEmpty()) return this

    return filter { track ->
        track.title.contains(needle, ignoreCase = true) ||
            track.artist?.contains(needle, ignoreCase = true) == true ||
            track.album?.contains(needle, ignoreCase = true) == true
    }
}

/** "3 of 39 songs match", which is the line under an in-list search field. */
fun listFilterLabel(matched: Int, total: Int): String =
    when {
        matched == total -> songCountLabel(total)
        matched == 1 -> "1 of $total songs matches"
        else -> "$matched of $total songs match"
    }
