package com.exodidio.mimi.library

/**
 * Where the library's files live on disk.
 *
 * The library itself knows each song by the address the media provider gave it, but a
 * playlist file holds paths. This is the bridge between the two: one row per song, with
 * the path it can be found at when the provider will say, and the name of the file when
 * it will not.
 */

/** One song of the library, as a playlist file would name it. */
data class LibraryFile(
    val trackId: Long,
    val contentUri: String,
    val path: String? = null,
    val fileName: String? = null,
)

/** A song of a playlist file that was found in the library. */
data class MatchedSong(
    val entry: M3uEntry,
    val trackId: Long,
    val contentUri: String,
)

/**
 * What an imported playlist came to: the songs that were found, and how many were not.
 *
 * The count of what was skipped is kept rather than the list of what was skipped,
 * because a playlist can name hundreds of files that are not here and the listener wants
 * to know how many, not to be shown every one.
 */
data class M3uMatch(
    val matched: List<MatchedSong>,
    val skipped: Int,
)

/**
 * The songs of a playlist file that the library holds.
 *
 * A path is matched exactly first, because that is what the file means. Failing that the
 * name of the file is tried, which is what makes a playlist written on another phone
 * work: the path there is not the path here, but the file is the same file.
 *
 * A name that several songs share is not matched at all. Guessing between them would put
 * the wrong song in the playlist, and a playlist with one song missing is a smaller
 * problem than a playlist that plays the wrong thing.
 */
fun matchM3uEntries(entries: List<M3uEntry>, files: List<LibraryFile>): M3uMatch {
    val byPath = files
        .filter { !it.path.isNullOrBlank() }
        .associateBy { normalisePath(it.path!!) }
    val byName = files
        .filter { !it.fileName.isNullOrBlank() }
        .groupBy { it.fileName!!.lowercase() }

    val matched = mutableListOf<MatchedSong>()
    var skipped = 0

    entries.forEach { entry ->
        val direct = byPath[normalisePath(entry.path)]
        val named = direct ?: byName[fileNameOf(entry.path).lowercase()]?.singleOrNull()
        if (named == null) {
            skipped += 1
        } else {
            matched += MatchedSong(entry = entry, trackId = named.trackId, contentUri = named.contentUri)
        }
    }

    return M3uMatch(matched = matched, skipped = skipped)
}

/**
 * A path as two playlists of the same library would write it.
 *
 * The separators and the case are ignored because a path copied between devices can
 * arrive in either, and the trailing slash says nothing.
 */
fun normalisePath(path: String): String =
    path.trim().replace('\\', '/').trimEnd('/').lowercase()

/** The name of the file a path points at, without the folders in front of it. */
fun fileNameOf(path: String): String =
    normalisePath(path).substringAfterLast('/')
