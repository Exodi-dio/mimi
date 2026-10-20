package com.exodidio.mimi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Disc3
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MicVocal
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Repeat1
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Timer
import com.exodidio.mimi.playback.PlaybackState
import com.exodidio.mimi.playback.RepeatMode
import kotlin.math.abs

/**
 * Deliberately plain: artwork, title, artist, scrubber and transport.
 * The Apple Music inspired silhouette replaces this once playback is trusted.
 *
 * The header carries only a way out and a menu; the two faces the listener flips between
 * sit with the transport, where a thumb already is.
 */
@Composable
fun NowPlayingScreen(
    state: PlaybackState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    face: PlayerFace,
    onShowFace: (PlayerFace) -> Unit,
    onQueueItemClick: (Int) -> Unit,
    onQueueMoveUp: (Int) -> Unit,
    onQueueMoveDown: (Int) -> Unit,
    onQueueRemove: (Int) -> Unit,
    onSaveQueue: () -> Unit,
    onClearUpcoming: () -> Unit,
    sleepRemaining: Long?,
    sleepAtEndOfTrack: Boolean,
    onStartSleepTimer: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onStopAfterSong: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = state.current
    val lyricsState = rememberLyrics(current?.mediaId)

    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<PlayerDialog?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onClose) {
                Icon(Lucide.ChevronDown, contentDescription = "Close player")
            }
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Lucide.Ellipsis, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(sleepLabel(sleepRemaining)) },
                        leadingIcon = { Icon(Lucide.Timer, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            dialog = PlayerDialog.SLEEP
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Playback speed") },
                        leadingIcon = { Icon(Lucide.Gauge, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            dialog = PlayerDialog.SPEED
                        },
                    )
                    if (!current?.album.isNullOrBlank()) {
                        DropdownMenuItem(
                            text = { Text("Go to album") },
                            leadingIcon = { Icon(Lucide.Disc3, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onGoToAlbum()
                            },
                        )
                    }
                    if (!current?.artist.isNullOrBlank()) {
                        DropdownMenuItem(
                            text = { Text("Go to artist") },
                            leadingIcon = { Icon(Lucide.MicVocal, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onGoToArtist()
                            },
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (face) {
                PlayerFace.QUEUE -> QueuePanel(
                    queue = state.queue,
                    currentIndex = state.currentIndex,
                    shuffled = state.shuffleEnabled,
                    onItemClick = onQueueItemClick,
                    onMoveUp = onQueueMoveUp,
                    onMoveDown = onQueueMoveDown,
                    onRemove = onQueueRemove,
                    onSaveAsPlaylist = onSaveQueue,
                    onClearUpcoming = onClearUpcoming,
                    modifier = Modifier.fillMaxSize(),
                )

                PlayerFace.LYRICS -> when (val lyrics = lyricsState) {
                    is LyricsState.Found -> LyricsPanel(
                        lyrics = lyrics.lyrics,
                        positionMs = state.positionMs,
                        onSeek = onSeek,
                        modifier = Modifier.fillMaxSize(),
                    )

                    LyricsState.Loading -> LyricsMessage("Looking for lyrics\u2026")
                    LyricsState.None -> LyricsMessage("No lyrics in this file.")
                }

                PlayerFace.ARTWORK -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AlbumArtwork(mediaId = current?.mediaId)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = current?.title.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = current?.artist.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(24.dp))

        Scrubber(positionMs = state.positionMs, durationMs = state.durationMs, onSeek = onSeek)

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Lucide.Shuffle,
                    contentDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                    tint = accent(state.shuffleEnabled),
                )
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Lucide.SkipBack,
                    contentDescription = "Previous",
                    modifier = Modifier.size(36.dp),
                )
            }
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(68.dp)) {
                Icon(
                    imageVector = if (state.isPlaying) Lucide.Pause else Lucide.Play,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Lucide.SkipForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ONE) Lucide.Repeat1 else Lucide.Repeat,
                    contentDescription = repeatDescription(state.repeatMode),
                    tint = accent(state.repeatMode != RepeatMode.OFF),
                )
            }
        }

        // The words and the queue were header icons; they belong within thumb reach of the
        // transport, and the pair of step buttons fills the space either side of them.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = { onSeekBy(-STEP_MS) }) {
                Text("\u221210s")
            }
            IconButton(
                onClick = {
                    onShowFace(if (face == PlayerFace.LYRICS) PlayerFace.ARTWORK else PlayerFace.LYRICS)
                },
            ) {
                Icon(
                    imageVector = Lucide.MicVocal,
                    contentDescription = if (face == PlayerFace.LYRICS) "Hide lyrics" else "Show lyrics",
                    tint = accent(face == PlayerFace.LYRICS),
                )
            }
            IconButton(
                onClick = {
                    onShowFace(if (face == PlayerFace.QUEUE) PlayerFace.ARTWORK else PlayerFace.QUEUE)
                },
            ) {
                Icon(
                    imageVector = Lucide.ListMusic,
                    contentDescription = if (face == PlayerFace.QUEUE) "Hide up next" else "Show up next",
                    tint = accent(face == PlayerFace.QUEUE),
                )
            }
            TextButton(onClick = { onSeekBy(STEP_MS) }) {
                Text("+10s")
            }
        }
    }

    when (dialog) {
        PlayerDialog.SLEEP -> ChoiceDialog(
            title = "Sleep timer",
            options = SleepChoices.map { it.label },
            selectedIndex = when {
                sleepAtEndOfTrack -> SleepChoices.indexOfFirst { it.durationMs == null }
                sleepRemaining == null -> SleepChoices.indexOfFirst { it.durationMs == 0L }
                else -> -1
            },
            onPick = { index ->
                dialog = null
                SleepChoices.getOrNull(index)?.let { choice ->
                    when (choice.durationMs) {
                        // Null is "wait for this song", zero is "stop waiting at all".
                        null -> onStopAfterSong()
                        0L -> onCancelSleepTimer()
                        else -> onStartSleepTimer(choice.durationMs)
                    }
                }
            },
            onDismiss = { dialog = null },
        )

        PlayerDialog.SPEED -> ChoiceDialog(
            title = "Playback speed",
            options = SpeedChoices.map { it.second },
            selectedIndex = SpeedChoices.indexOfFirst { abs(it.first - state.speed) < 0.01f },
            onPick = { index ->
                dialog = null
                SpeedChoices.getOrNull(index)?.let { onSetSpeed(it.first) }
            },
            onDismiss = { dialog = null },
        )

        null -> Unit
    }
}

