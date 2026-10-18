package com.exodidio.mimi.library

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * What the listener has done with one song.
 *
 * A table of its own rather than columns on `tracks`, for the same reason loved songs
 * live outside it: `tracks` is thrown away and rebuilt on every scan, and how many times
 * someone has played a song is not something a scan can recover.
 *
 * Nothing here is written until there is something to record, so a library that has never
 * been played through has no rows in this table at all.
 */
@Entity(tableName = "play_stats")
data class PlayStat(
    @PrimaryKey @ColumnInfo("track_id") val trackId: Long,
    /** Times the song was listened to past the point where it counts as listened to. */
    @ColumnInfo("play_count") val playCount: Int = 0,
    /** Times the listener moved on before it got that far. */
    @ColumnInfo("skip_count") val skipCount: Int = 0,
    /** Wall clock, so "recently played" survives a reboot and a rescan. */
    @ColumnInfo("last_played_at") val lastPlayedAt: Long? = null,
)
