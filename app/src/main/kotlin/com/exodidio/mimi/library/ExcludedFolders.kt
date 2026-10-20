package com.exodidio.mimi.library

/**
 * Folders the listener has said not to bother with.
 *
 * A folder is named by its path relative to the shared storage, which is what the scanner
 * sees and what the folders slice already shows. Nothing here touches the file system: an
 * excluded folder is simply a folder whose songs are not put in the library, so it stays
 * visible on the device and comes back the moment it is allowed again.
 */

/** Separates the stored paths. A folder name cannot contain a newline. */
private const val FOLDER_SEPARATOR = "\n"

/** The single stored value holding every excluded folder, or empty. */
fun encodeExcludedFolders(folders: Collection<String>): String =
    folders.map { it.trim().trim('/') }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .sorted()
        .joinToString(FOLDER_SEPARATOR)

/** The excluded folders held in [stored]. */
fun decodeExcludedFolders(stored: String?): List<String> =
    stored.orEmpty()
        .split(FOLDER_SEPARATOR)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }

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
