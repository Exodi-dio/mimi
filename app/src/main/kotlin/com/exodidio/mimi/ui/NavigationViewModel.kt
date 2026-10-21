package com.exodidio.mimi.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** The sections of the library, in the order the switcher shows them. */
enum class LibrarySection(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    GENRES("Genres"),
    COMPOSERS("Composers"),
    YEARS("Years"),
    FOLDERS("Folders"),
}

/**
 * Where the user is.
 *
 * A stack rather than a navigation library: the whole app is a handful of screens and a
 * skin will want to rearrange them freely, so keeping the rules here — visible and
 * small — beats configuring someone else's graph.
 *
 * The library's sections are one destination, not four, because switching between songs
 * and albums is browsing the same collection rather than going somewhere new. Only real
 * moves — opening an album, opening a playlist — are pushed onto the stack.
 */
class NavigationViewModel : ViewModel() {

    var stack by mutableStateOf<List<Screen>>(listOf(Screen.Home))
        private set

    var librarySection by mutableStateOf(LibrarySection.SONGS)

    /**
     * Where each library section was left, keyed by section and the order it was shown in.
     *
     * Held here because the screens are rebuilt when a detail screen opens; a value kept
     * in the screen would be gone exactly when returning to the list needs it. A plain map
     * rather than snapshot state: the position is read once when a list is rebuilt, and
     * making every scroll tick a recomposition would cost more than it saves.
     */
    private val scrollPositions = HashMap<String, Int>()

    /** The first visible row remembered for [key], or the top of the list. */
    fun scrollIndex(key: String): Int = scrollPositions[key] ?: 0

    /** Remembers the first visible row for a section that is being scrolled. */
    fun rememberScroll(key: String, index: Int) {
        scrollPositions[key] = index
    }

    val current: Screen get() = stack.last()

    /** Which tab is lit. A detail screen keeps its parent tab lit while it is open. */
    val rootTab: Screen get() = stack.first()

    val canGoBack: Boolean get() = stack.size > 1

    fun selectTab(screen: Screen) {
        if (stack.first() == screen && stack.size == 1) return
        stack = listOf(screen)
    }

    /**
     * Switching section while the library is already open leaves the stack alone, so the
     * back button never walks through sections the user only glanced at.
     */
    fun selectSection(section: LibrarySection) {
        librarySection = section
        if (stack.size == 1 && stack.first() == Screen.Library) return
        stack = listOf(Screen.Library)
    }

    fun push(screen: Screen) {
        stack = stack + screen
    }

    /**
     * Opens a destination from the drawer without leaving a trail behind it.
     *
     * Walking the drawer is one decision after another, so a settings page that is already
     * open is replaced rather than stacked on top of itself. The back button then returns
     * to the list the drawer was opened from instead of walking back through every page
     * glanced at on the way.
     */
    fun openFromDrawer(screen: Screen) {
        val settingsOpen = current is Screen.SettingsGroup ||
            current is Screen.SettingsSearch ||
            current is Screen.Profile
        stack = if (settingsOpen && stack.size > 1) {
            stack.dropLast(1) + screen
        } else {
            stack + screen
        }
    }

    fun pop() {
        if (canGoBack) stack = stack.dropLast(1)
    }

    /**
     * Leaves a deleted playlist. Popping back would land on a detail screen for something
     * that no longer exists, so the stack is cut back to the library instead.
     */
    fun popToLibrary() {
        librarySection = LibrarySection.PLAYLISTS
        stack = listOf(Screen.Library)
    }
}
