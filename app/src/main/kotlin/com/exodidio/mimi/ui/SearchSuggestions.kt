package com.exodidio.mimi.ui

import com.exodidio.mimi.library.Track
import kotlin.math.max

/**
 * How many single-character edits turn one word into another.
 *
 * The plain edit distance: the fewest changes of inserting, deleting or replacing one
 * character that make the two words the same. It is the measure that answers the
 * question a mistyped word actually asks — "how far did I get?".
 *
 * Pure, so the ranking that depends on it can be tested without a device.
 */
fun levenshteinDistance(first: String, second: String): Int {
    if (first == second) return 0
    if (first.isEmpty()) return second.length
    if (second.isEmpty()) return first.length

    // Two rows of the table, kept rather than a whole one: the answer only ever needs
    // the row above and the row being filled in.
    var previous = IntArray(second.length + 1) { it }
    var current = IntArray(second.length + 1)

    for (i in 1..first.length) {
        current[0] = i
        for (j in 1..second.length) {
            val substitution = previous[j - 1] +
                if (first[i - 1] == second[j - 1]) 0 else 1
            val insertion = current[j - 1] + 1
            val deletion = previous[j] + 1
            current[j] = minOf(substitution, insertion, deletion)
        }
        val swap = previous
        previous = current
        current = swap
    }

    return previous[second.length]
}

/**
 * How close a candidate is to what was typed, from nothing to one.
 *
 * The distance is measured against the longer of the two, so a slip in a long title
 * costs almost nothing while a slip in a short word costs a lot. Without that, a title
 * of thirty characters could never be near a three-character query, which is exactly the
 * case a near match is for.
 *
 * Pure, so the ranking that depends on it can be tested without a device.
 */
fun termSimilarity(query: String, candidate: String): Double {
    val needle = query.trim().lowercase()
    val haystack = candidate.trim().lowercase()
    if (needle.isEmpty() || haystack.isEmpty()) return 0.0
    if (needle == haystack) return 1.0

    val longest = max(needle.length, haystack.length)
    val distance = levenshteinDistance(needle, haystack)
    return 1.0 - distance.toDouble() / longest.toDouble()
}

/**
 * How well a candidate answers the query, where a larger score is a better answer.
 *
 * A candidate that starts with the query is what somebody meant when they typed it, so
 * it comes first however short it is. One that holds the query somewhere in the middle
 * comes next, and everything else is ranked by how near it is. Without this order a
 * long title that merely contains the query would beat the name that was typed.
 *
 * Null means the candidate is too far away to be worth offering.
 */
private fun termScore(needle: String, candidate: String): Double? {
    val haystack = candidate.trim().lowercase()
    if (haystack.isEmpty()) return null
    if (haystack.startsWith(needle)) return 1.0
    if (haystack.contains(needle)) return 0.95

    // A candidate whose length is nowhere near the query's cannot be within the floor,
    // and measuring it would be the most expensive part of the pass over the library.
    if (haystack.length * 2 < needle.length || haystack.length > needle.length * 2) {
        return null
    }

    val similarity = termSimilarity(needle, haystack)
    return similarity.takeIf { it >= NEAR_MATCH_FLOOR }
}

/**
 * How near a candidate has to be before it is worth offering.
 *
 * Half of the longer word away is the line: a word that is half wrong is a different
 * word, and offering it would be guessing rather than helping.
 */
private const val NEAR_MATCH_FLOOR = 0.5

/**
 * The terms worth offering when a search finds nothing.
 *
 * The candidates are ranked by how near they are to the query and the best few are
 * returned. A blank query offers nothing: there is no mistyped word to be near to, and
 * a list of everything in the library is not an answer to anything.
 *
 * Pure, so the ranking can be tested without a device.
 */
fun suggestTerms(
    query: String,
    candidates: List<String>,
    limit: Int = 5,
): List<String> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty() || limit <= 0) return emptyList()

    // The same name can be in the library twice, spelled differently. Offering it twice
    // would look like two answers to one question.
    val seen = mutableSetOf<String>()

    return candidates
        .asSequence()
        .filter { it.isNotBlank() }
        .filter { seen.add(it.trim().lowercase()) }
        .mapNotNull { candidate ->
            termScore(needle, candidate)?.let { candidate to it }
        }
        .sortedWith(
            compareByDescending<Pair<String, Double>> { it.second }
                // The shorter of two equally near candidates is the one that was meant:
                // "Roxanne" is likelier than "Roxanne (Rarities)".
                .thenBy { it.first.length }
                .thenBy { it.first },
        )
        .take(limit)
        .map { it.first }
}

/**
 * The words the library can be searched by: every title, artist and album in it.
 *
 * A search looks in all three, so a near match has to be offered from all three as
 * well: somebody who mistypes an album name is as lost as somebody who mistypes a song.
 */
fun libraryTerms(tracks: List<Track>): List<String> {
    val terms = LinkedHashSet<String>()
    tracks.forEach { track ->
        track.title.takeIf { it.isNotBlank() }?.let(terms::add)
        track.artist?.takeIf { it.isNotBlank() }?.let(terms::add)
        track.album?.takeIf { it.isNotBlank() }?.let(terms::add)
    }
    return terms.toList()
}
