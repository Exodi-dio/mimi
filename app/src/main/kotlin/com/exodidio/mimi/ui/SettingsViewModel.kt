package com.exodidio.mimi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.library.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = (application as MimiApplication).settings

    private val writeLock = Mutex()

    val settings: StateFlow<Settings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings(),
    )

    /**
     * Change one of the settings.
     *
     * The current values are read back before the change is written rather than taken from
     * the last emission, so a screen that has only just opened cannot write stale values
     * over choices made somewhere else. Writes are serialised because each one stores the
     * whole set: two taps in the same instant would otherwise each write a set that knows
     * nothing about the other.
     */
    fun change(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            writeLock.withLock {
                runCatching {
                    val current = store.settings.first()
                    val updated = transform(current)
                    if (updated != current) store.update(updated)
                }
            }
        }
    }
}
