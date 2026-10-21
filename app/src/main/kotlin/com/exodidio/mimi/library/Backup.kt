package com.exodidio.mimi.library

/**
 * The backup file: plain text, one record per line.
 *
 * A backup holds what the listener made rather than what the device already has: the
 * playlists they put together, the choices they made about how the app behaves, and how
 * much they have listened to what. The songs themselves are not in it, because the songs
 * are files on the device and a backup that copied them would be a copy of the music
 * rather than of the library.
 *
 * The format is a line-based text file rather than something binary so that a backup can
 * be read by eye, opened in any text editor and repaired by hand if it is ever damaged.
 * The first line says which version of the format the file is, and a file from a newer
 * version is refused rather than half-read.
 */

/** The version of the format this build writes and reads. */
const val BACKUP_VERSION = 1

/** The words the first line of a backup begins with. */
const val BACKUP_HEADER = "MIMI BACKUP"

/** One playlist in a backup, with the songs it holds in the order they were listed. */
data class BackupPlaylist(
    val name: String,
    val entries: List<BackupEntry> = emptyList(),
)

/**
 * One song of a playlist in a backup.
 *
 * The song is named by the address the media provider gave it rather than by a number
 * the device happens to use, because those numbers mean nothing on another phone. The
 * name of the file is kept as well, so a playlist can still be put back together on a
 * device where the addresses are different but the music is the same.
 */
data class BackupEntry(
    val contentUri: String,
    val position: Int,
    val fileName: String? = null,
)

/** How much a song has been played. */
data class BackupHistory(
    val contentUri: String,
    val playCount: Int,
    val skipCount: Int,
    val lastPlayedAt: Long? = null,
)

/** How long a song was heard on one day. */
data class BackupListenDay(
    val contentUri: String,
    val day: String,
    val listenedMs: Long,
    val plays: Int,
)

/**
 * Everything a backup file holds.
 *
 * The settings are kept as the rows they are stored as, so a backup carries the choices
 * exactly as they were written and restoring them is the same operation as saving them.
 */
data class BackupData(
    val createdAt: Long,
    val settings: List<MetaEntry> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList(),
    val history: List<BackupHistory> = emptyList(),
    val listenDays: List<BackupListenDay> = emptyList(),
)

/** What reading a backup file came to. */
sealed interface BackupFile {

    /** The file was read, and this is what it holds. */
    data class Read(val backup: BackupData) : BackupFile

    /** The file was written by a newer build, so this one cannot be trusted to read it. */
    data class UnsupportedVersion(val version: Int) : BackupFile

    /** The file stops in the middle, so what is there cannot be trusted either. */
    data class Truncated(val reason: String) : BackupFile

    /** The file is not a backup at all. */
    data class Malformed(val reason: String) : BackupFile
}

/**
 * What a restore managed to do.
 *
 * A restore merges rather than replaces, so some of it can always be applied and some of
 * it cannot: a song the backup names may not be on this device, and a folder the backup
 * remembers belongs to the device it was made on. The report says which of the two
 * happened rather than pretending the restore was complete.
 */
data class RestoreReport(
    val playlistsCreated: Int = 0,
    val playlistsMerged: Int = 0,
    val songsRestored: Int = 0,
    val songsSkipped: Int = 0,
    val settingsApplied: Int = 0,
    val settingsSkipped: Int = 0,
    val historyRestored: Int = 0,
    val daysRestored: Int = 0,
    val notes: List<String> = emptyList(),
) {
    /**
     * The one paragraph the listener is shown.
     *
     * What was applied comes first and what could not be applied last, because the first
     * is what the listener asked for and the second is what they need to know about.
     */
    val summary: String
        get() = buildString {
            val playlists = playlistsCreated + playlistsMerged
            append(
                when {
                    playlists == 0 -> "No playlists were restored."
                    playlists == 1 -> "1 playlist was restored."
                    else -> "$playlists playlists were restored."
                }
            )
            if (playlistsCreated > 0 || playlistsMerged > 0) {
                append(" ($playlistsCreated new, $playlistsMerged merged)")
            }
            append(" ")
            append(
                when (songsRestored) {
                    0 -> "No songs were added."
                    1 -> "1 song was added."
                    else -> "$songsRestored songs were added."
                }
            )
            if (settingsApplied > 0) {
                append(" $settingsApplied settings were applied.")
            }
            if (historyRestored > 0 || daysRestored > 0) {
                append(" $historyRestored songs' history and $daysRestored days of listening were merged.")
            }
            if (songsSkipped > 0) {
                append(" $songsSkipped songs in the backup are not in this library.")
            }
            if (settingsSkipped > 0) {
                append(" $settingsSkipped settings were left alone because they name a folder on another device.")
            }
            notes.forEach { note ->
                append(" ")
                append(note)
            }
        }
}

