package com.exodidio.mimi.ui

import androidx.compose.runtime.compositionLocalOf
import com.exodidio.mimi.library.Settings

/**
 * The settings, for anything that draws.
 *
 * A choice like "show cover art" is read by rows all over the app — songs, albums, artists,
 * playlists, search, and the screens still to come. Handing it down as a parameter would
 * put an argument on every one of those composables to serve a single boolean, and would
 * have to be done again for the next one.
 *
 * Provided once by the shell, so the value is the same everywhere and changing it is a
 * recomposition rather than a rewrite.
 */
val LocalSettings = compositionLocalOf { Settings() }

/**
 * How a screen changes a setting without being handed the whole settings view model.
 *
 * Sorting is offered in list headers all over the app. Threading a change callback through
 * every screen would add an argument to each one for an action that only a sheet uses, and
 * a composition local keeps the sheets and the settings screen writing the same way.
 */
val LocalSettingsChange = compositionLocalOf<((Settings) -> Settings) -> Unit> { {} }
