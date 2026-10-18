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

    /**
     * Albums, derived on demand.
     *
     * `MIN(artist)` picks one name to represent the album: tracks on a compilation can
     * disagree, and something has to be printed under the cover.
     */
    @Query(
        """
        SELECT album_id AS album_id,
               album AS album,
               MIN(artist) AS artist,
               COUNT(*) AS track_count,
               MIN(content_uri) AS artwork_uri
        FROM tracks
        WHERE album IS NOT NULL AND TRIM(album) != ''
        GROUP BY album_id
        ORDER BY album COLLATE NOCASE ASC
        """
    )
    fun observeAlbums(): Flow<List<AlbumSummary>>

    @Query(
        """
        SELECT artist AS artist,
               COUNT(*) AS track_count,
               COUNT(DISTINCT album_id) AS album_count
        FROM tracks
        WHERE artist IS NOT NULL AND TRIM(artist) != ''
        GROUP BY artist
        ORDER BY artist COLLATE NOCASE ASC
        """
    )
    fun observeArtists(): Flow<List<ArtistSummary>>

    /** Album running order: track number first, with title as the tie-break. */
    @Query(
        """
        SELECT * FROM tracks
        WHERE album_id = :albumId
        ORDER BY track_number ASC, title COLLATE NOCASE ASC
        """
    )
    fun observeAlbumTracks(albumId: Long): Flow<List<Track>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE artist = :artist
        ORDER BY album COLLATE NOCASE ASC, track_number ASC, title COLLATE NOCASE ASC
        """
    )
    fun observeArtistTracks(artist: String): Flow<List<Track>>

    @Query(
        """
        SELECT album_id AS album_id,
               album AS album,
               MIN(artist) AS artist,
               COUNT(*) AS track_count,
               MIN(content_uri) AS artwork_uri
        FROM tracks
        WHERE artist = :artist AND album IS NOT NULL AND TRIM(album) != ''
        GROUP BY album_id
        ORDER BY album COLLATE NOCASE ASC
        """
    )
    fun observeArtistAlbums(artist: String): Flow<List<AlbumSummary>>

    /**
     * Substring search over the three things a person actually remembers: the title, who
     * it is by, and which record it came from.
     *
     * SQLite's LIKE ignores case for ASCII by default, so "Kind" and "kind" behave alike.
     * The sort is by title rather than by relevance: matching a rank would mean inventing
     * one, and guessing wrong is worse than never guessing at all.
     */
    @Query(
        """
        SELECT * FROM tracks
        WHERE title LIKE '%' || :query || '%'
           OR artist LIKE '%' || :query || '%'
           OR album LIKE '%' || :query || '%'
        ORDER BY title COLLATE NOCASE ASC, artist COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<Track>>
}

@Dao
interface MetaDao {

    @Query("SELECT meta_value FROM meta WHERE meta_key = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: MetaEntry)
}
