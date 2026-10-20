package com.exodidio.mimi

import android.app.Application
import com.exodidio.mimi.library.LibraryRepository
import com.exodidio.mimi.library.MimiDatabase
import com.exodidio.mimi.library.SettingsStore
import com.exodidio.mimi.lyrics.LyricsFinder
import com.exodidio.mimi.lyrics.LyricsLoader
import com.exodidio.mimi.playback.SleepTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MimiApplication : Application() {

    /** Outlives every screen, which is what the sleep timer needs. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: MimiDatabase by lazy { MimiDatabase.get(this) }

    val library: LibraryRepository by lazy { LibraryRepository(this, database) }

    val settings: SettingsStore by lazy { SettingsStore(database.metaDao()) }

    /**
     * Shared so its cache outlives any one screen: the words for a song are read once
     * and kept, rather than re-read every time the player is opened.
     */
    val lyrics: LyricsLoader by lazy { LyricsLoader(this) }

    /**
     * Asks a lyrics database about a song the file itself had nothing for.
     *
     * Held here for the same reason the loader is: it answers slowly and keeps what it
     * has already been told, so it should outlive any one screen.
     */
    val lyricsFinder: LyricsFinder by lazy { LyricsFinder() }

    /** The countdown that stops the music, kept where the screen cannot end it. */
    val sleepTimer: SleepTimer by lazy { SleepTimer(scope) }
}
