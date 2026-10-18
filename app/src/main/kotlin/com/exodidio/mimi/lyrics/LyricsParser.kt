package com.exodidio.mimi.lyrics

/**
 * Reads the words a file carries, timed or not.
 *
 * Timed lyrics arrive as LRC, which wraps each line in one or more `[mm:ss.xx]` stamps.
 * Untimed lyrics are just lines of text. Both are handled here so that the rest of the app
 * only ever deals with [Lyrics].
 */
object LyricsParser {

    /** `[mm:ss.xx]`, with either a dot or a colon before the fraction, and the fraction optional. */
    private val TIMESTAMP = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    /** Enhanced LRC marks when each word starts. The panel highlights lines, not words, so they go. */
    private val WORD_TIMESTAMP = Regex("""<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>""")

    /**
     * `[ar:]`, `[ti:]` and friends describe the song rather than being part of it.
     *
     * Only tags that really are headers are dropped. A line such as `[Chorus]` has no colon
     * and is left alone, because it is a line of the song.
     */
    private val HEADER = Regex(
        """^\[(ar|ti|al|au|by|re|ve|length|offset|encoding):""",
        RegexOption.IGNORE_CASE,
    )

    /** `[offset:+250]` shifts every stamp, for rips that ran fast or slow. */
    private val OFFSET = Regex("""^\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): Lyrics {
        val text = raw.replace("\r\n", "\n").replace('\r', '\n')
        if (text.isBlank()) return Lyrics.EMPTY

        var offsetMs = 0L
        val timed = mutableListOf<LyricLine>()
        val untimed = mutableListOf<String>()

        for (line in text.split('\n')) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val offset = OFFSET.matchEntire(trimmed)
            if (offset != null) {
                offsetMs = offset.groupValues[1].toLongOrNull() ?: 0L
                continue
            }

            val stamps = TIMESTAMP.findAll(trimmed).toList()
            // Whatever is neither a line stamp nor a word stamp is the words themselves.
            val words = WORD_TIMESTAMP.replace(TIMESTAMP.replace(trimmed, ""), "").trim()

            if (stamps.isEmpty()) {
                if (HEADER.containsMatchIn(trimmed)) continue
                if (words.isNotEmpty()) untimed += words
                continue
            }
            if (words.isEmpty()) continue
            for (stamp in stamps) timed += LyricLine(stamp.toMillis() + offsetMs, words)
        }

        // Timed lines sorted by time; otherwise the words alone, in the order they were written.
        if (timed.isEmpty()) return Lyrics(untimed.map { LyricLine(null, it) })
        return Lyrics(timed.sortedBy { it.timeMs })
    }

    private fun MatchResult.toMillis(): Long {
        val minutes = groupValues[1].toLongOrNull() ?: 0L
        val seconds = groupValues[2].toLongOrNull() ?: 0L
        val fraction = groupValues[3]
        val millis = when {
            fraction.isNullOrEmpty() -> 0L
            fraction.length == 1 -> fraction.toLong() * 100
            fraction.length == 2 -> fraction.toLong() * 10
            else -> fraction.take(3).toLong()
        }
        return minutes * 60_000L + seconds * 1_000L + millis
    }
}
