package com.exodidio.mimi.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * A plain back stack, held in a ViewModel so it survives configuration changes.
 *
 * The app is small enough that a navigation library would be more machinery than it is
 * worth, and owning the stack outright means the eventual skin can restructure the
 * whole shell without fighting anyone else's graph.
 */
class NavigationViewModel : ViewModel() {

    var stack by mutableStateOf<List<Screen>>(listOf(Screen.Songs))
        private set

    val current: Screen get() = stack.last()

    /** Which tab stays lit while detail screens are open on top of it. */
    val rootTab: Screen get() = stack.first()

    val canGoBack: Boolean get() = stack.size > 1

    /** Selecting a tab abandons whatever was pushed on top of the previous one. */
    fun selectTab(screen: Screen) {
        if (stack.first() == screen && stack.size == 1) return
        stack = listOf(screen)
    }

    fun push(screen: Screen) {
        stack = stack + screen
    }

    fun pop() {
        if (canGoBack) stack = stack.dropLast(1)
    }
}
