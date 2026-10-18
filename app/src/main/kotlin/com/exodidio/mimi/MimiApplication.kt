package com.exodidio.mimi

import android.app.Application
import com.exodidio.mimi.library.LibraryRepository
import com.exodidio.mimi.library.MimiDatabase

class MimiApplication : Application() {

    val database: MimiDatabase by lazy { MimiDatabase.get(this) }

    val library: LibraryRepository by lazy { LibraryRepository(this, database) }
}
