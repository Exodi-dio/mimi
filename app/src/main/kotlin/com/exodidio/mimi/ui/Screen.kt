package com.exodidio.mimi.ui

/**
 * A place in the app.
 *
 * Detail screens carry only an id or a name, never a copy of the data, so a screen is
 * always reading the current truth from the database instead of a stale snapshot taken
 * when the user tapped.
 */
sealed interface Screen {

    /** What the app opens on: what is new, what was played, what gets played most. */
    data object Home : Screen

    /** The library. Its sections — songs, albums, artists and the rest — are switched inside it. */
    data object Library : Screen

    /** Only the songs marked as loved, which is a place of its own rather than a section. */
    data object Loved : Screen

    data object Search : Screen

    /**
     * One page of settings, reached from the drawer.
     *
     * [reveal] is the place on the page the drawer or the search screen had in mind. It is
     * carried on the screen rather than kept beside it so opening a page and finding the
     * thing that was asked for are one decision: the page scrolls there once, tints the row
     * when it is a row, and then it is just a page again.
     */
    data class SettingsGroup(
        val group: SettingsGroupId,
        val reveal: SettingsReveal? = null,
    ) : Screen

    /** Every setting by name, for someone who knows the word but not the page. */
    data object SettingsSearch : Screen

    /** The name and pictures shown at the top of the drawer. */
    data object Profile : Screen

    /** What the library adds up to, reached from the tools page. */
    data object Statistics : Screen

    /** Charts of the library and of listening, over a period the screen chooses. */
    data object Insights : Screen

    data class AlbumDetail(val albumId: Long) : Screen

    data class ArtistDetail(val artist: String) : Screen

    data class PlaylistDetail(val playlistId: Long) : Screen

    data class GenreDetail(val genre: String) : Screen

    data class ComposerDetail(val composer: String) : Screen

    data class YearDetail(val year: Int) : Screen

    data class FolderDetail(val folder: String) : Screen

    /** Everything known about one song, reached from any row's overflow. */
    data class TrackInfo(val trackId: Long) : Screen
}
