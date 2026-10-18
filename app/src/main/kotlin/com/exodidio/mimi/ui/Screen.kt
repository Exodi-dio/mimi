package com.exodidio.mimi.ui

/**
 * A place in the app.
 *
 * Detail screens carry only an id or a name, never a copy of the data, so a screen is
 * always reading the current truth from the database instead of a stale snapshot taken
 * when the user tapped.
 */
sealed interface Screen {

    /** The library. Its four sections — songs, albums, artists, playlists — are switched inside it. */
    data object Library : Screen

    /** Only the songs marked as loved, which is a place of its own rather than a section. */
    data object Loved : Screen

    data object Search : Screen

    data object Settings : Screen

    data class AlbumDetail(val albumId: Long) : Screen

    data class ArtistDetail(val artist: String) : Screen

    data class PlaylistDetail(val playlistId: Long) : Screen
}
