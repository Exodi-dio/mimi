package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Disc3
import com.composables.icons.lucide.ListEnd
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.ListStart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MicVocal
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Share2
import com.exodidio.mimi.library.PlayStat
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.channelLabel
import com.exodidio.mimi.library.readAudioChannelCount
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Everything the app knows about one song.
 *
 * The library only ever shows a title, an artist and a length, which is all a list needs.
 * This is the place for the rest of it: what the tags say, where the file came from, and
 * what has happened to it since it was found.
 *
 * The song is looked up by id rather than handed over, so a file that has gone away since
 * the row was tapped leaves this screen able to say so instead of showing a ghost.
 */
@Composable
fun TrackInfoScreen(
    track: Track?,
    stat: PlayStat?,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onEditTags: () -> Unit = {},
    modifier: Modifier = Modifier,
    playingMediaId: String? = null,
) {
    if (track == null) {
        Column(modifier = modifier.fillMaxSize()) {
            ScreenHeader(title = "Song info", onBack = onBack)
            InfoMessage("This song is no longer in your library.")
        }
        return
    }

    val albumLine = listOfNotNull(track.artist, track.album).joinToString(" \u00b7 ")

    // Read from the file itself rather than from the provider, which is why it takes a
    // moment to arrive.
    val channels = rememberAudioChannels(track.contentUri)

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = track.title,
            subtitle = albumLine.takeIf { it.isNotEmpty() },
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Artwork(
                    bitmap = rememberAlbumArt(
                        mediaId = track.contentUri,
                        targetPx = 512,
                        albumId = track.albumId,
                    ),
                    modifier = Modifier.size(180.dp),
                    shape = RoundedCornerShape(12.dp),
                    iconSize = 56.dp,
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Lucide.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (track.contentUri == playingMediaId) "Playing" else "Play")
                }
                IconButton(onClick = onPlayNext) {
                    Icon(Lucide.ListStart, contentDescription = "Play next")
                }
                IconButton(onClick = onAddToQueue) {
                    Icon(Lucide.ListEnd, contentDescription = "Add to queue")
                }
                IconButton(onClick = onAddToPlaylist) {
                    Icon(Lucide.ListPlus, contentDescription = "Add to playlist")
                }
                IconButton(onClick = onShare) {
                    Icon(Lucide.Share2, contentDescription = "Share")
                }
            }

            InfoSection("The song")
            InfoRow("Title", track.title)
            InfoRow("Artist", track.artist)
            InfoRow("Album", track.album)
            InfoRow("Album artist", track.albumArtist)
            InfoRow("Track number", track.trackNumber.takeIf { it > 0 }?.toString())
            InfoRow("Year", track.year?.toString())
            InfoRow("Genre", track.genre)
            InfoRow("Composer", track.composer)
            InfoRow("Length", formatDuration(track.durationMs))

            InfoLink("Edit the tags", Lucide.Pencil, onEditTags)

            InfoSection("The file")
            InfoRow("Name", track.fileName)
            InfoRow("Folder", track.folder)
            InfoRow("Format", track.mimeType)
            InfoRow("Size", formatFileSize(track.sizeBytes))
            InfoRow("Added", formatDate(track.dateAdded))
            InfoRow("Modified", formatDate(track.dateModified))
            // Said only once the file has been read: a row that says "unknown" while the
            // answer is still on its way would be wrong rather than honest.
            channels?.let { InfoRow("Channels", channelLabel(it.channels)) }
            InfoRow("Location", track.contentUri)

            InfoSection("Listening")
            InfoRow("Times played", (stat?.playCount ?: 0).toString())
            InfoRow("Times skipped", (stat?.skipCount ?: 0).toString())
            InfoRow("Last played", formatDate(stat?.lastPlayedAt))

            if (!track.album.isNullOrBlank() || !track.artist.isNullOrBlank()) {
                InfoSection("Go to")
                if (!track.album.isNullOrBlank()) {
                    InfoLink("Open the album", Lucide.Disc3, onGoToAlbum)
                }
                if (!track.artist.isNullOrBlank()) {
                    InfoLink("Open the artist", Lucide.MicVocal, onGoToArtist)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoSection(title: String) {
    HorizontalDivider(
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * One fact. Left out entirely when there is nothing to say, because a screen of blanks
 * reads as a screen that has failed rather than as a song whose tags are thin.
 */
@Composable
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(112.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InfoLink(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoMessage(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Seconds since the epoch, as the device reads a date.
 *
 * Formatting is left to the platform rather than done by hand so it follows the phone's
 * own language and date order. Not pure, so it lives here rather than in [formatDuration]
 * beside the others.
 */
private fun formatDate(epochSeconds: Long?): String? {
    if (epochSeconds == null || epochSeconds <= 0L) return null
    return runCatching {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochSeconds * 1_000L))
    }.getOrNull()
}

/**
 * The channel count of the file, once it has been read.
 *
 * Null while the read is still on its way, which is what keeps the row off the screen
 * until there is something true to say about it.
 */
private data class AudioChannelReading(val channels: Int?)

/**
 * How many channels the file behind [mediaId] has.
 *
 * The read opens the file and reads its header, so it is made off the main thread: a
 * file that takes a moment to open would otherwise hold the screen up while it does.
 */
@Composable
private fun rememberAudioChannels(mediaId: String?): AudioChannelReading? {
    val context = LocalContext.current
    return produceState<AudioChannelReading?>(initialValue = null, mediaId) {
        value = AudioChannelReading(
            channels = withContext(Dispatchers.IO) {
                readAudioChannelCount(context, mediaId)
            },
        )
    }.value
}
