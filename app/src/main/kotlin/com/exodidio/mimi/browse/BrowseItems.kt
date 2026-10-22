package com.exodidio.mimi.browse

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.albumArtUriFor

/**
 * The library's row as the tree sees it.
 *
 * The cover is the one the device indexed for the album, because the picture inside the
 * file is only found by opening the file, and a tree of thousands of songs cannot open
 * every file it lists.
 */
fun Track.toBrowseSong(): BrowseSong = BrowseSong(
    mediaId = contentUri,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    trackNumber = trackNumber,
    durationMs = durationMs,
    artworkUri = albumArtUriFor(albumId),
)

/**
 * The item a browser is shown for a song.
 *
 * The uri is in the item as well as in its id, so the item is playable the moment a
 * browser hands it back: a car asks for a song by its id, and the session has to be
 * able to start it without another round trip.
 */
fun BrowseSong.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(mediaId)
    .setUri(mediaId)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri?.let { Uri.parse(it) })
            .setDurationMs(durationMs)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build(),
    )
    .build()

/**
 * The item a browser is shown for a shelf.
 *
 * A shelf has no sound of its own, so it carries no uri: a browser that tries to play
 * one is asking for something that is not there.
 */
fun BrowseEntry.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(mediaId)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri?.let { Uri.parse(it) })
            .setDurationMs(if (playable) durationMs else null)
            .setIsBrowsable(browsable)
            .setIsPlayable(playable)
            .build(),
    )
    .build()
