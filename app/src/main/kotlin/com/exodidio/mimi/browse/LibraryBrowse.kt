package com.exodidio.mimi.browse

import com.exodidio.mimi.library.splitCredits

/**
 * The library as a tree of shelves: the songs, the albums and the artists.
 *
 * The shelves are worked out from the songs rather than kept in a table of their own, so
 * a shelf cannot fall out of step with the songs that are in it. The ids are the tree's
 * own words: a song is named by the media store uri, which is what the player knows it
 * by, and a shelf is named by what it holds, so an id says what it is instead of being a
 * number to be looked up somewhere else.
 *
 * The artist shelves are split at the same separators the library uses, so a duet is one
 * song under two artists rather than one song under a name nobody has heard of.
 */
class LibraryBrowse(
    private val songs: List<BrowseSong>,
    artistSeparators: List<String> = emptyList(),
) {

    /** The separators the artist credits are split at. */
    private val separators = artistSeparators

    /** The root of the tree, which a browser asks for before anything else. */
    fun root(): BrowseEntry = BrowseEntry(
        mediaId = ROOT_ID,
        title = "MIMI",
        artist = null,
        album = null,
        artworkUri = null,
        browsable = true,
        playable = false,
    )

    /** The shelves under the root: the three ways into the library. */
    fun rootChildren(): List<BrowseEntry> = listOf(
        shelf(SONGS_ID, "Songs"),
        shelf(ALBUMS_ID, "Albums"),
        shelf(ARTISTS_ID, "Artists"),
    )

    /**
     * What is under [parentId].
     *
     * An id the tree does not hold has nothing under it rather than an error, because a
     * browser is allowed to ask for something that has since gone from the library.
     */
    fun childrenOf(parentId: String): List<BrowseEntry> = when {
        parentId == ROOT_ID -> rootChildren()
        parentId == SONGS_ID -> songs.map { it.toEntry() }
        parentId == ALBUMS_ID -> albumEntries()
        parentId == ARTISTS_ID -> artistEntries()
        parentId.startsWith(ALBUM_PREFIX) -> songsOfAlbum(parentId.removePrefix(ALBUM_PREFIX))
            .map { it.toEntry() }

        parentId.startsWith(ARTIST_PREFIX) -> songsOfArtist(parentId.removePrefix(ARTIST_PREFIX))
            .map { it.toEntry() }

        else -> emptyList()
    }

    /** The song with [mediaId], or null when the library does not hold it. */
    fun song(mediaId: String): BrowseSong? = songs.firstOrNull { it.mediaId == mediaId }

    /**
     * The entry with [mediaId], whether it is a shelf or a song.
     *
     * A browser that asks for one thing by its id gets that thing: the root, one of the
     * shelves under it, an album, an artist or a song.
     */
    fun entry(mediaId: String): BrowseEntry? = when {
        mediaId == ROOT_ID -> root()
        mediaId == SONGS_ID || mediaId == ALBUMS_ID || mediaId == ARTISTS_ID ->
            rootChildren().firstOrNull { it.mediaId == mediaId }

        mediaId.startsWith(ALBUM_PREFIX) -> albumEntries().firstOrNull { it.mediaId == mediaId }
        mediaId.startsWith(ARTIST_PREFIX) -> artistEntries().firstOrNull { it.mediaId == mediaId }
        else -> song(mediaId)?.toEntry()
    }

    /** A shelf with nothing on it but a name. */
    private fun shelf(id: String, title: String): BrowseEntry = BrowseEntry(
        mediaId = id,
        title = title,
        artist = null,
        album = null,
        artworkUri = null,
        browsable = true,
        playable = false,
    )

    /**
     * The albums in the library, one row each.
     *
     * The songs are grouped by the album's id rather than by its name, because two
     * records can share a name and still be two records. The name shown is the first
     * one the songs agree on.
     */
    private fun albumEntries(): List<BrowseEntry> = songs
        .groupBy { it.albumId }
        .map { (albumId, group) ->
            BrowseEntry(
                mediaId = ALBUM_PREFIX + albumId,
                title = group.firstOrNull { !it.album.isNullOrBlank() }?.album ?: "Unknown album",
                artist = group.mapNotNull { it.artist }.minOrNull(),
                album = null,
                artworkUri = group.firstNotNullOfOrNull { it.artworkUri },
                browsable = true,
                playable = false,
            )
        }
        .sortedBy { it.title.lowercase() }

    /**
     * The artists in the library, one row each.
     *
     * The names are folded together by their letters rather than by their spelling, so a
     * library that is inconsistent about capitalisation does not grow two rows for one
     * artist, and the spelling shown is the first one that was met.
     */
    private fun artistEntries(): List<BrowseEntry> {
        val grouped = LinkedHashMap<String, Pair<String, MutableList<BrowseSong>>>()
        for (song in songs) {
            for (credit in splitCredits(song.artist, separators)) {
                val key = credit.lowercase()
                val known = grouped[key]
                if (known == null) {
                    grouped[key] = credit to mutableListOf(song)
                } else {
                    known.second.add(song)
                }
            }
        }
        return grouped.values
            .map { (name, group) ->
                BrowseEntry(
                    mediaId = ARTIST_PREFIX + name,
                    title = name,
                    artist = null,
                    album = null,
                    artworkUri = group.firstNotNullOfOrNull { it.artworkUri },
                    browsable = true,
                    playable = false,
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    /** The songs of one album, in the order the record runs them. */
    private fun songsOfAlbum(albumId: String): List<BrowseSong> = songs
        .filter { it.albumId.toString() == albumId }
        .sortedWith(compareBy({ it.trackNumber }, { it.title.lowercase() }))

    /** The songs that name [name] in their credit. */
    private fun songsOfArtist(name: String): List<BrowseSong> = songs.filter { song ->
        splitCredits(song.artist, separators).any { it.equals(name.trim(), ignoreCase = true) }
    }

    /** The song as an entry of the tree. */
    private fun BrowseSong.toEntry(): BrowseEntry = BrowseEntry(
        mediaId = mediaId,
        title = title,
        artist = artist,
        album = album,
        artworkUri = artworkUri,
        browsable = false,
        playable = true,
        durationMs = durationMs,
    )

    companion object {
        /** The id of the tree itself, which a browser asks for first. */
        const val ROOT_ID = "root"

        /** The shelf of every song in the library. */
        const val SONGS_ID = "songs"

        /** The shelf of every album in the library. */
        const val ALBUMS_ID = "albums"

        /** The shelf of every artist in the library. */
        const val ARTISTS_ID = "artists"

        /** What an album's own id is written as. */
        const val ALBUM_PREFIX = "album/"

        /** What an artist's name is written as. */
        const val ARTIST_PREFIX = "artist/"
    }
}
