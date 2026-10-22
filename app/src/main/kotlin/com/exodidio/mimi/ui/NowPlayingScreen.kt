package com.exodidio.mimi.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Disc3
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.ListEnd
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MicVocal
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Repeat1
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Timer
import com.exodidio.mimi.lyrics.LyricsQuery
import com.exodidio.mimi.library.SongFormat
import com.exodidio.mimi.library.readSongFormat
import com.exodidio.mimi.playback.PlaybackState
import com.exodidio.mimi.playback.QueueItem
import com.exodidio.mimi.playback.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * The player.
 *
 * The words are the screen. The line being sung sits in the middle of the block with the
 * lines around it fading away from it, the song's name and artist sit underneath, and
 * the transport, the scrubber and the player's own row of actions follow. The app's
 * bottom navigation is not drawn while this is up: the player takes the whole screen and
 * the row at the bottom belongs to the song rather than to the app.
 *
 * The cover sits above the words at roughly two fifths of the screen's height. It takes the
 * place of nothing: the words keep their block underneath it, and the cover fades from one
 * song to the next rather than changing in place. A tap on it opens it full screen, which
 * is the only way the artwork is seen at full size now that the artwork face is gone.
 *
 * The queue moved behind the queue button, since a face that had to be swiped to was a
 * face that was never found. The sideways swipe over the words still works and reaches
 * the same place, and it does not fight the lyrics: the gesture is only claimed once it
 * is clearly sideways.
 */
