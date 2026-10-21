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
        assertEquals(0xFFF5E7C6, onColourFor(Color(0xFF000000)).value.toInt())
        assertEquals(0xFFF5E7C6, onColourFor(Color(0xFF121014)).value.toInt())
    }

    @Test
    fun paleBackingsGetDarkText() {
        assertEquals(0xFF141218, onColourFor(Color(0xFFFDF6E7)).value.toInt())
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
        assertEquals(0xFF000000, scheme.background.value.toInt())
        assertEquals(0xFF000000, scheme.surface.value.toInt())
    }

    @Test
    fun thePlainDarkSchemeIsNotTrueBlack() {
        val scheme = darkSchemeFor(accentByName("ink"), amoled = false)
        assertNotEquals(0xFF000000, scheme.background.value.toInt())
    }

    @Test
    fun theAccentReachesTheScheme() {
        val accent = accentByName("rose")
        val scheme = darkSchemeFor(accent, amoled = false)
        assertEquals(accent.dark.value.toInt(), scheme.primary.value.toInt())
    }
}
