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

    /** Every cached id, so a scan can tell what has gone since the last one. */
    @Query("SELECT id FROM tracks")
    suspend fun ids(): List<Long>

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
               MIN(COALESCE(album_artist, artist)) AS artist,
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

    /** The newest arrivals, which is what a "recently added" shelf wants. */
    @Query(
        """
        SELECT * FROM tracks
        WHERE date_added IS NOT NULL
        ORDER BY date_added DESC, title COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun observeRecentlyAdded(limit: Int): Flow<List<Track>>

    @Query(
        """
        SELECT genre AS genre,
               COUNT(*) AS track_count,
               MIN(content_uri) AS artwork_uri
        FROM tracks
        WHERE genre IS NOT NULL AND TRIM(genre) != ''
        GROUP BY genre
        ORDER BY genre COLLATE NOCASE ASC
        """
    )
    fun observeGenres(): Flow<List<GenreSummary>>

    @Query(
        """
        SELECT composer AS composer,
               COUNT(*) AS track_count,
               MIN(content_uri) AS artwork_uri
        FROM tracks
        WHERE composer IS NOT NULL AND TRIM(composer) != ''
        GROUP BY composer
        ORDER BY composer COLLATE NOCASE ASC
        """
    )
    fun observeComposers(): Flow<List<ComposerSummary>>

    /** Newest year first: someone browsing by year is almost always looking backwards. */
    @Query(
        """
        SELECT year AS year,
               COUNT(*) AS track_count,
               MIN(content_uri) AS artwork_uri
        FROM tracks
        WHERE year IS NOT NULL
        GROUP BY year
        ORDER BY year DESC
        """
    )
    fun observeYears(): Flow<List<YearSummary>>

    @Query(
        """
        SELECT folder AS folder,
               COUNT(*) AS track_count,
               MIN(content_uri) AS artwork_uri
        FROM tracks
        WHERE folder IS NOT NULL AND TRIM(folder) != ''
        GROUP BY folder
        ORDER BY folder COLLATE NOCASE ASC
        """
    )
    fun observeFolders(): Flow<List<FolderSummary>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE genre = :genre
        ORDER BY artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC,
                 track_number ASC, title COLLATE NOCASE ASC
        """
    )
    fun observeGenreTracks(genre: String): Flow<List<Track>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE composer = :composer
        ORDER BY album COLLATE NOCASE ASC, track_number ASC, title COLLATE NOCASE ASC
        """
    )
    fun observeComposerTracks(composer: String): Flow<List<Track>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE year = :year
        ORDER BY album COLLATE NOCASE ASC, track_number ASC, title COLLATE NOCASE ASC
        """
    )
    fun observeYearTracks(year: Int): Flow<List<Track>>

    /** File name order, which is how a folder on disk actually reads. */
    @Query(
        """
        SELECT * FROM tracks
        WHERE folder = :folder
        ORDER BY file_name COLLATE NOCASE ASC, title COLLATE NOCASE ASC
        """
    )
    fun observeFolderTracks(folder: String): Flow<List<Track>>
}

@Dao
interface MetaDao {

    @Query("SELECT meta_value FROM meta WHERE meta_key = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: MetaEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAll(entries: List<MetaEntry>)

    /**
     * Every row whose key starts with [prefix].
     *
     * Settings share this table with the playback session, so they are read back by their
     * own prefix rather than by reading the table whole: the session is rewritten every few
     * seconds, and a settings flow that re-emitted with it would restart the player.
     */
    @Query("SELECT * FROM meta WHERE meta_key LIKE :prefix || '%'")
    fun observeWithPrefix(prefix: String): Flow<List<MetaEntry>>

    /** The same rows, read once rather than watched. */
    @Query("SELECT * FROM meta WHERE meta_key LIKE :prefix || '%'")
    suspend fun getAllWithPrefix(prefix: String): List<MetaEntry>

    /** Removes every row under a prefix, so a rewritten list leaves nothing behind. */
    @Query("DELETE FROM meta WHERE meta_key LIKE :prefix || '%'")
    suspend fun deleteWithPrefix(prefix: String)
}

/**
 * Loved songs.
 *
 * Reads join against `tracks` rather than storing a copy of the song, so a rescan
 * cannot leave a loved entry describing a file that has gone.
 */
@Dao
interface FavoriteDao {

    @Query("SELECT track_id FROM favorites")
    fun observeIds(): Flow<List<Long>>

