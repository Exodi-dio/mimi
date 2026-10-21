package com.exodidio.mimi.lyrics

/**
 * The formats a file of words can be written in, and how to read each one.
 *
 * The app reads two of them. LRC is the stamped line format that most players write, and
 * QRC is the XML that QQ Music writes, which says when each word is sung as well as when
 * each line is. They are told apart by their shape rather than by their name, because a
 * file is often called something that says nothing about what is inside it.
 */
object LyricsFormats {

    /**
     * Reads a file of words in whichever of the formats it is written in.
     *
     * A file that is neither format gives nothing back, which is the same answer a file
     * with no words in it gives.
     */
    fun parse(raw: String): Lyrics = if (isQrc(raw)) QrcParser.parse(raw) else LyricsParser.parse(raw)

    /**
     * True when the text is QRC rather than LRC.
     *
     * QRC is XML, so it either announces itself as such or carries the element that
     * holds the lines of a song.
     */
    fun isQrc(raw: String): Boolean =
        raw.trimStart().startsWith("<?xml") || raw.contains("<Lyrics_")
}
