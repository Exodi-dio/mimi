package com.exodidio.mimi.library

/**
 * Folders the listener has said not to bother with.
 *
 * A folder is named by its path relative to the shared storage, which is what the scanner
 * sees and what the folders slice already shows. Nothing here touches the file system: an
 * excluded folder is simply a folder whose songs are not put in the library, so it stays
 * visible on the device and comes back the moment it is allowed again.
 */

/**
 * Separates the values in a stored list. Shared with the credit separators, which are
 * stored the same way, so one decoder reads both and neither format can drift.
 */
internal const val STORED_LIST_SEPARATOR = "\n"

/**
 * A list as one stored value, in the order it was given.
 *
 * This is the general form the settings use for anything list-shaped: separators, playlists
 * ranges, the order of tabs. Order is kept rather than normalised because some of those
 * lists mean something by it — the order of the bottom tabs is a choice, not a set.
 */
fun encodeStoredList(values: Collection<String>): String =
    values.map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(STORED_LIST_SEPARATOR)

/** The values held in [stored], in the order they were written. */
fun decodeStoredList(stored: String?): List<String> =
    stored.orEmpty()
        .split(STORED_LIST_SEPARATOR)
        .map { it.trim() }
        .filter { it.isNotEmpty() }

/**
 * The single stored value holding every excluded folder, or empty.
 *
 * Folders are sorted and deduplicated because a folder belongs to a set: two rows naming
 * the same folder twice would mean nothing, and the same set should always be written the
 * same way, so a stored value can be compared with one that was just built.
 */
fun encodeExcludedFolders(folders: Collection<String>): String =
    encodeStoredList(
        folders.map { it.trim().trim('/') }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sorted()
    )

/** The excluded folders held in [stored]. */
fun decodeExcludedFolders(stored: String?): List<String> =
    decodeStoredList(stored).distinctBy { it.lowercase() }

/**
 * Whether [folder] is under one of [excluded].
 *
 * A folder is excluded by naming it, and everything inside it goes with it — excluding
 * "Music/Ringtones" has to cover "Music/Ringtones/Old" too, or the listener would have to
 * name every subfolder they have. The comparison respects path boundaries so that
 * "Music/Ringtone" is not excluded by "Music/Ringtones".
 *
 * Case is ignored because the storage may be case-insensitive, and a folder that is
 * matched on one phone and not another would be a confusing thing to own.
 */
fun isExcludedFolder(folder: String?, excluded: Collection<String>): Boolean {
    if (folder.isNullOrBlank() || excluded.isEmpty()) return false

    val path = folder.trim().trim('/').lowercase()
    return excluded.any { rule ->
        val wanted = rule.trim().trim('/').lowercase()
        wanted.isNotEmpty() && (path == wanted || path.startsWith("$wanted/"))
    }
}
