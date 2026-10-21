package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditsTest {

    /** The default separators, which is what most of these cases are about. */
    private val separators = Settings.DEFAULT_CREDIT_SEPARATORS

    @Test
    fun `an ampersand separates two artists`() {
        assertEquals(listOf("A", "B"), splitCredits("A & B", separators))
    }

    @Test
    fun `a featured credit is its own artist`() {
        assertEquals(listOf("A", "B"), splitCredits("A feat. B", separators))
        assertEquals(listOf("A", "B"), splitCredits("A ft. B", separators))
    }

    @Test
    fun `a word separator is matched whatever its case`() {
        assertEquals(listOf("A", "B"), splitCredits("A Featuring B", separators))
    }

    @Test
    fun `a repeated credit is listed once`() {
        assertEquals(listOf("A", "B"), splitCredits("A; B; A", separators))
    }

    @Test
    fun `a name with a comma stays whole when the comma is not a separator`() {
        assertEquals(
            listOf("Earth, Wind & Fire"),
            splitCredits("Earth, Wind & Fire", listOf(";")),
        )
    }

    @Test
    fun `a comma separator is what breaks that name apart`() {
        // Not the default, and this is why: the same tag is one band above and three
        // artists here. The setting is the only place that decision belongs.
        assertEquals(
            listOf("Earth", "Wind", "Fire"),
            splitCredits("Earth, Wind & Fire", listOf(",", "&")),
        )
    }

    @Test
    fun `blank credit belongs to nobody`() {
        assertTrue(splitCredits(null, separators).isEmpty())
        assertTrue(splitCredits("", separators).isEmpty())
        assertTrue(splitCredits("   ", separators).isEmpty())
    }

    @Test
    fun `a separator at either end does not leave an empty credit`() {
        assertEquals(listOf("A"), splitCredits("A & ", separators))
        assertEquals(listOf("A", "B"), splitCredits(" & A ; B ", separators))
    }

    @Test
    fun `duplicates are removed regardless of case`() {
        assertEquals(listOf("A", "B"), splitCredits("A; a; B; b", separators))
    }

    @Test
    fun `spacing inside a name is tidied`() {
        assertEquals(listOf("A", "B"), splitCredits("  A \t  ;   B  ", separators))
    }

    @Test
    fun `a dot in a separator is a dot and nothing else`() {
        assertEquals(listOf("A featX B"), splitCredits("A featX B", listOf("feat.")))
    }

    @Test
    fun `no separators configured leaves the value as one credit`() {
        assertEquals(listOf("A & B"), splitCredits(" A   &   B ", emptyList()))
    }

    @Test
    fun `separators are stored in the same list form as the excluded folders`() {
        assertEquals(";\n/\n&", encodeCreditSeparators(listOf(" ; ", "/", "", "&", ";")))
        assertEquals(
            listOf(";", "/", "&"),
            decodeExcludedFolders(encodeCreditSeparators(listOf(";", "/", "&"))),
        )
    }
}
