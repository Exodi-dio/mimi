package com.exodidio.mimi.lyrics

/**
 * The mark that holds a line in the order it was given.
 *
 * Arabic and Hebrew are read from right to left, and the text engine would otherwise
 * turn such a line around once more. The mark says that the words are already in the
 * order they should be drawn in, so the sweep decides where the highlight travels.
 */
const val LEFT_TO_RIGHT_MARK = "\u200E"

/**
 * The zero width joiner that keeps the pieces of a word together.
 *
 * Without it a long line of Korean or Japanese could be broken in the middle of a word,
 * which would put a particle on a line of its own.
 */
private const val WORD_JOINER = "\u2060"

/**
 * One word of a line, and how much of it has been sung.
 *
 * A word can be made of several pieces, such as a Korean word with the particle that
 * belongs to it, and the pieces are kept apart so that they can be joined up again in a
 * way that the page cannot break in two.
 */
data class SweptWord(
    /** The pieces the word was made of, in the order they are drawn. */
    val parts: List<String>,
    /** The space that comes before the word on the page. */
    val gap: String,
    /** How much of the word has been sung, from nothing at all to all of it. */
    val brightness: Float,
    /** True while the song is inside this word. */
    val current: Boolean,
) {
    /** The word as it is read. */
    val text: String get() = parts.joinToString("")

    /** The word as it is drawn, with its pieces joined so that a line cannot break it. */
    val drawn: String get() = parts.joinToString(WORD_JOINER)
}

/**
 * Works out how the words of a line are lit as the song is sung.
 *
 * The decisions are made here rather than while drawing, because what is lit and how a
 * line is laid out are matters of the song rather than of the page. Nothing in this file
 * knows what a screen is.
 */
object KaraokeSweep {

    /**
     * The words of [line] as they are drawn at [positionMs].
     *
     * The words come back in the order they are drawn, which for a line that is read from
     * right to left is the reverse of the order they are sung in. A line without word
     * timings gives nothing back, because there is nothing to sweep.
     */
    fun sweep(line: LyricLine, positionMs: Long): List<SweptWord> {
        if (line.words.isEmpty()) return emptyList()

        val rightToLeft = isRightToLeft(line.text)
        val groups = group(line.words, rightToLeft)
        val drawn = if (rightToLeft) groups.reversed() else groups

        return drawn.map { entry ->
            SweptWord(
                parts = entry.parts,
                gap = entry.gap,
                brightness = progress(entry.startMs, entry.endMs, positionMs),
                current = positionMs >= entry.startMs && positionMs <= entry.endMs,
            )
        }
    }

    /**
     * True when the words of a line are read from right to left.
     *
     * The direction is taken from the first letter of the line, which is how a page
     * decides which way to begin reading.
     */
    fun isRightToLeft(text: String): Boolean {
        for (char in text) {
            if (isRightToLeftChar(char)) return true
            if (char.isLetter()) return false
        }
        return false
    }

    /**
     * The words of a line, gathered into the words that are drawn as one.
     *
     * A particle of Korean or Japanese that is written straight onto the word before it
     * belongs to that word, so the two are drawn together and lit together. A particle
     * that stands on its own is a word of its own.
     *
     * The space between two words belongs to the word that follows it on the page, which
     * is what keeps the words of a line in the same order whichever way the line is
     * read.
     */
    private fun group(words: List<LyricWord>, rightToLeft: Boolean): List<Group> {
        val groups = mutableListOf<Group>()

        for (word in words) {
            val previous = groups.lastOrNull()

            if (previous != null && isParticle(word.text) && !previous.endsWithGap()) {
                groups[groups.lastIndex] = previous.copy(
                    parts = previous.parts + word.text,
                    endMs = maxOf(previous.endMs, word.endMs),
                )
            } else {
                groups += Group(parts = listOf(word.text), startMs = word.startMs, endMs = word.endMs)
            }
        }

        return groups.mapIndexed { index, entry ->
            val previous = groups.getOrNull(index - 1)
            val gap = if (rightToLeft) entry.ownGap() else previous?.ownGap().orEmpty()
            entry.copy(parts = entry.parts.withoutTrailingGap(), gap = gap)
        }
    }

    /**
     * True when the word is a particle written straight onto the word before it.
     *
     * A particle with a space in front of it stands on its own and is a word of its own.
     */
    private fun isParticle(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length != 1 || !PARTICLES.contains(trimmed)) return false
        return text.first() == trimmed.first()
    }

    /**
     * How much of a word has been sung by [positionMs].
     *
     * The word fills up over its own span of time, so the light steps across the word
     * rather than switching on and off at its edges.
     */
    private fun progress(startMs: Long, endMs: Long, positionMs: Long): Float = when {
        positionMs < startMs -> 0f
        positionMs >= endMs -> 1f
        endMs <= startMs -> 1f
        else -> (positionMs - startMs).toFloat() / (endMs - startMs).toFloat()
    }

    /** True when the character is written from right to left. */
    private fun isRightToLeftChar(char: Char): Boolean = when (char.code) {
        in 0x0590..0x05FF -> true
        in 0x0600..0x07BF -> true
        in 0x08A0..0x08FF -> true
        in 0xFB1D..0xFDFF -> true
        in 0xFE70..0xFEFF -> true
        in 0x10800..0x10FFF -> true
        in 0x1E800..0x1EFFF -> true
        else -> false
    }

    /** The whitespace at the end of the last piece, which is the gap after the word. */
    private fun Group.ownGap(): String = parts.lastOrNull()?.trailingGap().orEmpty()

    /** True when the word is written straight onto the one after it. */
    private fun Group.endsWithGap(): Boolean = parts.lastOrNull()?.endsWithWhitespace() == true

    /** The whitespace at the end of a piece of text. */
    private fun String.trailingGap(): String {
        val trimmed = trimEnd()
        return substring(trimmed.length)
    }

    /** True when the text ends with whitespace. */
    private fun String.endsWithWhitespace(): Boolean = lastOrNull()?.isWhitespace() == true

    /** The pieces of a word without the space that came after it. */
    private fun List<String>.withoutTrailingGap(): List<String> {
        if (isEmpty()) return this
        val last = last()
        return dropLast(1) + last.trimEnd()
    }

    /**
     * The particles of Korean and Japanese, which belong to the word before them.
     */
    private val PARTICLES = setOf(
        "은",
        "는",
        "이",
        "가",
        "을",
        "를",
        "의",
        "の",
        "は",
        "が",
        "を",
        "に",
        "と",
        "も",
        "や",
    )

    /**
     * The words of a line as they are drawn, before they are lit.
     */
    private data class Group(
        /** The pieces the word was made of. */
        val parts: List<String>,
        /** When the word starts, in milliseconds from the beginning of the song. */
        val startMs: Long,
        /** When the word ends, in milliseconds from the beginning of the song. */
        val endMs: Long,
        /** The space that comes before the word on the page. */
        val gap: String = "",
    )
}
