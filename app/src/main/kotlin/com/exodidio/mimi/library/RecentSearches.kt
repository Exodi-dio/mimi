package com.exodidio.mimi.library

/**
 * What has been searched for lately.
 *
 * Kept as ordinary rows in the table everything else durable lives in, one row per search,
 * numbered by how recent it is. A table of its own would have needed a schema version to
 * go with it, and a schema version on this app means the listener's loved songs and
 * playlists are destroyed to make room for a list of words.
 */

/** Where the searches are kept. Numbered from zero, most recent first. */
const val RECENT_SEARCH_PREFIX = "search.recent."

/** How many are kept before the oldest drops off. */
const val RECENT_SEARCH_LIMIT = 8

/**
 * [this] with [text] put in front of it, and the oldest dropped if that makes too many.
 *
 * Searching for the same thing twice moves it back to the front rather than storing it
 * again, and it counts as the same thing regardless of case — somebody who types "Nina"
 * after "nina" is not looking for two different things.
 *
 * Absorbed into the list rather than beside it, so nothing downstream has to decide what
 * to do with a blank search.
 */
fun List<String>.remembering(text: String): List<String> {
    val cleaned = text.trim()
    if (cleaned.isEmpty()) return this

    return (listOf(cleaned) + filterNot { it.equals(cleaned, ignoreCase = true) })
        .take(RECENT_SEARCH_LIMIT)
}

/** The rows that store [searches], ready to be written. */
fun recentSearchRows(searches: List<String>): List<MetaEntry> =
    searches.take(RECENT_SEARCH_LIMIT).mapIndexed { index, text ->
        MetaEntry(RECENT_SEARCH_PREFIX + index, text)
    }

/**
 * The searches stored in [rows], most recent first.
 *
 * Rows are ordered by the number in their key rather than by the order the query returned
 * them in, because a row's place in the list is the whole point and the query gives no
 * guarantee about it. A row whose key says nothing usable is skipped.
 */
fun recentSearchesFrom(rows: List<MetaEntry>): List<String> =
    rows.mapNotNull { row ->
        val index = row.key.removePrefix(RECENT_SEARCH_PREFIX).toIntOrNull()
        if (index == null || row.value.isBlank()) null else index to row.value
    }
        .sortedBy { it.first }
        .map { it.second }
        .take(RECENT_SEARCH_LIMIT)
