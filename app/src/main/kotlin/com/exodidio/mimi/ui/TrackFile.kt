package com.exodidio.mimi.ui

import com.exodidio.mimi.library.Track

/**
 * Where a song sits on the storage, as far as the library knows.
 *
 * MediaStore reports the folder separately from the file name, so the two are joined for
 * anything the listener needs to read as a path. A song without either falls back to the
 * content URI rather than showing a blank line.
 */
fun trackFilePath(track: Track): String {
    val folder = track.folder?.trim()?.trim('/').orEmpty()
    val file = track.fileName?.trim().orEmpty()
    return when {
        folder.isNotEmpty() && file.isNotEmpty() -> "$folder/$file"
        file.isNotEmpty() -> file
        folder.isNotEmpty() -> folder
        else -> track.contentUri
    }
}