/**
 * What came of putting a backup file back.
 *
 * A file that cannot be trusted is refused rather than half-read, and the reason is
 * worth saying out loud: a restore that quietly did nothing would look exactly like a
 * restore that worked.
 */
sealed interface RestoreOutcome {

    /** The backup was read and merged into the library. */
    data class Done(val report: RestoreReport) : RestoreOutcome

    /** The file could not be used, and this is why. */
    data class Refused(val message: String) : RestoreOutcome
}

/**
 * How long an automatic backup waits before the next one is written.
 *
 * A week is seven days and a month is thirty: a calendar month would need the day the
 * backup was made, which is not what a listener means by "monthly" when they mean "not
 * every day".
 */
fun backupIntervalMillis(frequency: String): Long = when (frequency) {
    Settings.BACKUP_WEEKLY -> 7L * DAY_MS
    Settings.BACKUP_MONTHLY -> 30L * DAY_MS
    else -> DAY_MS
}

/** A day in milliseconds, which is the shortest gap between two automatic backups. */
private const val DAY_MS = 24L * 60L * 60L * 1000L

/**
 * The name an automatic backup is written under.
 *
 * The day is part of the name so that a folder of backups reads as a history rather than
 * as one file that keeps being overwritten.
 */
fun backupFileName(at: Long): String {
    val day = java.time.Instant.ofEpochMilli(at)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .toString()
    return "mimi-backup-$day.txt"
}

/**
 * The text of a backup, as it is written to a file.
 *
 * Each section says how many lines follow it, so a reader knows what it should find and
 * can tell a file that stops early from one that is complete. The last line is `END`,
 * which is the reader's proof that the whole file arrived.
 */
fun buildBackup(backup: BackupData): String = buildString {
    append(BACKUP_HEADER).append(' ').append(BACKUP_VERSION).append('\n')
    append("CREATED ").append(backup.createdAt).append('\n')

    append("SETTINGS ").append(backup.settings.size).append('\n')
    backup.settings.forEach { entry ->
        append("s\t").append(escapeField(entry.key)).append('\t')
            .append(escapeField(entry.value)).append('\n')
    }

    append("PLAYLISTS ").append(backup.playlists.size).append('\n')
    backup.playlists.forEach { playlist ->
        append("p\t").append(escapeField(playlist.name)).append('\t')
            .append(playlist.entries.size).append('\n')
        playlist.entries.forEach { entry ->
            append("e\t").append(escapeField(entry.contentUri)).append('\t')
                .append(entry.position).append('\t')
                .append(escapeField(entry.fileName.orEmpty())).append('\n')
        }
    }

    append("HISTORY ").append(backup.history.size).append('\n')
    backup.history.forEach { stat ->
        append("h\t").append(escapeField(stat.contentUri)).append('\t')
            .append(stat.playCount).append('\t').append(stat.skipCount).append('\t')
            .append(stat.lastPlayedAt ?: 0L).append('\n')
    }

    append("DAYS ").append(backup.listenDays.size).append('\n')
    backup.listenDays.forEach { day ->
        append("d\t").append(escapeField(day.contentUri)).append('\t')
            .append(escapeField(day.day)).append('\t').append(day.listenedMs)
            .append('\t').append(day.plays).append('\n')
    }

    append("END\n")
}

