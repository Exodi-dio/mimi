package com.exodidio.mimi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The one warm accent in the app: the play button, the progress fill, the selected tab and
// a loved song. Everything else is a grey, so the accent always means "this is live".
internal val Accent = Color(0xFFFF5C42)

// The near blacks the app is built on. A page is pure black, a card or a sheet is one step
// up, and a control inside a card is one step up again.
internal val PageBlack = Color(0xFF000000)
internal val CardGrey = Color(0xFF1C1C1E)
internal val ControlGrey = Color(0xFF2C2C2E)

// The two text strengths and the hairline. Secondary is the system grey rather than a
// tinted one, so it stays neutral against every cover in the library.
internal val PrimaryText = Color(0xFFFFFFFF)
internal val SecondaryText = Color(0xFF8E8E93)
internal val Hairline = Color(0xFF2C2C2E)

// Light mode keeps the same structure and accent on white, because the theme setting is a
// display choice rather than a different design.
private val LightPage = Color(0xFFFFFFFF)
private val LightCard = Color(0xFFF2F2F7)
private val LightControl = Color(0xFFE5E5EA)
private val LightText = Color(0xFF000000)
private val LightSecondaryText = Color(0xFF6E6E73)

/**
 * The single colour scheme of the app.
 *
 * Every surface, text strength and line in the app comes from here. Screens ask for a role
 * — background, surface, surfaceVariant, outlineVariant, primary — rather than naming a
 * colour, so the whole look moves by changing this file and no screen can drift back to a
 * palette of its own.
 *
 * The dark scheme is the one the app is designed around: black pages, one step up for
 * cards and sheets, white for what is read first and the system grey for what is read
 * second. The primary role is the warm accent, which is why the play button, the progress
 * fill and a loved heart all take it without asking for a colour of their own.
 */
private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Accent,
    onPrimaryContainer = Color.White,
    secondary = SecondaryText,
    onSecondary = PageBlack,
    secondaryContainer = ControlGrey,
    onSecondaryContainer = PrimaryText,
    tertiary = Accent,
    onTertiary = Color.White,
    background = PageBlack,
    onBackground = PrimaryText,
    surface = PageBlack,
    onSurface = PrimaryText,
    surfaceVariant = CardGrey,
    onSurfaceVariant = SecondaryText,
    surfaceContainerLowest = PageBlack,
    surfaceContainerLow = CardGrey,
    surfaceContainer = CardGrey,
    surfaceContainerHigh = ControlGrey,
    surfaceContainerHighest = ControlGrey,
    inverseSurface = PrimaryText,
    inverseOnSurface = PageBlack,
    outline = SecondaryText,
    outlineVariant = Hairline,
    error = Color(0xFFFF453A),
    onError = Color.White,
    scrim = PageBlack,
)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Accent,
    onPrimaryContainer = Color.White,
    secondary = LightSecondaryText,
    onSecondary = LightPage,
    secondaryContainer = LightControl,
    onSecondaryContainer = LightText,
    tertiary = Accent,
    onTertiary = Color.White,
    background = LightPage,
    onBackground = LightText,
    surface = LightPage,
    onSurface = LightText,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightSecondaryText,
    surfaceContainerLowest = LightPage,
    surfaceContainerLow = LightCard,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightControl,
    surfaceContainerHighest = LightControl,
    inverseSurface = LightText,
    inverseOnSurface = LightPage,
    outline = LightSecondaryText,
    outlineVariant = LightControl,
    error = Color(0xFFD70015),
    onError = Color.White,
    scrim = LightText,
)

/**
 * The type: a 34 sp bold large title, a 17 sp bold bar title for a pushed screen, and the
 * rest of the scale underneath it.
 *
 * The sizes are set here rather than at each call site so a song's title is the same size
 * in the songs list, an album and a search result. Weight carries the hierarchy: bold for
 * what a screen is called, regular for everything that is read inside it.
 */
private val MimiTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 34.sp,
        lineHeight = 41.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.2.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleSmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Medium,
    ),
)

/** Corners: cards and covers are gently rounded, sheets are rounded along their top edge. */
private val MimiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * The look of the app, in one place.
 *
 * There is no second skin beside this one: a screen that needs a colour reads a role from
 * the scheme, so light and dark move together and the layout stays the same in both.
 */
@Composable
fun MimiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MimiTypography,
        shapes = MimiShapes,
        content = content,
    )
}
