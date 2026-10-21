package com.exodidio.mimi.library

/**
 * How a list inside a browse screen is ordered.
 *
 * [AS_LISTED] is first and is the default because an album's running order, an artist's
 * records and a playlist's running order all carry meaning. Sorting is something the
 * listener asks for, not something the app decides on their behalf.
 */
enum class DetailSort(val label: String) {
    AS_LISTED("As listed"),
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    YEAR("Year"),
    DATE_ADDED("Recently added"),
    DURATION("Duration"),
}

/**
 * Orders [this] by [sort], or leaves it untouched when the list's own order was chosen.
 *
 * The direction is shared with the songs list: one switch in a sheet changes the way every
 * sorted list reads, which is easier to predict than a separate direction per screen.
 *
 * Pure, so the ordering can be checked without a device.
 */
fun List<Track>.sortedForDetail(sort: DetailSort, descending: Boolean): List<Track> =
    when (sort) {
        DetailSort.AS_LISTED -> this
        DetailSort.TITLE -> sortedWith(SongSort.TITLE.comparator(descending))
        DetailSort.ARTIST -> sortedWith(SongSort.ARTIST.comparator(descending))
        DetailSort.ALBUM -> sortedWith(SongSort.ALBUM.comparator(descending))
        DetailSort.YEAR -> sortedWith(SongSort.YEAR.comparator(descending))
        DetailSort.DATE_ADDED -> sortedWith(SongSort.DATE_ADDED.comparator(descending))
        DetailSort.DURATION -> sortedWith(SongSort.DURATION.comparator(descending))
    }
