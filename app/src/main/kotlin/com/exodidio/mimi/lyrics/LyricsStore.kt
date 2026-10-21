package com.exodidio.mimi.lyrics

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Words that were found rather than read out of the file.
 *
 * A tag cannot be written back into someone's music, so a lyric the listener picked has to
 * live somewhere else. One small file per song, named after the song's own key, keeps it
 * out of the library database (which is wiped and rebuilt on every scan) and out of the
 * settings table (which is for choices, not for kilobytes of text).
 *
 * A file rather than a row because lyric text runs to several kilobytes and a table built
 * for key/value settings would carry it badly. Reading is a single small file, which is
 * cheaper than the tag read that came before it.
 */
class LyricsStore(private val context: Context) {

    /** The words kept for [key], or nothing when none were kept. */
    suspend fun load(key: String): Lyrics = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileFor(key)
            if (!file.isFile) return@runCatching Lyrics.EMPTY
            LyricsParser.parse(file.readText())
        }.getOrDefault(Lyrics.EMPTY)
    }

    /** Keeps [content] for [key], replacing anything kept before. */
    suspend fun keep(key: String, content: String) = withContext(Dispatchers.IO) {
        runCatching { fileFor(key).writeText(content) }
        Unit
    }

    /** Drops what was kept for [key], if anything was. */
    fun forget(key: String) {
        runCatching { fileFor(key).delete() }
    }

    private fun fileFor(key: String): File {
        // Keys are content uris, so the name is bounded and stripped of anything a
        // filesystem would object to. Long keys are cut rather than allowed to fail.
        val name = key.replace(UNSAFE, "_").take(MAX_NAME_LENGTH)
        val folder = File(context.filesDir, "lyrics")
        if (!folder.isDirectory) folder.mkdirs()
        return File(folder, "$name.lrc")
    }

    private companion object {
        val UNSAFE = Regex("[^A-Za-z0-9._-]")
        const val MAX_NAME_LENGTH = 120
    }
}
