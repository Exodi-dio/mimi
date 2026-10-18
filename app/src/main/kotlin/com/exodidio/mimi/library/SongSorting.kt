package com.exodidio.mimi.library

/**
 * Orders songs the way the songs list was asked to.
 *
 * Every comparison ends on the title and then the id, so songs that agree on the chosen
 * field — the common case, since one album and one year cover many songs — come out in a
 * settled order rather than in whatever order the list happened to arrive in. A list that
 * reshuffles its ties on every re-emit is a list whose rows move as you reach for them.
 *
 * Pure, so the ordering can be tested without a device.
 */
fun SongSort.comparator(descending: Boolean): Comparator<Track> {
    val field: Comparator<Track> = when (this) {
        SongSort.TITLE -> orderedBy(descending) { track: Track -> track.title.lowercase() }
        SongSort.ARTIST -> orderedBy(descending) { track: Track -> track.artist?.lowercase() }
        SongSort.ALBUM -> orderedBy(descending) { track: Track -> track.album?.lowercase() }
        SongSort.YEAR -> orderedBy(descending) { track: Track -> track.year }
        SongSort.DATE_ADDED -> orderedBy(descending) { track: Track -> track.dateAdded }
        SongSort.DURATION -> orderedBy(descending) { track: Track -> track.durationMs }
    }

    return field.thenBy { it.title.lowercase() }.thenBy { it.id }
}

/**
 * Orders by one field.
 *
 * A song with nothing in that field goes last whichever way the sort runs. It is not
 * "less than" a song that has an artist, it simply has nothing to say, and letting the
 * direction fling those to the top would bury the songs the listener asked to see.
 *
 * Only the field itself is reversed; the ties that [comparator] appends stay ascending,
 * so a descending list still reads sensibly within each group.
 */
private fun <T, K : Comparable<K>> orderedBy(
    descending: Boolean,
    selector: (T) -> K?,
): Comparator<T> = Comparator { a, b ->
    val left = selector(a)
    val right = selector(b)
    when {
        left == null && right == null -> 0
        left == null -> 1
        right == null -> -1
        else -> if (descending) right.compareTo(left) else left.compareTo(right)
    }
}
