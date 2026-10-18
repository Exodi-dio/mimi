package com.exodidio.mimi

import android.app.Application
import com.exodidio.mimi.library.LibraryRepository
import com.exodidio.mimi.library.MimiDatabase
import com.exodidio.mimi.lyrics.LyricsLoader

class MimiApplication : Application() {

    val database: MimiDatabase by lazy { MimiDatabase.get(this) }

    val library: LibraryRepository by lazy { LibraryRepository(this, database) }

    /**
     * Shared so its cache outlives any one screen: the words for a song are read once
     * and kept, rather than re-read every time the player is opened.
     */
    val lyrics: LyricsLoader by lazy { LyricsLoader(this) }
}
