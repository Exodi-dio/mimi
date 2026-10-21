package com.exodidio.mimi.library

/**
 * How the albums grid is ordered.
 *
 * An order has a natural direction: names read upwards, which is how the library already
 * reads them, while counts run downwards, because a list ordered by how much of it there
 * is only means anything with the most at the top. The reverse switch flips whichever
 * order is chosen; it is a view preference, not a second meaning for the field.
 */
enum class AlbumSort(val label: String) {
    NAME("Album name"),
    ARTIST("Album artist"),
    SONGS("Most songs"),
}

/** How the artists list is ordered, with the same natural directions as [AlbumSort]. */
enum class ArtistSort(val label: String) {
    NAME("Artist name"),
    ALBUMS("Most albums"),
    SONGS("Most songs"),
}

/**
 * Orders albums the way the albums grid was asked to.
 *
 * Every order ends on the album name and then the album id, so albums that agree on the
 * chosen field — the common case for "album artist" and for counts — come out in a
 * settled order rather than in whatever order the query happened to return. A grid that
 * reshuffles its ties on every re-emit is a grid whose cells move as you reach for them.
 *
 * Pure, so the ordering can be tested without a device.
 */
fun AlbumSort.comparator(descending: Boolean = false): Comparator<AlbumSummary> {
    val natural: Comparator<AlbumSummary> = when (this) {
        AlbumSort.NAME -> compareBy<AlbumSummary> { it.album.lowercase() }
            .thenBy { it.albumId }

        AlbumSort.ARTIST -> compareBy<AlbumSummary, String?>(nullsLast()) { it.artist?.lowercase() }
            .thenBy { it.album.lowercase() }
            .thenBy { it.albumId }

        AlbumSort.SONGS -> compareByDescending<AlbumSummary> { it.trackCount }
            .thenBy { it.album.lowercase() }
            .thenBy { it.albumId }
    }
    return if (descending) natural.reversed() else natural
}

/**
 * Orders artists the way the artists list was asked to.
 *
 * The name is the tie-break rather than an id, because an artist is named by a string
 * rather than by a row, and two artists cannot share a name.
 */
fun ArtistSort.comparator(descending: Boolean = false): Comparator<ArtistSummary> {
    val natural: Comparator<ArtistSummary> = when (this) {
        ArtistSort.NAME -> compareBy<ArtistSummary> { it.artist.lowercase() }

        ArtistSort.ALBUMS -> compareByDescending<ArtistSummary> { it.albumCount }
            .thenBy { it.artist.lowercase() }

        ArtistSort.SONGS -> compareByDescending<ArtistSummary> { it.trackCount }
            .thenBy { it.artist.lowercase() }
    }
    return if (descending) natural.reversed() else natural
}