    /** Most recently loved first, which is the order the user built it in. */
    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN favorites ON favorites.track_id = tracks.id
        ORDER BY favorites.added_at DESC
        """
    )
    fun observeLoved(): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE track_id = :trackId")
    suspend fun remove(trackId: Long)
}

/**
 * Playlists and their contents.
 *
 * Every read of a playlist's songs goes through a join against `tracks`, so a song that
 * no longer exists on the device drops out on its own.
 */
@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id AS id,
               p.name AS name,
               (SELECT COUNT(*) FROM playlist_entries e WHERE e.playlist_id = p.id) AS track_count
        FROM playlists p
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    /** Playlist running order. Songs missing from the device are simply not joined in. */
    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN playlist_entries ON playlist_entries.track_id = tracks.id
        WHERE playlist_entries.playlist_id = :playlistId
        ORDER BY playlist_entries.position ASC
        """
    )
    fun observePlaylistTracks(playlistId: Long): Flow<List<Track>>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun rename(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    /** Deleted first and explicitly rather than leaning on the foreign key cascade. */
    @Query("DELETE FROM playlist_entries WHERE playlist_id = :playlistId")
    suspend fun deleteEntries(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_entries WHERE playlist_id = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("SELECT COUNT(*) FROM playlist_entries WHERE playlist_id = :playlistId AND track_id = :trackId")
    suspend fun entryCount(playlistId: Long, trackId: Long): Int

    @Insert
    suspend fun insertEntry(entry: PlaylistEntry)

    @Query("DELETE FROM playlist_entries WHERE playlist_id = :playlistId AND track_id = :trackId")
    suspend fun removeEntry(playlistId: Long, trackId: Long)

    /** The running order as ids, so a move can be reasoned about without the songs. */
    @Query("SELECT track_id FROM playlist_entries WHERE playlist_id = :playlistId ORDER BY position ASC")
    suspend fun entryTrackIds(playlistId: Long): List<Long>

    @Query(
        "UPDATE playlist_entries SET position = :position " +
            "WHERE playlist_id = :playlistId AND track_id = :trackId"
    )
    suspend fun setPosition(playlistId: Long, trackId: Long, position: Int)
}

/**
 * How the listener has listened.
 *
 * Every read joins against `tracks`, so a song that has gone from the device takes its
 * history out of these lists on its own rather than leaving a row that cannot be opened.
 */
@Dao
interface PlayStatDao {

    @Query(
        """
        SELECT track_id AS track_id,
               play_count AS play_count,
               skip_count AS skip_count,
               last_played_at AS last_played_at
        FROM play_stats
        """
    )
    fun observeAll(): Flow<List<PlayStat>>

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN play_stats ON play_stats.track_id = tracks.id
        WHERE play_stats.last_played_at IS NOT NULL
        ORDER BY play_stats.last_played_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentlyPlayed(limit: Int): Flow<List<Track>>

    /** Only songs that were actually heard, most heard first. */
    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN play_stats ON play_stats.track_id = tracks.id
        WHERE play_stats.play_count > 0
        ORDER BY play_stats.play_count DESC, tracks.title COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun observeMostPlayed(limit: Int): Flow<List<Track>>

    /** Ignored when a row is already there, so history is only ever added to. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(stat: PlayStat)

    @Query(
        """
        UPDATE play_stats
        SET play_count = play_count + 1, last_played_at = :at
        WHERE track_id = :trackId
        """
    )
    suspend fun recordPlay(trackId: Long, at: Long)

    @Query("UPDATE play_stats SET skip_count = skip_count + 1 WHERE track_id = :trackId")
    suspend fun recordSkip(trackId: Long)

    /** History for files that are no longer on the device. */
    @Query("DELETE FROM play_stats WHERE track_id NOT IN (SELECT id FROM tracks)")
    suspend fun prune()
}

/**
 * Listening time, one row per song per local day.
 *
 * The write is an upsert that adds to the row already there rather than replacing it: a
 * song heard twice in an evening, or in two stretches either side of a pause, is one day of
 * listening and not the last stretch alone.
 *
 * Reads are unkeyed because the screen can ask for any window it likes, and a handful of
 * thousand small rows is cheaper to fold in memory than to teach the database about every
 * period the picker offers.
 */
@Dao
interface ListenDayDao {

    @Query(
        """
        SELECT id, track_id, day, listened_ms, plays
        FROM listen_days
        ORDER BY day ASC
        """
    )
    fun observeAll(): Flow<List<ListenDay>>

    /**
     * Adds listening to the row for this song and day, creating it when it is the first.
     *
     * `excluded` is the row this statement tried to insert, so the existing totals are
     * added to the new amounts rather than being overwritten by them.
     */
    @Query(
        """
        INSERT INTO listen_days (track_id, day, listened_ms, plays)
        VALUES (:trackId, :day, :listenedMs, :plays)
        ON CONFLICT(track_id, day) DO UPDATE SET
            listened_ms = listened_ms + excluded.listened_ms,
            plays = plays + excluded.plays
        """
    )
    suspend fun addListening(trackId: Long, day: String, listenedMs: Long, plays: Int)

    /** History for files that are no longer on the device, pruned with the files themselves. */
    @Query("DELETE FROM listen_days WHERE track_id NOT IN (SELECT id FROM tracks)")
    suspend fun prune()
}
