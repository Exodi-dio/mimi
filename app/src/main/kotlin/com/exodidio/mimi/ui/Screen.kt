package com.exodidio.mimi.ui

/**
 * Everywhere the app can be.
 *
 * The five roots are the tabs; everything else is pushed on top of one. Detail screens
 * carry only an id, never a copy of the data — the screen re-reads it, so a rescan
 * cannot leave a stale name on screen.
 */
sealed interface Screen {

    data object Songs : Screen

    data object Albums : Screen

    data object Artists : Screen

    data object Search : Screen

    data object Settings : Screen

    data class AlbumDetail(val albumId: Long) : Screen

    data class ArtistDetail(val artist: String) : Screen
}
