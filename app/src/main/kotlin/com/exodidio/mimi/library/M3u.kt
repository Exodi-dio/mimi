package com.exodidio.mimi.library

/**
 * The M3U playlist format, as plain text.
 *
 * An M3U file is a list of file paths, one per line, each optionally preceded by an
 * `#EXTINF` header saying how long the song is and what it is called. It is the one
 * playlist format every player understands, which is why a playlist made here can be
 * handed to another app and a playlist made elsewhere can be brought in here.
 *
 * The text is built and read by plain functions, so the format can be tested without a
 * device: what goes into a file and what comes back out of one is decided here and
 * nowhere else.
 */

/** One line of a playlist: where the file is, and what the playlist called it. */
data class M3uEntry(
    val path: String,
    val title: String? = null,
    val artist: String? = null,
    val seconds: Int? = null,
)

/**
 * A song as it is written into a playlist.
 *
 * The path is the only part that cannot be invented, so it is required: a playlist entry
 * without a file behind it would be a line that points at nothing.
 */
data class M3uSong(
    val path: String,
    val title: String,
    val artist: String? = null,
    val durationMs: Long = 0L,
)

/**
 * The text of a playlist, in the form other players read.
 *
 * Every song gets an `#EXTINF` header carrying its length and the name it is listed
 * under, then its path on the line below. The header is what a player shows in its list;
 * the path is what it plays.
 *
 * The length is written in whole seconds because that is what the format counts in, and
 * a song of unknown length is written as zero rather than left out: the header has to be
 * there for the entry to be read at all.
 */
fun buildM3u(songs: List<M3uSong>): String = buildString {
    append("#EXTM3U\n")
    songs.forEach { song ->
        val seconds = (song.durationMs / 1_000L).coerceAtLeast(0L)
        append("#EXTINF:")
        append(seconds)
        append(',')
        append(playlistDisplayName(song.artist, song.title))
        append('\n')
        append(song.path)
        append('\n')
    }
}

/**
 * The name a playlist lists a song under.
 *
 * The format's convention is the artist and the title joined by a dash, and a song with
 * no artist is listed under its title alone rather than under a leading dash.
 */
fun playlistDisplayName(artist: String?, title: String): String {
    val name = artist?.trim().orEmpty()
    val song = title.trim()
    return if (name.isEmpty()) song else "$name - $song"
}

/**
 * The songs named by a playlist, in the order it lists them.
 *
 * A file may open with `#EXTM3U` and carry directives this app has no use for, and it
 * may list songs with no header at all. Both are read: a path on a line of its own is a
 * song, and the header before it, if there was one, says what it is called.
 *
 * The header belongs to the path below it and to nothing else, so a header with no path
 * after it is forgotten rather than being attached to the next song.
 */
fun parseM3u(text: String): List<M3uEntry> {
    val entries = mutableListOf<M3uEntry>()
    var header: M3uHeader? = null

    text.lineSequence().forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty()) return@forEach

        when {
            line.startsWith(EXTINF_PREFIX, ignoreCase = true) -> {
                header = parseExtInf(line.substringAfter(':'))
            }
            line.startsWith("#") -> Unit
            else -> {
                entries += M3uEntry(
                    path = line,
                    title = header?.title,
                    artist = header?.artist,
                    seconds = header?.seconds,
                )
                header = null
            }
        }
    }

    return entries
}

/** The header that carries a song's length and name, read but not yet attached. */
private data class M3uHeader(val title: String?, val artist: String?, val seconds: Int?)

/** The prefix that marks the header of a song. */
private const val EXTINF_PREFIX = "#EXTINF"

/**
 * The header of one song: a length in seconds and a name.
 *
 * The name is the artist and the title joined by a dash, which is how the format spells
 * it. A name with no dash in it is a title on its own, because a song listed without an
 * artist is ordinary enough.
 */
private fun parseExtInf(body: String): M3uHeader {
    val seconds = body.substringBefore(',').trim().toIntOrNull()?.takeIf { it > 0 }
    val display = body.substringAfter(',', "").trim()
    if (display.isEmpty()) return M3uHeader(title = null, artist = null, seconds = seconds)

    val parts = display.split(" - ", limit = 2)
    return if (parts.size == 2) {
        M3uHeader(
            title = parts[1].trim().ifEmpty { null },
            artist = parts[0].trim().ifEmpty { null },
            seconds = seconds,
        )
    } else {
        M3uHeader(title = display.ifEmpty { null }, artist = null, seconds = seconds)
    }
}
