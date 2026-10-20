package com.exodidio.mimi.lyrics

/**
 * Just enough JSON to read a lyrics answer.
 *
 * The app has no JSON library and no serialisation plugin, and the only JSON it ever sees
 * comes from one endpoint with a shape known in advance, so a full parser would be a
 * dependency bought for one screen. This walks the text and pulls out the string fields of
 * the objects at the top level, which is exactly what a lyrics lookup needs.
 *
 * Nested objects and arrays are skipped rather than descended into, because nothing inside
 * them is wanted. Numbers and booleans are skipped for the same reason: the only things
 * the caller reads are `syncedLyrics` and `plainLyrics`, and both are strings or null.
 *
 * Deliberately tolerant. Malformed input yields fewer objects rather than an exception,
 * because this runs on an answer from the network and a crash is a worse outcome than a
 * song without lyrics.
 */
object LyricsJson {

    /**
     * Every object at the top level of [raw], keyed by field name.
     *
     * A response may be a single object (a direct lookup) or an array of them (a search),
     * so both are accepted and a single object reads as a list of one.
     */
    fun objects(raw: String): List<Map<String, String>> {
        val reader = Reader(raw)
        reader.skipSpace()
        return when (reader.peek()) {
            '[' -> reader.readArray()
            '{' -> listOfNotNull(reader.readObject())
            else -> emptyList()
        }
    }

    private class Reader(private val text: String) {
        private var index = 0

        fun peek(): Char? = if (index < text.length) text[index] else null

        fun skipSpace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        private fun take(char: Char): Boolean {
            skipSpace()
            if (peek() != char) return false
            index++
            return true
        }

        fun readArray(): List<Map<String, String>> {
            val found = mutableListOf<Map<String, String>>()
            if (!take('[')) return found
            skipSpace()
            if (peek() == ']') {
                index++
                return found
            }

            while (true) {
                skipSpace()
                when (peek()) {
                    '{' -> readObject()?.let { found += it }
                    ']' -> break
                    null -> break
                    else -> skipValue()
                }
                skipSpace()
                when (peek()) {
                    ',' -> index++
                    ']' -> break
                    null -> break
                    else -> break
                }
            }

            if (peek() == ']') index++
            return found
        }

        /**
         * One object, or null when the text ran out before it was closed.
         *
         * An unclosed object is reported as nothing rather than as a short answer: half a
         * song's words are worse than none, and a connection that dropped mid answer
         * should cost the songs it did not finish, not the ones it did.
         */
        fun readObject(): Map<String, String>? {
            val found = mutableMapOf<String, String>()
            if (!take('{')) return null
            skipSpace()
            if (peek() == '}') {
                index++
                return found
            }

            var closed = false
            while (true) {
                skipSpace()
                val key = readString() ?: break
                if (!take(':')) break
                skipSpace()

                if (peek() == '"') {
                    readString()?.let { found[key] = it }
                } else {
                    skipValue()
                }

                skipSpace()
                when (peek()) {
                    ',' -> index++
                    '}' -> {
                        closed = true
                        break
                    }
                    else -> break
                }
            }

            if (peek() == '}') index++
            return if (closed || peek() == '}') found else null
        }

        private fun readString(): String? {
            skipSpace()
            if (peek() != '"') return null
            index++

            val built = StringBuilder()
            while (index < text.length) {
                val char = text[index++]
                when {
                    char == '"' -> return built.toString()
                    char == '\\' -> {
                        if (index >= text.length) return built.toString()
                        when (val escaped = text[index++]) {
                            'n' -> built.append('\n')
                            't' -> built.append('\t')
                            'r' -> built.append('\r')
                            'b' -> built.append('\b')
                            'f' -> built.append('\u000C')
                            'u' -> {
                                val end = minOf(index + 4, text.length)
                                val code = text.substring(index, end).toIntOrNull(16)
                                index = end
                                if (code != null) built.append(code.toChar())
                            }
                            else -> built.append(escaped)
                        }
                    }
                    else -> built.append(char)
                }
            }

            // Ran out of text mid-string: hand back what there was rather than nothing.
            return built.toString()
        }

        /** Moves past one value of any kind. */
        private fun skipValue() {
            skipSpace()
            when (peek()) {
                '"' -> readString()
                '{' -> {
                    readObject()
                    Unit
                }
                '[' -> readArray()
                else -> {
                    while (index < text.length && text[index] !in ",}]") index++
                }
            }
        }
    }
}
