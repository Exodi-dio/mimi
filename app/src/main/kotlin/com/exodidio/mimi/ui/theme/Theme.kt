package com.exodidio.mimi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The second colour, for the things that should catch the eye without shouting.
 *
 * It is a companion local rather than a field on the scheme because the scheme is
 * Material's, and Material will not carry a colour it does not know about. Anything
 * that wants the highlight asks for it by name.
 */
val LocalHighlight = staticCompositionLocalOf { Color(0xFF2F6FED) }

/**
 * The skinless beta theme.
 *
 * Deliberately plain: the real look is a later milestone, this just keeps the app
 * readable in light and dark mode.
 *
 * [accent] chooses the colour the app draws attention with, [amoled] takes the dark
 * backgrounds to true black so an OLED screen spends no battery on them, and
 * [highlight] is the quieter companion used for the smaller emphases.
 */
@Composable
fun MimiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    accent: String? = null,
    highlight: String? = null,
    content: @Composable () -> Unit,
) {
    val chosen = accentByName(accent)
    val scheme = if (darkTheme) darkSchemeFor(chosen, amoled) else lightSchemeFor(chosen)

    val background = scheme.background
    val highlightColour = highlight?.let { name ->
        Accents.firstOrNull { it.name == name }?.let { picked ->
            if (darkTheme) picked.dark else picked.light
        }
    } ?: highlightFor(if (darkTheme) chosen.dark else chosen.light, background)

    CompositionLocalProvider(LocalHighlight provides highlightColour) {
        MaterialTheme(
            colorScheme = scheme,
            content = content,
        )
    }
}
