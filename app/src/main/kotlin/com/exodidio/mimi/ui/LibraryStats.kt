package com.exodidio.mimi.ui

import com.exodidio.mimi.library.PlayStat
import com.exodidio.mimi.library.Track

/**
 * What the library adds up to.
 *
 * Counted from the songs that are actually there rather than from the tally rows, so a
 * song that has gone from the device stops being counted and a tally that outlived its
 * song is not mistaken for listening.
 */
data class LibraryStats(
    val songs: Int = 0,
    val albums: Int = 0,
    val artists: Int = 0,
    val loved: Int = 0,
    val playlists: Int = 0,
    val totalDurationMs: Long = 0L,
    val totalPlays: Long = 0L,
    val totalSkips: Long = 0L,
    val longestSong: Track? = null,
)

/**
 * Adds up [songs] and the tallies that still belong to them.
 *
 * Runs off the values already in the database, so it is pure and can be checked without
 * a device. Only tallies whose song is still in [songs] are counted; anything left over
 * from a file that has since been deleted is ignored.
 */
fun libraryStats(
    songs: List<Track>,
    albumCount: Int,
    artistCount: Int,
    lovedIds: Set<Long>,
    playlistCount: Int,
    playStats: Map<Long, PlayStat>,
): LibraryStats {
    val present = songs.mapTo(HashSet()) { it.id }

    var plays = 0L
    var skips = 0L
    for ((trackId, stat) in playStats) {
        if (trackId !in present) continue
        plays += stat.playCount
        skips += stat.skipCount
    }

    return LibraryStats(
        songs = songs.size,
        albums = albumCount,
        artists = artistCount,
        loved = songs.count { it.id in lovedIds },
        playlists = playlistCount,
        totalDurationMs = songs.sumOf { it.durationMs },
        totalPlays = plays,
        totalSkips = skips,
        longestSong = songs.maxByOrNull { it.durationMs },
    )
}