/** Which sheet is open over the player, if any. */
private enum class PlayerDialog { SLEEP, SPEED }

/** How far the step buttons move, in milliseconds. */
private const val STEP_MS = 10_000L

/**
 * One entry in the sleep timer's menu.
 *
 * [durationMs] is null for "end of track", which is a wait rather than a length, and zero
 * for "off". Keeping both as absent lengths in the same list means the menu cannot get out
 * of step with what the timer can do.
 */
private data class SleepChoice(val durationMs: Long?, val label: String)

/** Every way the sleep timer can be set. */
private val SleepChoices = listOf(
    SleepChoice(0L, "Off"),
    SleepChoice(null, "End of track"),
    SleepChoice(5 * 60_000L, "5 minutes"),
    SleepChoice(10 * 60_000L, "10 minutes"),
    SleepChoice(15 * 60_000L, "15 minutes"),
    SleepChoice(30 * 60_000L, "30 minutes"),
    SleepChoice(45 * 60_000L, "45 minutes"),
    SleepChoice(60 * 60_000L, "1 hour"),
)

/** The rates worth offering: half speed to well past double. */
private val SpeedChoices = listOf(
    0.5f to "0.5\u00d7",
    0.75f to "0.75\u00d7",
    1f to "1\u00d7",
    1.25f to "1.25\u00d7",
    1.5f to "1.5\u00d7",
    2f to "2\u00d7",
    2.5f to "2.5\u00d7",
    3f to "3\u00d7",
)

/** Names the timer, and says how long is left when it is running. */
private fun sleepLabel(remaining: Long?): String =
    if (remaining == null) "Sleep timer" else "Sleep timer \u00b7 " + formatDuration(remaining)

@Composable
private fun accent(active: Boolean) = if (active) {
    MaterialTheme.colorScheme.primary
} else {
    MaterialTheme.colorScheme.onSurfaceVariant
}

private fun repeatDescription(mode: RepeatMode): String = when (mode) {
    RepeatMode.OFF -> "Repeat off"
    RepeatMode.ALL -> "Repeat all"
    RepeatMode.ONE -> "Repeat one"
}

@Composable
private fun AlbumArtwork(mediaId: String?) {
    val artwork = rememberAlbumArt(mediaId)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Lucide.Music,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Scrubber(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    // While a finger is on the slider we follow the finger; otherwise we follow playback.
    var scrubbingAt by remember { mutableStateOf<Float?>(null) }

    val max = durationMs.coerceAtLeast(1L).toFloat()
    val value = (scrubbingAt ?: positionMs.toFloat()).coerceIn(0f, max)

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = value,
            onValueChange = { scrubbingAt = it },
            onValueChangeFinished = {
                scrubbingAt?.let { onSeek(it.toLong()) }
                scrubbingAt = null
            },
            valueRange = 0f..max,
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (LocalSettings.current.showRemainingTime) {
                    "-" + formatDuration((max.toLong() - value.toLong()).coerceAtLeast(0L))
                } else {
                    formatDuration(value.toLong())
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
