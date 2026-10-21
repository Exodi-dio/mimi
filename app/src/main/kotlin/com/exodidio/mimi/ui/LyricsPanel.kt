package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.lyrics.AndroidRomanizationEngine
import com.exodidio.mimi.lyrics.Lyrics
import com.exodidio.mimi.lyrics.RomanizeLyrics
import com.exodidio.mimi.lyrics.RomanizedStatus

/** What there is to show for the song playing right now. */
sealed interface LyricsState {
    /** Still reading the file. */
    data object Loading : LyricsState

    /** The song carries no words of its own. */
    data object None : LyricsState

    data class Found(val lyrics: Lyrics) : LyricsState
}

/**
 * The words for [mediaId], read out of the file itself.
 *
 * Keyed on the track, so skipping to the next song drops the previous words rather than
 * showing them over the new one for a moment.
 */
@Composable
fun rememberLyrics(mediaId: String?, version: Int = 0): LyricsState {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as MimiApplication }

    return produceState<LyricsState>(initialValue = LyricsState.Loading, mediaId, version) {
        val id = mediaId
        if (id == null) {
            value = LyricsState.None
            return@produceState
        }
        // What the file says first, always. Words found and kept are a substitute for a
        // file with none, never a replacement for one that has them.
        val inFile = app.lyrics.load(id)
        val words = if (inFile.isEmpty) app.lyricsStore.load(id) else inFile
        value = if (words.isEmpty) LyricsState.None else LyricsState.Found(words)
    }.value
}

/**
 * The words, with the line being sung picked out and the rest fading away from it.
 *
 * The song is the scroll: the line being sung sits in the middle of the block and the
 * words around it fade with their distance from it, so the sheet reads as one line of
 * light in a dimmer page. A listener who scrolls away takes the window with them and
 * the song takes it back when the next line starts.
 *
 * Untimed words still scroll; they simply have nothing to follow and nothing to seek
 * to, which is the honest thing to do with a plain block of text.
 */
@Composable
fun LyricsPanel(
    lyrics: Lyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The button over the words owns the choice, starting from the setting so a listener
    // who always wants the readings does not press it again for every song.
    val startInLatin = LocalSettings.current.romanizeLyrics
    var showLatin by remember(lyrics) { mutableStateOf(startInLatin) }
    val romanised = rememberRomanised(lyrics, showLatin)

    Box(modifier = modifier.fillMaxSize()) {
        if (!lyrics.synced) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // Room on the right for the button, so the ends of long lines are not
                    // hidden underneath it.
                    .padding(top = 8.dp, bottom = 8.dp, end = 52.dp),
            ) {
                lyrics.lines.forEachIndexed { index, line ->
                    LyricText(
                        text = line.text,
                        romanised = romanised?.getOrNull(index).orEmpty(),
                        current = false,
                        alpha = 1f,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()
            val currentIndex = lyrics.indexAt(positionMs)

            // Whether the listener has left the line being sung. The song takes the
            // window back as soon as the next line starts, so scrolling away is a way of
            // reading ahead rather than a way of losing the words.
            var scrolledAway by remember(lyrics) { mutableStateOf(false) }

            // Only a finger counts. The scroll the song makes is not the listener's,
            // so it must not be mistaken for one.
            val dragged by listState.interactionSource.collectIsDraggedAsState()

            LaunchedEffect(dragged) {
                if (dragged) scrolledAway = true
            }

            val focusIndex = if (scrolledAway) listState.firstVisibleItemIndex else currentIndex
            val window = remember(lyrics.lines, focusIndex) { lyricWindow(lyrics.lines, focusIndex) }
            val distances = window.associate { it.index to it.distance }

            // The block is padded by half a screen at each end so that the first and the
            // last line can sit in the middle of it rather than at the edge.
            val density = LocalDensity.current
            val viewportHeight = listState.layoutInfo.viewportSize.height
            val halfViewport = with(density) { (viewportHeight / 2).toDp() }

            LaunchedEffect(currentIndex, viewportHeight) {
                if (currentIndex < 0 || viewportHeight <= 0) return@LaunchedEffect
                scrolledAway = false
                // An estimate of the line's own height, which is enough to put the words
                // in the middle of the block rather than a third of the way down it.
                val lineHeight = with(density) { 44.dp.toPx() }
                val offset = -((viewportHeight - lineHeight) / 2).toInt()
                listState.animateScrollToItem(
                    index = currentIndex.coerceAtMost(lyrics.lines.lastIndex),
                    scrollOffset = offset,
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = halfViewport, bottom = halfViewport, end = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val distance = distances[index] ?: (LYRIC_SIDE_LINES + 1)
                    val visible = distance <= LYRIC_SIDE_LINES
                    LyricText(
                        text = line.text,
                        romanised = romanised?.getOrNull(index).orEmpty(),
                        current = index == currentIndex,
                        alpha = if (index == currentIndex) 1f else lyricAlpha(distance),
                        modifier = Modifier
                            .fillMaxWidth()
                            // A line nobody can see is not something to be tapped: the
                            // words that are visible are the ones that can be seeked to.
                            .clickable(enabled = visible && line.timeMs != null) {
                                line.timeMs?.let(onSeek)
                            }
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }

        RomanizationButton(
            enabled = showLatin,
            loading = showLatin && romanised == null,
            onClick = { showLatin = !showLatin },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 2.dp),
        )
    }
}

/** Shown in place of the words when there are none to show. */
@Composable
fun LyricsMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

/**
 * One line of words.
 *
 * The size is fixed rather than scaled with the distance, because the reference sheet
 * keeps every visible line the same size and lets the fading alone carry the distance.
 */
@Composable
private fun LyricText(
    text: String,
    current: Boolean,
    alpha: Float,
    modifier: Modifier = Modifier,
    romanised: String = "",
    fontSize: TextUnit = 33.sp,
) {
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Text(
            text = text,
            fontSize = fontSize,
            lineHeight = fontSize * 1.25f,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (current) 1f else alpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // Under the original rather than instead of it: reading a transliteration is
        // easier with the characters it came from still in sight.
        if (romanised.isNotBlank() && romanised != text) {
            Text(
                text = romanised,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The same lines written out in latin letters, or null when that was not asked for.
 *
 * The engine is kept for the life of the app because it holds the dictionaries once they
 * are read, and rebuilding it per song would read a megabyte of them again each time.
 * Lines it cannot read come back blank rather than as a copy of themselves, so a sheet
 * that is mostly English does not double in length.
 */
@Composable
private fun rememberRomanised(lyrics: Lyrics, enabled: Boolean): List<String>? {
    if (!enabled) return null

    val context = LocalContext.current
    val romaniser = remember(context) {
        RomanizeLyrics(AndroidRomanizationEngine { name -> context.assets.open(name) })
    }

    return produceState<List<String>?>(initialValue = null, lyrics, romaniser) {
        val converted = romaniser(lyrics.lines.map { it.text })
        value = converted.map { line ->
            if (line.status == RomanizedStatus.Converted) line.text else ""
        }
    }.value
}

/**
 * Shown when a song has no words and none were kept for it.
 *
 * Points at the menu rather than offering its own button: looking words up is one action
 * and belongs in one place, and a second door to the same room is how a screen starts to
 * sprawl.
 */
@Composable
fun MissingLyrics(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No lyrics in this file.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Find lyrics in the menu at the top right.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
