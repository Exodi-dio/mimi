package com.exodidio.mimi.library

/**
 * Names the listener has hidden from the library.
 *
 * Hiding is a view, not a deletion: a hidden artist or album keeps its files exactly where
 * they are, and every list, search result and count simply leaves it out. [separators] is
 * carried along so `A & B` hides when either A or B is hidden, which is the same rule the
 * artist list already uses to decide who is on a credit.
 */
data class HiddenNames(
    val artists: Set<String> = emptySet(),
    val albums: Set<String> = emptySet(),
    val separators: List<String> = Settings.DEFAULT_CREDIT_SEPARATORS,
) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty()
}

/** The hidden names in [this], normalised for comparison. */
fun Settings.hiddenNames(): HiddenNames = HiddenNames(
    artists = hiddenArtists.normalisedNames(),
    albums = hiddenAlbums.normalisedNames(),
    separators = creditSeparators,
)

/**
 * The shared list format, under a name that says what it holds.
 *
 * It deliberately delegates to the folder encoder: the format is the same newline-separated
 * list, and one decoder means a change to the format cannot reach one list and miss another.
 */
fun encodeHiddenNames(names: Collection<String>): String = encodeExcludedFolders(names)

/** The hidden names held in [stored]. */
fun decodeHiddenNames(stored: String?): List<String> = decodeExcludedFolders(stored)

/**
 * Adds [name] to a hidden list.
 *
 * Comparison ignores case and stray spacing, so hiding `Ada` twice, or once as `ada`,
 * cannot leave two rows that mean the same thing in the settings list.
 */
fun List<String>.addingHiddenName(name: String): List<String> {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return this
    return (this + trimmed)
        .distinctBy { it.trim().lowercase() }
        .sortedBy { it.lowercase() }
}

/** Takes [name] out of a hidden list, ignoring case and stray spacing. */
fun List<String>.removingHiddenName(name: String): List<String> {
    val wanted = name.trim()
    return filterNot { it.trim().equals(wanted, ignoreCase = true) }
}

/** Whether this song is hidden by name, either because of its album or one of its credits. */
fun Track.isHiddenBy(hidden: HiddenNames): Boolean {
    if (hidden.isEmpty) return false

    val albumName = album?.trim().orEmpty().lowercase()
    if (albumName.isNotEmpty() && albumName in hidden.albums) return true

    val credits = splitCredits(artist, hidden.separators) +
        splitCredits(albumArtist, hidden.separators)
    return credits.any { it.trim().lowercase() in hidden.artists }
}

/** This list with every hidden song left out, keeping the order it arrived in. */
fun List<Track>.tracksWithoutHidden(hidden: HiddenNames): List<Track> =
    if (hidden.isEmpty) this else filterNot { it.isHiddenBy(hidden) }

/** Whether an album row is hidden by its own name or by its artist. */
fun AlbumSummary.isHiddenBy(hidden: HiddenNames): Boolean {
    if (hidden.isEmpty) return false
    if (album.trim().lowercase() in hidden.albums) return true
    val credits = splitCredits(artist, hidden.separators)
    return credits.any { it.trim().lowercase() in hidden.artists }
}

/** Whether an artist row is hidden by name. */
fun ArtistSummary.isHiddenBy(hidden: HiddenNames): Boolean =
    !hidden.isEmpty && artist.trim().lowercase() in hidden.artists

private fun Collection<String>.normalisedNames(): Set<String> =
    map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()