/**
 * A backup file, read.
 *
 * A file that names a version this build does not know is refused whole: reading half of
 * a format that may have changed is how a restore quietly loses what it was given. A
 * file that stops early is refused for the same reason.
 */
fun parseBackup(text: String): BackupFile = try {
    readBackup(text)
} catch (problem: BackupProblem) {
    problem.file
}

/** The reading itself, which gives up by throwing when the file cannot be trusted. */
private fun readBackup(text: String): BackupFile {
    val reader = BackupReader(text.split('\n').map { it.trimEnd('\r') })

    val header = reader.next()?.trim().orEmpty()
    if (!header.startsWith("$BACKUP_HEADER ")) {
        throw BackupProblem(BackupFile.Malformed("This file is not a MIMI backup."))
    }
    val version = header.substringAfter("$BACKUP_HEADER ").trim().toIntOrNull()
        ?: throw BackupProblem(BackupFile.Malformed("The backup does not say which version it is."))
    if (version != BACKUP_VERSION) {
        throw BackupProblem(BackupFile.UnsupportedVersion(version))
    }

    val created = reader.next()?.trim().orEmpty()
    if (!created.startsWith("CREATED ")) {
        throw BackupProblem(BackupFile.Truncated("The backup stops before it says when it was made."))
    }
    val createdAt = created.substringAfter("CREATED ").trim().toLongOrNull()
        ?: throw BackupProblem(BackupFile.Malformed("The backup does not say when it was made."))

    val settings = reader.section("SETTINGS") { line ->
        val parts = line.split('\t')
        if (parts.size < 3 || parts[0] != "s") {
            null
        } else {
            MetaEntry(unescapeField(parts[1]), unescapeField(parts.drop(2).joinToString("\t")))
        }
    }

    val playlists = reader.playlists()

    val history = reader.section("HISTORY") { line ->
        val parts = line.split('\t')
        if (parts.size < 4 || parts[0] != "h") {
            null
        } else {
            BackupHistory(
                contentUri = unescapeField(parts[1]),
                playCount = parts[2].trim().toIntOrNull() ?: 0,
                skipCount = parts[3].trim().toIntOrNull() ?: 0,
                lastPlayedAt = parts.getOrNull(4)?.trim()?.toLongOrNull()?.takeIf { it > 0L },
            )
        }
    }

    val days = reader.section("DAYS") { line ->
        val parts = line.split('\t')
        if (parts.size < 5 || parts[0] != "d") {
            null
        } else {
            BackupListenDay(
                contentUri = unescapeField(parts[1]),
                day = unescapeField(parts[2]),
                listenedMs = parts[3].trim().toLongOrNull() ?: 0L,
                plays = parts[4].trim().toIntOrNull() ?: 0,
            )
        }
    }

    if (reader.next()?.trim() != "END") {
        throw BackupProblem(BackupFile.Truncated("The backup stops before it ends."))
    }

    return BackupFile.Read(
        BackupData(
            createdAt = createdAt,
            settings = settings,
            playlists = playlists,
            history = history,
            listenDays = days,
        )
    )
}

/** A file that cannot be trusted, carried out of the reading as a value. */
private class BackupProblem(val file: BackupFile) : Exception()

/**
 * A line-by-line reader over a backup file.
 *
 * Each section says how many lines it holds, so the reader can tell at once whether the
 * file really holds them: a section that claims more lines than are there is a file
 * that was cut short, not a file to be read as far as it goes.
 */
private class BackupReader(private val lines: List<String>) {

    private var index = 0

    /** The next line of the file, or null when the file has run out. */
    fun next(): String? = lines.getOrNull(index)?.also { index += 1 }

