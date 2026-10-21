package com.exodidio.mimi.lyrics

import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

/**
 * Reads the words of a QRC file.
 *
 * QRC is the XML that QQ Music writes. A line is an element carrying the span of time it
 * is sung over, and inside it every word is an element of its own with the span of time
 * that word is sung over. The times are in milliseconds, so nothing has to be converted.
 *
 * The reading is deliberately forgiving, because these files come from several tools that
 * each get them slightly wrong. A line without words is still a line, a time that is not
 * a number is ignored, and a file that is not XML at all gives nothing back rather than
 * an exception.
 */
object QrcParser {

    /** The type that marks a set of lines as carrying no timing at all. */
    private const val UNSYNCED = "2"

    /**
     * The words of a QRC file, or nothing when the file cannot be read.
     *
     * The lines come back in the order they are sung in.
     */
    fun parse(raw: String): Lyrics {
        if (raw.isBlank()) return Lyrics.EMPTY

        val document = runCatching { read(raw) }.getOrNull() ?: return Lyrics.EMPTY
        val holders = holders(document)
        if (holders.isEmpty()) return Lyrics.EMPTY

        // A file may carry a timed set of lines and an untimed one beside it, such as a
        // translation. The timed one is the one worth showing.
        val timed = holders.filter { it.type() != UNSYNCED }
        val chosen = if (timed.isNotEmpty()) timed else holders

        val lines = chosen.flatMap { holder ->
            holder.lines().mapNotNull { line(it, holder.type() == UNSYNCED) }
        }
        if (lines.isEmpty()) return Lyrics.EMPTY
        return Lyrics(lines.sortedBy { it.timeMs ?: Long.MAX_VALUE })
    }

    /**
     * Reads the file into a tree of elements.
     *
     * A file that is not XML throws here, and the caller turns that into nothing rather
     * than letting it reach the screen.
     */
    private fun read(raw: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        // A QRC file has no document type, so refusing one keeps a file from being able
        // to describe entities of its own.
        runCatching {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val builder = factory.newDocumentBuilder()
        builder.setErrorHandler(QUIET)
        return builder.parse(InputSource(StringReader(raw)))
    }

    /** The elements that hold a set of lines, such as `<Lyrics_1 LyricsType="1">`. */
    private fun holders(document: Document): List<Element> {
        val all = document.getElementsByTagName("*")
        val found = mutableListOf<Element>()
        for (index in 0 until all.length) {
            val element = all.item(index) as? Element ?: continue
            if (element.tagName.startsWith("Lyrics_")) found += element
        }
        return found
    }

    /** The lines inside a holder, which are the elements named after a lyric. */
    private fun Element.lines(): List<Element> = children().filter { it.tagName.startsWith("Lyric_") }

    /**
     * One line of the file.
     *
     * The words inside the line are the line itself when it has any, because a file that
     * spells out the words of a line also spells out the line. The text written directly
     * on the line is used only when the line has no words of its own.
     */
    private fun line(element: Element, unsynced: Boolean): LyricLine? {
        val words = element.children().filter { it.tagName.startsWith("Lyric_") }.mapNotNull { word(it) }
        val text = if (words.isNotEmpty()) words.joinToString("") { it.text } else element.ownText().trim()
        if (text.isBlank()) return null

        val start = element.attribute("Start")?.toLongOrNull() ?: words.firstOrNull()?.startMs
        return LyricLine(
            timeMs = if (unsynced) null else start,
            text = text,
            words = if (unsynced) emptyList() else words,
        )
    }

    /** One word of a line, with the span of time it is sung over. */
    private fun word(element: Element): LyricWord? {
        val text = element.textContent
        if (text.isBlank()) return null
        val start = element.attribute("Start")?.toLongOrNull() ?: return null
        val end = element.attribute("End")?.toLongOrNull() ?: start
        return LyricWord(text = text, startMs = start, endMs = if (end < start) start else end)
    }

    /** The elements directly inside this one. */
    private fun Element.children(): List<Element> {
        val found = mutableListOf<Element>()
        val nodes = childNodes
        for (index in 0 until nodes.length) {
            (nodes.item(index) as? Element)?.let { found += it }
        }
        return found
    }

    /** The text written directly on this element, without the text of any element inside it. */
    private fun Element.ownText(): String {
        val builder = StringBuilder()
        val nodes = childNodes
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE) {
                builder.append(node.nodeValue)
            }
        }
        return builder.toString()
    }

    /** The kind of lines this holder carries, which is empty when it says nothing. */
    private fun Element.type(): String = getAttribute("LyricsType").trim()

    /** The value of an attribute, or nothing when the element does not have it. */
    private fun Element.attribute(name: String): String? =
        if (hasAttribute(name)) getAttribute(name).trim() else null

    /**
     * Keeps the parser quiet and stops a broken file from being read half way through.
     *
     * A fatal error is thrown again so that the file is given up on rather than shown as
     * a half finished song.
     */
    private val QUIET = object : ErrorHandler {
        override fun warning(exception: SAXParseException) = Unit
        override fun error(exception: SAXParseException) = Unit
        override fun fatalError(exception: SAXParseException): Unit = throw exception
    }
}
