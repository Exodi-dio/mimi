package com.exodidio.mimi.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentPaletteTest {

    @Test
    fun everyAccentHasItsOwnName() {
        val names = Accents.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun everyAccentHasALabel() {
        assertTrue(Accents.all { it.label.isNotBlank() })
    }

    @Test
    fun anUnknownNameFallsBackToTheAppsOwnColour() {
        assertEquals(DefaultAccent, accentByName("chartreuse"))
        assertEquals(DefaultAccent, accentByName(null))
        assertEquals(DefaultAccent, accentByName(""))
    }

    @Test
    fun aKnownNameFindsItsAccent() {
        assertEquals("blue", accentByName("blue").name)
    }

    @Test
    fun darkBackingsGetLightText() {
        assertEquals(Color(0xFFF5E7C6), onColourFor(Color(0xFF000000)))
        assertEquals(Color(0xFFF5E7C6), onColourFor(Color(0xFF121014)))
    }

    @Test
    fun paleBackingsGetDarkText() {
        assertEquals(Color(0xFF141218), onColourFor(Color(0xFFFDF6E7)))
    }

    @Test
    fun theHighlightIsNotTheAccentItself() {
        val accent = Color(0xFF2F6FED)
        assertNotEquals(accent, highlightFor(accent, Color(0xFF121014)))
    }

    @Test
    fun theHighlightKeepsTheAccentsHue() {
        val accent = Color(0xFF2F6FED)
        val highlight = highlightFor(accent, Color(0xFF121014))
        // A highlight that drifts to another colour is a second accent, not a highlight.
        assertTrue(highlight.blue > highlight.red)
        assertTrue(highlight.blue > highlight.green)
    }

    @Test
    fun theAmoledSchemeIsTrueBlack() {
        val scheme = darkSchemeFor(accentByName("ink"), amoled = true)
        assertEquals(Color(0xFF000000), scheme.background)
        assertEquals(Color(0xFF000000), scheme.surface)
    }

    @Test
    fun thePlainDarkSchemeIsNotTrueBlack() {
        val scheme = darkSchemeFor(accentByName("ink"), amoled = false)
        assertNotEquals(Color(0xFF000000), scheme.background)
    }

    @Test
    fun theAccentReachesTheScheme() {
        val accent = accentByName("rose")
        val scheme = darkSchemeFor(accent, amoled = false)
        assertEquals(accent.dark, scheme.primary)
    }
}
