package com.exodidio.mimi.lyrics

/**
 * One word of a line, with the span of time it is sung over.
 *
 * Only files that say when each word is sung have these. An LRC file says when a line
 * starts and nothing at all about the words inside it, so a line read from one carries no
 * words and behaves exactly as it always did.
 */
data class LyricWord(
    /** The word as it is written, spaces included, so a line can be put back together. */
    val text: String,
    /** When the word starts, in milliseconds from the beginning of the song. */
    val startMs: Long,
    /** When the word ends, in milliseconds from the beginning of the song. */
    val endMs: Long,
)

/**
 * One line of lyrics.
 *
 * [timeMs] is null when the file carries words but no timing, which is what most embedded
 * tags do: the app then shows the words without pretending to follow along.
 *
 * [words] is empty unless the file says when each word is sung. When they are there the
 * line can be lit up word by word as the song plays, and the line itself is unchanged.
 */
data class LyricLine(
    val timeMs: Long?,
    val text: String,
    val words: List<LyricWord> = emptyList(),
)

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
