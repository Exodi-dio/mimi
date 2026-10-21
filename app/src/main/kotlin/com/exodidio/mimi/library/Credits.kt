package com.exodidio.mimi.library

/**
 * Splitting a credit line into the people or genres it actually names.
 *
 * A tag reading `A & B feat. C` is three credits rather than one long name, and treating it
 * as one pollutes every list built from it. Which marks separate names is a property of the
 * library and not of the format — `Earth, Wind & Fire` is one band — so the separators come
 * from settings and this file only applies them.
 */

/** Runs of whitespace inside a name, which tags pick up and lists should not show. */
private val WHITESPACE = Regex("\\s+")

/**
 * The credits held in [value], in the order they were written.
 *
 * Separators are matched literally and without regard to case, so `feat.` does not match
 * `featuring` unless that is also listed, and an entry full of blanks is dropped rather
 * than becoming a nameless artist. A separator at either end is ignored rather than
 * producing an empty credit, because a trailing ` & ` in a tag is a mistake, not a person.
 *
 * An empty [separators] list means "do not split" and gives the whole value back: settings
 * can be cleared to nothing, and the honest reading of that is one credit, not none.
 */
fun splitCredits(value: String?, separators: List<String>): List<String> {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return emptyList()

    val usable = separators.map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
    if (usable.isEmpty()) return listOf(text.collapseWhitespace())

    // Longest first, so a word like "featuring" is tried before the shorter "feat" that
    // starts it rather than leaving the tail of the word behind as part of a name.
    val pattern = usable.sortedByDescending { it.length }
        .joinToString("|") { Regex.escape(it) }

    return text.split(Regex(pattern, RegexOption.IGNORE_CASE))
        .map { it.collapseWhitespace() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
}

/**
 * The separators as one stored value.
 *
 * This is the same newline-separated form the excluded folders use, so [decodeExcludedFolders]
 * reads it back and there is one format to remember rather than two. The order is kept as
 * the listener wrote it, because that is the order the settings screen shows it in.
 */
fun encodeCreditSeparators(separators: Collection<String>): String =
    separators.map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .joinToString(STORED_LIST_SEPARATOR)

/**
 * [separators] with [value] added, or the same list when adding it would change nothing.
 *
 * The editor adds one separator at a time rather than parsing a line, which is what makes a
 * lone comma possible: a comma-delimited field cannot hold the comma itself. Only the ends
 * are trimmed, so a multi-word separator keeps the space between its words, and a value
 * already present in another case is dropped rather than stored twice.
 */
fun addCreditSeparator(separators: List<String>, value: String): List<String> {
    val cleaned = value.trim()
    if (cleaned.isEmpty()) return separators
    if (separators.any { it.equals(cleaned, ignoreCase = true) }) return separators
    return separators + cleaned
}

/** Trimmed with runs of whitespace folded to one space, which is how a name is shown. */
private fun String.collapseWhitespace(): String = trim().replace(WHITESPACE, " ")