    /**
     * The lines of the section named [name], read with [read].
     *
     * A line the reader cannot make sense of makes the whole file unusable rather than
     * being passed over: half a playlist is worse than none, because it looks complete.
     */
    fun <T> section(name: String, read: (String) -> T?): List<T> {
        val header = next()?.trim().orEmpty()
        if (!header.startsWith("$name ")) {
            throw BackupProblem(BackupFile.Truncated("The backup stops before its $name section."))
        }
        val count = header.substringAfter("$name ").trim().toIntOrNull()
            ?: throw BackupProblem(
                BackupFile.Malformed("The backup does not say how many $name it holds.")
            )
        if (count < 0) {
            throw BackupProblem(
                BackupFile.Malformed("The backup says it holds a negative number of $name.")
            )
        }

        val values = mutableListOf<T>()
        repeat(count) {
            val line = next()
                ?: throw BackupProblem(BackupFile.Truncated("The backup stops in the middle of its $name."))
            val value = read(line)
                ?: throw BackupProblem(BackupFile.Malformed("A line of the $name cannot be read."))
            values += value
        }
        return values
    }

    /**
     * The playlists of the file, each with the songs it holds.
     *
     * This section is read on its own rather than through [section] because its lines are
     * not one for one: a playlist is a line of its own followed by a line for each of its
     * songs, and the counts are what say where one playlist ends and the next begins.
     */
    fun playlists(): List<BackupPlaylist> {
        val header = next()?.trim().orEmpty()
        if (!header.startsWith("PLAYLISTS ")) {
            throw BackupProblem(BackupFile.Truncated("The backup stops before its playlists."))
        }
        val count = header.substringAfter("PLAYLISTS ").trim().toIntOrNull()
            ?: throw BackupProblem(BackupFile.Malformed("The backup does not say how many playlists it holds."))
        if (count < 0) {
            throw BackupProblem(BackupFile.Malformed("The backup says it holds a negative number of playlists."))
        }

        val playlists = mutableListOf<BackupPlaylist>()
        repeat(count) {
            val line = next()
                ?: throw BackupProblem(BackupFile.Truncated("The backup stops in the middle of its playlists."))
            val parts = line.split('\t')
            if (parts.size < 3 || parts[0] != "p") {
                throw BackupProblem(BackupFile.Malformed("A playlist of the backup cannot be read."))
            }
            val entries = parts[2].trim().toIntOrNull()
                ?: throw BackupProblem(BackupFile.Malformed("A playlist of the backup cannot be read."))

            val songs = mutableListOf<BackupEntry>()
            repeat(entries) {
                val song = next()
                    ?: throw BackupProblem(BackupFile.Truncated("The backup stops in the middle of a playlist."))
                val songParts = song.split('\t')
                if (songParts.size < 3 || songParts[0] != "e") {
                    throw BackupProblem(BackupFile.Malformed("A song of a playlist cannot be read."))
                }
                songs += BackupEntry(
                    contentUri = unescapeField(songParts[1]),
                    position = songParts[2].trim().toIntOrNull() ?: 0,
                    fileName = songParts.getOrNull(3)?.let(::unescapeField)?.ifBlank { null },
                )
            }
            playlists += BackupPlaylist(name = unescapeField(parts[1]), entries = songs)
        }
        return playlists
    }
}

/**
 * A field with the marks that would break a line written out of it.
 *
 * A stored list of settings holds its items separated by line breaks, so a value can
 * easily contain the very characters the format uses to separate its fields.
 */
private fun escapeField(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\t", "\\t")
    .replace("\n", "\\n")
    .replace("\r", "\\r")

/** A field written out by [escapeField], read back as it was. */
private fun unescapeField(value: String): String {
    val out = StringBuilder(value.length)
    var position = 0

    while (position < value.length) {
        val mark = value[position]
        if (mark != '\\' || position + 1 >= value.length) {
            out.append(mark)
            position += 1
            continue
        }
        val next = value[position + 1]
        when (next) {
            't' -> out.append('\t')
            'n' -> out.append('\n')
            'r' -> out.append('\r')
            '\\' -> out.append('\\')
            else -> {
                out.append(mark)
                out.append(next)
            }
        }
        position += 2
    }

    return out.toString()
}
