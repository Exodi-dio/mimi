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

    var stack by mutableStateOf<List<Screen>>(listOf(Screen.Library))
        private set

    var librarySection by mutableStateOf(LibrarySection.SONGS)

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
