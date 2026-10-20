package com.exodidio.mimi.lyrics

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RomanizationTest {

    /**
     * An engine reading from three tiny dictionaries held in memory.
     *
     * The real ones are a megabyte and a quarter of pinyin data, and a unit test that
     * needed those would be a test of the asset files rather than of the code that reads
     * them. What is being checked here is the matching, the script splitting and the
     * safety limits.
     */
    private fun engine(): AndroidRomanizationEngine {
        val assets = mapOf(
            "romanization/pinyin.txt" to "你好: n\u01d0 h\u01ceo",
            "romanization/TSCharacters.txt" to "",
            "romanization/characters.tsv" to "4f60\tn\u01d0\n597d\th\u01ceo",
        )
        return AndroidRomanizationEngine { name -> assets.getValue(name).byteInputStream() }
    }

    @Test
    fun `a normal lyric sheet is accepted`() {
        validateRomanization(listOf("[00:12.34] a line", "a second line"))
    }

    @Test
    fun `too many lines are refused`() {
        val lines = List(2_001) { "line" }
        val refused = runCatching { validateRomanization(lines) }.isFailure
        assertTrue(refused)
    }

    @Test
    fun `a line longer than the limit is refused`() {
        val tooLong = "a".repeat(2_049)
        assertTrue(runCatching { validateRomanization(listOf(tooLong)) }.isFailure)
    }

    @Test
    fun `a line at the limit is allowed`() {
        validateRomanization(listOf("a".repeat(2_048)))
    }

    @Test
    fun `a sheet larger than the limit is refused`() {
        // 2 000 lines of 100 bytes each is 200 KiB, past the 128 KiB ceiling.
        val lines = List(2_000) { "a".repeat(100) }
        assertTrue(runCatching { validateRomanization(lines) }.isFailure)
    }

    @Test
    fun `half of a surrogate pair is refused`() {
        assertTrue(runCatching { validateRomanization(listOf("\uD800")) }.isFailure)
        assertTrue(runCatching { validateRomanization(listOf("\uDC00")) }.isFailure)
    }

    @Test
    fun `a whole surrogate pair is allowed`() {
        validateRomanization(listOf("\uD83C\uDFB5 music note"))
    }

    @Test
    fun `korean syllables become latin letters`() = runBlocking {
        assertEquals("sarang", KoreanRomanizer.romanize("\uC0AC\uB791"))
        assertEquals("annyeong", KoreanRomanizer.romanize("\uC548\uB155"))
    }

    @Test
    fun `non hangul text is left exactly as it was`() {
        assertEquals("hello 123", KoreanRomanizer.romanize("hello 123"))
    }

    @Test
    fun `a chinese phrase wins over its characters`() = runBlocking {
        assertEquals("n\u01d0 h\u01ceo", engine().romanize("\u4F60\u597D"))
    }

    @Test
    fun `a single character falls back to its own reading`() = runBlocking {
        assertEquals("h\u01ceo", engine().romanize("\u597D"))
    }

    @Test
    fun `latin text runs into chinese with a space`() = runBlocking {
        assertEquals("Hello n\u01d0 h\u01ceo", engine().romanize("Hello\u4F60\u597D"))
    }

    @Test
    fun `chinese is reported as a mandarin default`() {
        val inspection = engine().inspect("\u4F60\u597D")
        assertTrue(inspection.supported)
        assertTrue(inspection.mandarinDefault)
    }

    @Test
    fun `hangul is supported but is not a mandarin default`() {
        val inspection = engine().inspect("\uC548\uB155")
        assertTrue(inspection.supported)
        assertFalse(inspection.mandarinDefault)
    }

    @Test
    fun `kana is left alone`() {
        assertFalse(engine().inspect("\u3053\u3093\u306B\u3061\u306F").supported)
    }

    @Test
    fun `plain latin is left alone`() {
        assertFalse(engine().inspect("just words").supported)
    }

    @Test
    fun `lines that need nothing are reported as unsupported`() = runBlocking {
        val result = RomanizeLyrics(engine()).invoke(listOf("just words", "\u4F60\u597D"))
        assertEquals(RomanizedStatus.Unsupported, result[0].status)
        assertEquals(RomanizedStatus.Converted, result[1].status)
        assertEquals("n\u01d0 h\u01ceo", result[1].text)
    }
}
