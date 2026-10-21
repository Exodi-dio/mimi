package com.exodidio.mimi.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * One accent, in both the light and the dark arrangement.
 *
 * A colour cannot be a single value for both arrangements: a mid-tone that reads as a
 * colour on white disappears on black, and the ink that reads well on cream is invisible
 * on a dark screen. So each accent carries its light pair and its dark pair, and the
 * theme picks the one the screen is in.
 */
data class Accent(
    /** How it is stored, and how it is spoken about in code. */
    val name: String,
    /** How it is shown to the listener. */
    val label: String,
    val light: Color,
    val onLight: Color,
    val dark: Color,
    val onDark: Color,
)

/**
 * The accents on offer.
 *
 * Ink and cream come first because they are the app's own colours, and a listener who
 * never touches the setting should still be looking at the app rather than at a
 * stranger's palette. The rest are the ones people actually reach for.
 */
val Accents: List<Accent> = listOf(
    Accent(
        name = "ink",
        label = "Ink",
        light = Color(0xFF1E1B20),
        onLight = Color(0xFFF5E7C6),
        dark = Color(0xFFF5E7C6),
        onDark = Color(0xFF1E1B20),
    ),
    Accent(
        name = "blue",
        label = "Blue",
        light = Color(0xFF2F6FED),
        onLight = Color(0xFFFFFFFF),
        dark = Color(0xFF9CC0FF),
        onDark = Color(0xFF10203F),
    ),
    Accent(
        name = "green",
        label = "Green",
        light = Color(0xFF2E7D46),
        onLight = Color(0xFFFFFFFF),
        dark = Color(0xFF9ADFB0),
        onDark = Color(0xFF0C2A16),
    ),
    Accent(
        name = "purple",
        label = "Purple",
        light = Color(0xFF6C4BD1),
        onLight = Color(0xFFFFFFFF),
        dark = Color(0xFFCBB8FF),
        onDark = Color(0xFF221043),
    ),
    Accent(
        name = "orange",
        label = "Orange",
        light = Color(0xFFB4540C),
        onLight = Color(0xFFFFFFFF),
        dark = Color(0xFFFFB77A),
        onDark = Color(0xFF3A1B02),
    ),
    Accent(
        name = "rose",
        label = "Rose",
        light = Color(0xFFB02A55),
        onLight = Color(0xFFFFFFFF),
        dark = Color(0xFFFFB0C6),
        onDark = Color(0xFF3D0A1B),
    ),
    Accent(
        name = "teal",
        label = "Teal",
        light = Color(0xFF00707A),
        onLight = Color(0xFFFFFFFF),
        dark = Color(0xFF82D4DE),
        onDark = Color(0xFF002F35),
    ),
)

/** The accent used when nothing has been chosen, or when the choice is not one we know. */
val DefaultAccent: Accent = Accents.first()

/**
 * The accent named [name], or the default when the name is not one we know.
 *
 * A stored name can be one this version of the app has never heard of — a downgrade, a
 * hand-edited file — and the answer to that is the app's own colour rather than a crash
 * on a screen.
 */
fun accentByName(name: String?): Accent =
    Accents.firstOrNull { it.name == name } ?: DefaultAccent

/**
 * The colour to write on [background].
 *
 * Black on a pale background, cream on a dark one. The cut is at half the perceived
 * brightness rather than at the halfway point of the numbers, because the eye reads
 * lightness, not the three channels.
 */
fun onColourFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF141218) else Color(0xFFF5E7C6)

/**
 * The second colour, for the things an accent should draw attention to without being
 * the loudest thing on the screen.
 *
 * It is the accent itself pulled towards the background, so it belongs to the palette
 * by construction rather than by being picked to match.
 */
fun highlightFor(accent: Color, background: Color): Color {
    val towardsBackground = if (background.luminance() > 0.5f) accent.lightened(0.45f) else accent.darkened(0.35f)
    return towardsBackground
}

/**
 * The dark scheme, with the accent's own colours in it.
 *
 * [amoled] is the same scheme with the backgrounds taken to true black. An OLED screen
 * draws a black pixel as a pixel that is off, so a window of black is a window of no
 * battery at all, which is the whole point of the setting on a phone that is mostly a
 * music player.
 */
fun darkSchemeFor(accent: Accent, amoled: Boolean): androidx.compose.material3.ColorScheme {
    val background = if (amoled) Color(0xFF000000) else Color(0xFF121014)
    return darkColorScheme(
        primary = accent.dark,
        onPrimary = accent.onDark,
        background = background,
        onBackground = Color(0xFFF5E7C6),
        surface = background,
        onSurface = Color(0xFFF5E7C6),
    )
}

/** The light scheme, with the accent's own colours in it. */
fun lightSchemeFor(accent: Accent): androidx.compose.material3.ColorScheme = lightColorScheme(
    primary = accent.light,
    onPrimary = accent.onLight,
    background = Color(0xFFFDF6E7),
    onBackground = Color(0xFF1E1B20),
    surface = Color(0xFFFDF6E7),
    onSurface = Color(0xFF1E1B20),
)

/**
 * A colour pulled towards white, by [fraction] of the way it has left to go.
 *
 * Written here rather than taken from a library because the versions that ship with
 * Compose are not part of its public surface, and a colour scheme that will not compile
 * is not a colour scheme.
 */
private fun Color.lightened(fraction: Float): Color = Color(
    red = red + (1f - red) * fraction,
    green = green + (1f - green) * fraction,
    blue = blue + (1f - blue) * fraction,
    alpha = alpha,
)

/** A colour pulled towards black, by [fraction] of the way there. */
private fun Color.darkened(fraction: Float): Color = Color(
    red = red * (1f - fraction),
    green = green * (1f - fraction),
    blue = blue * (1f - fraction),
    alpha = alpha,
)
