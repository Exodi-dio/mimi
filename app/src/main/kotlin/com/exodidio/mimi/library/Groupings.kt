package com.exodidio.mimi.library

import androidx.room.ColumnInfo

/**
 * One album, summarised from the tracks that belong to it.
 *
 * There is no `albums` table. Albums and artists are *views* over the track cache, so
 * a rescan can throw the whole track list away and rebuild it without ever leaving a
 * stale album row behind.
 */
data class AlbumSummary(
    @ColumnInfo(name = "album_id") val albumId: Long,
    val album: String,
    val artist: String?,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    /** A track in this album, used as the source of the cover art. */
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
)

/** One artist, summarised from the tracks credited to them. */
data class ArtistSummary(
    val artist: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "album_count") val albumCount: Int,
)

/**
 * One genre, summarised from the tracks filed under it.
 *
 * Like albums and artists, this is a view. The difference is that the value came from
 * MediaStore's genre tables rather than from a column of the audio table, so a song with
 * no genre simply belongs to no row here.
 */
data class GenreSummary(
    val genre: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
)

/** One composer, summarised from the tracks credited to them. */
data class ComposerSummary(
    val composer: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
)

/** One release year, summarised from the tracks that carry it. */
data class YearSummary(
    val year: Int,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
)

/**
 * One folder on the device, summarised from the files inside it.
 *
 * [folder] is the path relative to the storage root, e.g. `Music/Albums/Blue`. The whole
 * path is kept rather than just the last segment because two folders called `Live` in
 * different places are not the same folder, and merging them would be a lie.
 */
data class FolderSummary(
    val folder: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
)

/**
 * The albums in these songs, rebuilt in memory.
 *
 * The browse screens derive their rows from the visible songs rather than asking the
 * database for every row it holds. That is what lets a hidden artist or album leave the
 * counts as well as the list: a summary made from the songs that survived the filter
 * cannot keep counting the ones that did not.
 */
fun List<Track>.albumSummaries(): List<AlbumSummary> =
    filter { !it.album.isNullOrBlank() }
        .groupBy { it.albumId }
        .map { (albumId, songs) ->
            AlbumSummary(
                albumId = albumId,
                album = songs.first().album.orEmpty(),
                artist = songs.mapNotNull { it.albumArtist ?: it.artist }.minOrNull(),
                trackCount = songs.size,
                artworkUri = songs.map { it.contentUri }.minOrNull(),
            )
        }
        .sortedBy { it.album.lowercase() }

/** The artists credited across these songs, with hidden credits already removed. */
fun List<Track>.artistSummaries(separators: List<String>): List<ArtistSummary> =
    groupTracksByCredit({ it.artist }, separators)
        .map { (name, songs) ->
            ArtistSummary(
                artist = name,
                trackCount = songs.size,
                albumCount = songs.map { it.albumId }.distinct().size,
            )
        }
        .sortedBy { it.artist.lowercase() }

/** The genres across these songs, split by the configured separators. */
fun List<Track>.genreSummaries(separators: List<String>): List<GenreSummary> =
    groupTracksByCredit({ it.genre }, separators)
        .map { (name, songs) ->
            GenreSummary(
                genre = name,
                trackCount = songs.size,
                artworkUri = songs.map { it.contentUri }.minOrNull(),
            )
        }
        .sortedBy { it.genre.lowercase() }

/** Composers are printed exactly as tagged, so they are not split into credits. */
fun List<Track>.composerSummaries(): List<ComposerSummary> =
    mapNotNull { track -> track.composer?.trim()?.takeIf { it.isNotEmpty() }?.let { it to track } }
        .groupBy({ it.first }, { it.second })
        .map { (name, songs) ->
            ComposerSummary(
                composer = name,
                trackCount = songs.size,
                artworkUri = songs.map { it.contentUri }.minOrNull(),
            )
        }
        .sortedBy { it.composer.lowercase() }

/** Release years, newest first, which is the way the years list already reads. */
fun List<Track>.yearSummaries(): List<YearSummary> =
    mapNotNull { track -> track.year?.let { it to track } }
        .groupBy({ it.first }, { it.second })
        .map { (year, songs) ->
            YearSummary(
                year = year,
                trackCount = songs.size,
                artworkUri = songs.map { it.contentUri }.minOrNull(),
            )
        }
        .sortedByDescending { it.year }

/** Folders, named by their whole path so two folders with one name stay apart. */
fun List<Track>.folderSummaries(): List<FolderSummary> =
    mapNotNull { track -> track.folder?.trim()?.takeIf { it.isNotEmpty() }?.let { it to track } }
        .groupBy({ it.first }, { it.second })
        .map { (folder, songs) ->
            FolderSummary(
                folder = folder,
                trackCount = songs.size,
                artworkUri = songs.map { it.contentUri }.minOrNull(),
            )
        }
        .sortedBy { it.folder.lowercase() }

/**
 * Groups songs under every credit named by [selector].
 *
 * The key is case-folded so an inconsistently tagged library does not grow two rows for
 * one artist, while the spelling shown is the first one encountered. A song is counted
 * once per credit even if its tag repeats a name.
 */
private fun List<Track>.groupTracksByCredit(
    selector: (Track) -> String?,
    separators: List<String>,
): List<Pair<String, List<Track>>> {
    val spellings = LinkedHashMap<String, String>()
    val groups = HashMap<String, MutableList<Track>>()

    forEach { track ->
        splitCredits(selector(track), separators).forEach { credit ->
            val key = credit.lowercase()
            spellings.getOrPut(key) { credit }
            groups.getOrPut(key) { mutableListOf() }.add(track)
        }
    }

    return spellings.map { (key, spelling) -> spelling to groups.getValue(key).toList() }
}
