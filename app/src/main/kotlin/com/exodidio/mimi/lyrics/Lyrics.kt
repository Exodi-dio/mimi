package com.exodidio.mimi.lyrics

/**
 * One line of lyrics.
 *
 * [timeMs] is null when the file carries words but no timing, which is what most embedded
 * tags do: the app then shows the words without pretending to follow along.
 */
data class LyricLine(val timeMs: Long?, val text: String)

/** The words of one song, in the order they are read. */
data class Lyrics(val lines: List<LyricLine>) {

    val isEmpty: Boolean get() = lines.isEmpty()

    /** True when at least one line carries a timestamp. */
    val synced: Boolean get() = lines.any { it.timeMs != null }

    /**
     * The line that belongs on screen at [positionMs], or -1 before the first one.
     *
     * The parser keeps lines in time order, so this can walk forward and stop at the first
     * line that is still in the future.
     */
    fun indexAt(positionMs: Long): Int {
        var found = -1
        for (index in lines.indices) {
            val at = lines[index].timeMs ?: continue
            if (at > positionMs) break
            found = index
        }
        return found
    }

    companion object {
        val EMPTY = Lyrics(emptyList())
    }
}
