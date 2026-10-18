package com.exodidio.mimi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Seed colours taken from the MIMI mark: charcoal ink on cream.
private val Ink = Color(0xFF1E1B20)
private val Cream = Color(0xFFF5E7C6)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Cream,
    background = Color(0xFFFDF6E7),
    onBackground = Ink,
    surface = Color(0xFFFDF6E7),
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Cream,
    onPrimary = Ink,
    background = Color(0xFF121014),
    onBackground = Cream,
    surface = Color(0xFF121014),
    onSurface = Cream,
)

/**
 * Skinless beta theme. Deliberately plain: the real Apple Music inspired look is
 * a later milestone, this just keeps the app readable in light and dark mode.
 */
@Composable
fun MimiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
