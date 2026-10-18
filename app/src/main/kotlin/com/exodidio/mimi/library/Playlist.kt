package com.exodidio.mimi.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A playlist the user made.
 *
 * This is the first table holding something that cannot be rebuilt from a scan. A
 * rescan may delete the whole `tracks` table and put it back; it must never touch this.
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/**
 * One song's place in one playlist.
 *
 * [position] is a plain running number rather than a linked list, and removals leave
 * gaps instead of renumbering: rewriting every later row to delete one song would be
 * work with nothing to show for it, since only the relative order matters.
 *
 * Entries point at the track by id and are read through a join, so a song that is
 * deleted from the device simply stops appearing instead of leaving a blank row.
 */
@Entity(
    tableName = "playlist_entries",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["playlist_id"])],
)
data class PlaylistEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    @ColumnInfo(name = "track_id") val trackId: Long,
    val position: Int,
)

/**
 * A song the user marked as loved.
 *
 * Kept as its own table rather than a column on `tracks`, which is wiped and rewritten
 * on every scan — anything the user chose has to live somewhere a scan cannot reach.
 */
@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey @ColumnInfo(name = "track_id") val trackId: Long,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

/** A playlist and how many songs are in it, for the list screen. */
data class PlaylistSummary(
    val id: Long,
    val name: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
)
