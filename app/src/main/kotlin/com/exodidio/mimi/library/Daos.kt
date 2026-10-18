package com.exodidio.mimi.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    /** The whole library, already sorted the way the list screen wants it. */
    @Query(
        """
        SELECT * FROM tracks
        ORDER BY title COLLATE NOCASE ASC, artist COLLATE NOCASE ASC
        """
    )
    fun observeAll(): Flow<List<Track>>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<Track>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()
}

@Dao
interface MetaDao {

    @Query("SELECT meta_value FROM meta WHERE meta_key = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: MetaEntry)
}