@Composable
fun NowPlayingScreen(
    state: PlaybackState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    isLoved: Boolean,
    onToggleLove: () -> Unit,
    queueOpen: Boolean,
    onQueueOpenChange: (Boolean) -> Unit,
    sleepRemaining: Long?,
    sleepAtEndOfTrack: Boolean,
    onStartSleepTimer: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onStopAfterSong: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetPitch: (Int) -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onQueueItemClick: (Int) -> Unit,
    onQueueMoveUp: (Int) -> Unit,
    onQueueMoveDown: (Int) -> Unit,
    onQueueRemove: (Int) -> Unit,
    onSaveQueue: () -> Unit,
    onClearUpcoming: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = state.current
    // Words that have just been found and kept are not in the file, so the read that
    // produced the empty sheet has to be asked again for them to appear.
    var lyricsVersion by remember { mutableStateOf(0) }
    val lyricsState = rememberLyrics(mediaId = current?.mediaId, version = lyricsVersion)

    var menuAnchor by remember { mutableStateOf<MenuAnchor?>(null) }
    var dialog by remember { mutableStateOf<PlayerDialog?>(null) }
    var findingLyrics by remember { mutableStateOf(false) }
    var queueSheetOpen by remember { mutableStateOf(false) }

    val settings = LocalSettings.current
    val formatLine = rememberSongFormatLine(current?.mediaId)

    // The cover is measured against the screen rather than against the space left over by
    // the words, because the request is a share of the screen: an area that shrank as the
    // words grew would draw a different cover on every song.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val coverSide = minOf(maxWidth - 48.dp, maxHeight * COVER_HEIGHT_FRACTION)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        // A slim header: a way out on the left and the menu on the right. Nothing else,
        // because the song's own name is the title and it sits further down the screen.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onClose) {
                Icon(Lucide.ChevronLeft, contentDescription = "Close player")
            }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuAnchor = MenuAnchor.HEADER }) {
                    Icon(Lucide.Ellipsis, contentDescription = "More options")
                }
                PlayerMenu(
                    expanded = menuAnchor == MenuAnchor.HEADER,
                    onDismiss = { menuAnchor = null },
                    sleepRemaining = sleepRemaining,
                    onSleepTimer = {
                        menuAnchor = null
                        dialog = PlayerDialog.SLEEP
                    },
                    onSpeed = {
                        menuAnchor = null
                        dialog = PlayerDialog.SPEED
                    },
                    onFindLyrics = {
                        menuAnchor = null
                        findingLyrics = true
                    },
                    canGoToAlbum = !current?.album.isNullOrBlank(),
                    canGoToArtist = !current?.artist.isNullOrBlank(),
                    onGoToAlbum = {
                        menuAnchor = null
                        onGoToAlbum()
                    },
                    onGoToArtist = {
                        menuAnchor = null
                        onGoToArtist()
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // A horizontal swipe reaches the queue; the detector only claims a gesture
                // once it is clearly sideways, so a lyrics list keeps its vertical
                // scrolling and the queue's arrow buttons keep their taps.
                .pointerInput(queueOpen) {
                    var travelled = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { travelled = 0f },
                        onHorizontalDrag = { _, amount -> travelled += amount },
                        onDragCancel = { travelled = 0f },
                        onDragEnd = {
                            val threshold = 72.dp.toPx()
                            if (abs(travelled) > threshold) onQueueOpenChange(!queueOpen)
                            travelled = 0f
                        },
                    )
                },
        ) {
            AnimatedContent(
                targetState = queueOpen,
                transitionSpec = {
                    val forward = targetState
                    slideInHorizontally { width -> if (forward) width else -width } togetherWith
                        slideOutHorizontally { width -> if (forward) -width else width }
                },
                label = "player queue",
            ) { showingQueue ->
                if (showingQueue) {
                    QueuePanel(
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
                } else {
                    // The cover sits above the words and takes the same share of the
                    // screen whatever the song carries, so a song with no words does not
                    // suddenly get a bigger cover than the one before it. It is capped to
                    // the space this area can give without squeezing the words out.
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val side = minOf(coverSide, maxHeight * COVER_MAX_SHARE_OF_WORDS)

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            SongCover(mediaId = current?.mediaId, side = side)
                            Spacer(Modifier.height(8.dp))

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when (val lyrics = lyricsState) {
                                    is LyricsState.Found -> LyricsPanel(
                                        lyrics = lyrics.lyrics,
                                        positionMs = state.positionMs,
                                        onSeek = onSeek,
                                        isPlaying = state.isPlaying,
                                        modifier = Modifier.fillMaxSize(),
                                    )

                                    LyricsState.Loading -> LyricsMessage("Looking for lyrics\u2026")

                                    // The cover is already above, so this only has to say
                                    // that there is nothing to read beneath it.
                                    LyricsState.None -> MissingLyrics(
                                        modifier = Modifier.align(Alignment.Center),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // The song's own name sits under the words rather than over them: the words are
        // what the listener came for, and the name is what they already know.
        Spacer(Modifier.height(28.dp))

        Text(
            text = current?.title.orEmpty(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = current?.artist.orEmpty(),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ONE) Lucide.Repeat1 else Lucide.Repeat,
                    contentDescription = repeatDescription(state.repeatMode),
                    tint = accent(state.repeatMode != RepeatMode.OFF),
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Lucide.SkipBack,
                    contentDescription = "Previous",
                    modifier = Modifier.size(28.dp),
                )
            }
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Lucide.Pause else Lucide.Play,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Lucide.SkipForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Lucide.Shuffle,
                    contentDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                    tint = accent(state.shuffleEnabled),
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        ProgressLine(positionMs = state.positionMs, durationMs = state.durationMs, onSeek = onSeek)

        // What the file says about itself, when the listener has asked to be told.
        if (settings.showSongInfo) {
            formatLine?.let { line ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = line,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (settings.showVolumeSlider) {
            Spacer(Modifier.height(8.dp))
            VolumeSlider(volume = state.volume, onVolumeChange = onVolumeChange)
        }

        Spacer(Modifier.height(8.dp))

        // The player's own row of actions. It sits where the app's navigation would be,
        // which is why the navigation is not drawn while this screen is up.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = { onQueueOpenChange(!queueOpen) }) {
                Icon(
                    imageVector = Lucide.ListMusic,
                    contentDescription = "Queue",
                    tint = accent(queueOpen),
                )
            }
            IconButton(onClick = onToggleLove) {
                Icon(
                    imageVector = Lucide.Heart,
                    contentDescription = if (isLoved) "Remove from loved songs" else "Love this song",
                    tint = if (isLoved) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = { dialog = PlayerDialog.SLEEP }) {
                Icon(
                    imageVector = Lucide.Timer,
                    contentDescription = sleepLabel(sleepRemaining),
                    tint = accent(sleepRemaining != null),
                )
            }
            IconButton(onClick = { queueSheetOpen = true }) {
                Icon(
                    imageVector = Lucide.ListEnd,
                    contentDescription = "Queue list",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuAnchor = MenuAnchor.ACTIONS }) {
                    Icon(Lucide.Ellipsis, contentDescription = "More options")
                }
                PlayerMenu(
                    expanded = menuAnchor == MenuAnchor.ACTIONS,
                    onDismiss = { menuAnchor = null },
                    sleepRemaining = sleepRemaining,
                    onSleepTimer = {
                        menuAnchor = null
                        dialog = PlayerDialog.SLEEP
                    },
                    onSpeed = {
                        menuAnchor = null
                        dialog = PlayerDialog.SPEED
                    },
                    onFindLyrics = {
                        menuAnchor = null
                        findingLyrics = true
                    },
                    canGoToAlbum = !current?.album.isNullOrBlank(),
                    canGoToArtist = !current?.artist.isNullOrBlank(),
                    onGoToAlbum = {
                        menuAnchor = null
                        onGoToAlbum()
                    },
                    onGoToArtist = {
                        menuAnchor = null
                        onGoToArtist()
                    },
                )
            }
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

        // A sheet rather than a list of rates: the speed and the pitch are shown
        // together, each with the value it is set to, so the choice can be made while
        // the song is playing.
        PlayerDialog.SPEED -> SpeedPitchSheet(
            speed = state.speed,
            pitchSemitones = state.pitchSemitones,
            onSpeed = onSetSpeed,
            onPitch = onSetPitch,
            onDismiss = { dialog = null },
        )

        null -> Unit
    }

    val playing = current
    if (findingLyrics && playing != null) {
        FindLyricsDialog(
            // The player knows what is playing as a queue item, which is all a lyrics
            // database needs: a name, who it is by, and how long it runs.
            question = LyricsQuery(
                key = playing.mediaId,
                title = playing.title,
                artist = playing.artist,
                album = playing.album,
                durationMs = state.durationMs,
            ),
            onKept = {
                findingLyrics = false
                lyricsVersion++
            },
            onDismiss = { findingLyrics = false },
        )
    }

    if (queueSheetOpen) {
        QueueSheet(
            queue = state.queue,
            currentIndex = state.currentIndex,
            shuffled = state.shuffleEnabled,
            onItemClick = onQueueItemClick,
            onMoveUp = onQueueMoveUp,
            onMoveDown = onQueueMoveDown,
            onRemove = onQueueRemove,
            onSaveAsPlaylist = onSaveQueue,
            onClearUpcoming = onClearUpcoming,
            onDismiss = { queueSheetOpen = false },
        )
    }
}

/** Which of the two three-dot buttons has its menu open, if either. */
private enum class MenuAnchor { HEADER, ACTIONS }

/** Which sheet is open over the player, if any. */
private enum class PlayerDialog { SLEEP, SPEED }

/**
 * The menu behind the three dots.
 *
 * Both three-dot buttons open this same list, because a listener who reaches for the
 * menu at the bottom of the screen should not find a different menu there.
 */
@Composable
private fun PlayerMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    sleepRemaining: Long?,
    onSleepTimer: () -> Unit,
    onSpeed: () -> Unit,
    onFindLyrics: () -> Unit,
    canGoToAlbum: Boolean,
    canGoToArtist: Boolean,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(sleepLabel(sleepRemaining)) },
            leadingIcon = { Icon(Lucide.Timer, contentDescription = null) },
            onClick = onSleepTimer,
        )
        DropdownMenuItem(
            text = { Text("Speed and pitch") },
            leadingIcon = { Icon(Lucide.Gauge, contentDescription = null) },
            onClick = onSpeed,
        )
        DropdownMenuItem(
            text = { Text("Find lyrics") },
            leadingIcon = { Icon(Lucide.Search, contentDescription = null) },
            onClick = onFindLyrics,
        )
        if (canGoToAlbum) {
            DropdownMenuItem(
                text = { Text("Go to album") },
                leadingIcon = { Icon(Lucide.Disc3, contentDescription = null) },
                onClick = onGoToAlbum,
            )
        }
        if (canGoToArtist) {
            DropdownMenuItem(
                text = { Text("Go to artist") },
                leadingIcon = { Icon(Lucide.MicVocal, contentDescription = null) },
                onClick = onGoToArtist,
            )
        }
    }
}

/**
 * The queue as a sheet over the player.
 *
 * The same list the queue button shows in place of the words, but over them rather than
 * instead of them, for the times a listener wants to look at what is coming without
 * losing the words they were reading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    queue: List<QueueItem>,
    currentIndex: Int,
    shuffled: Boolean,
    onItemClick: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSaveAsPlaylist: () -> Unit,
    onClearUpcoming: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        QueuePanel(
            queue = queue,
            currentIndex = currentIndex,
            shuffled = shuffled,
            onItemClick = onItemClick,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onRemove = onRemove,
            onSaveAsPlaylist = onSaveAsPlaylist,
            onClearUpcoming = onClearUpcoming,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp),
        )
    }
}

/**
 * The volume, when the listener has asked to see it.
 *
 * The thumb is held while it is being dragged so the slider does not jump back to the
 * engine's older reading between one tick and the next.
 */
@Composable
private fun VolumeSlider(volume: Float, onVolumeChange: (Float) -> Unit) {
    var draggedTo by remember { mutableStateOf<Float?>(null) }
    val shown = draggedTo ?: volume

    Slider(
        value = shown.coerceIn(0f, 1f),
        onValueChange = {
            draggedTo = it
            onVolumeChange(it)
        },
        onValueChangeFinished = { draggedTo = null },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * The thin line under the transport: how far through the song it is, and where to jump.
 *
 * A rule three points high with a bead at its head, drawn rather than taken from the
 * material slider, because the reference player shows a rule and a bead rather than a
 * track with a handle. Dragging moves the bead and lets go where it stands.
 */
@Composable
private fun ProgressLine(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    var widthPx by remember { mutableStateOf(1f) }
    var draggedTo by remember { mutableStateOf<Float?>(null) }
    val latestSeek by rememberUpdatedState(onSeek)

    val fraction = if (durationMs <= 0L) {
        0f
    } else {
        (draggedTo ?: positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                // A finger needs more room than the rule itself to land on it.
                .height(24.dp)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(durationMs) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var target = (down.position.x / widthPx).coerceIn(0f, 1f)
                        draggedTo = target
                        down.consume()
                        drag(down.id) { change ->
                            change.consume()
                            target = (change.position.x / widthPx).coerceIn(0f, 1f)
                            draggedTo = target
                        }
                        draggedTo = null
                        if (durationMs > 0L) latestSeek((target * durationMs).toLong())
                    }
                },
        ) {
            val bead = 10.dp
            val travel = maxWidth - bead

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = travel * fraction)
                    .size(bead)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (LocalSettings.current.showRemainingTime) {
                    "-" + formatDuration((durationMs - positionMs).coerceAtLeast(0L))
                } else {
                    formatDuration(positionMs)
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(durationMs),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The one line that says what the file is.
 *
 * It is read from the device rather than from the library, because the library does not
 * keep a bitrate or a sample rate and the line is about the file as it is on the
 * phone. A file nothing is known about has no line at all.
 */
@Composable
private fun rememberSongFormatLine(mediaId: String?): String? {
    val context = LocalContext.current
    val format = produceState<SongFormat?>(initialValue = null, mediaId) {
        value = withContext(Dispatchers.IO) { readSongFormat(context, mediaId) }
    }.value

    return songFormatLine(format)
}

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

/** The share of the screen height the cover asks for. */
private val COVER_HEIGHT_FRACTION = 0.44f

/**
 * The most of the words' area the cover may take.
 *
 * On a short screen a cover at its full share would leave the words a line or two, which
 * is not enough to read along with. The words win that argument, so the cover gives way
 * until they have room.
 */
private const val COVER_MAX_SHARE_OF_WORDS = 0.62f

/** How long the cover takes to cross from one song to the next. */
private const val COVER_FADE_MS = 320

/**
 * The cover, faded from one song to the next.
 *
 * The fade is what keeps the change of song from reading as a flicker: the previous cover
 * is still there when the next one starts decoding, and the two cross rather than one
 * being removed before the other exists.
 */
@Composable
private fun SongCover(mediaId: String?, side: Dp) {
    AnimatedContent(
        targetState = mediaId,
        transitionSpec = {
            fadeIn(tween(COVER_FADE_MS)) togetherWith fadeOut(tween(COVER_FADE_MS))
        },
        label = "cover",
    ) { id ->
        AlbumArtwork(mediaId = id, modifier = Modifier.size(side))
    }
}

/**
 * The cover, and the full-screen view of it.
 *
 * A tap and a long press both open it, because the request named the long press and a
 * plain tap keeps it reachable for anyone who cannot hold a finger down.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumArtwork(mediaId: String?, modifier: Modifier = Modifier) {
    val artwork = rememberAlbumArt(mediaId)
    var fullScreen by remember(mediaId) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = { if (artwork != null) fullScreen = true },
                onLongClick = { if (artwork != null) fullScreen = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = "Album artwork",
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

    if (fullScreen && artwork != null) {
        Dialog(
            onDismissRequest = { fullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { fullScreen = false },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = artwork,
                    contentDescription = "Album artwork, full screen",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }
    }
}
