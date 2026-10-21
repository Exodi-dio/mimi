package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
 * The words, with the line being sung picked out and the list following along.
 *
 * Untimed words still scroll; they simply have nothing to follow and nothing to seek to,
 * which is the honest thing to do with a plain block of text.
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
    var showLatin by remember(lyrics) { mutableStateOf(LocalSettings.current.romanizeLyrics) }
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
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()
            val currentIndex = lyrics.indexAt(positionMs)

            LaunchedEffect(currentIndex) {
                if (currentIndex >= 0) listState.animateScrollToItem(currentIndex)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    LyricText(
                        text = line.text,
                        romanised = romanised?.getOrNull(index).orEmpty(),
                        current = index == currentIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = line.timeMs != null) {
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

@Composable
private fun LyricText(
    text: String,
    current: Boolean,
    modifier: Modifier = Modifier,
    romanised: String = "",
) {
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Text(
            text = text,
            style = if (current) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = if (current) FontWeight.SemiBold else null,
            color = if (current) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // Under the original rather than instead of it: reading a transliteration is
        // easier with the characters it came from still in sight.
        if (romanised.isNotBlank() && romanised != text) {
            Text(
                text = romanised,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (current) 0.9f else 0.6f),
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp, start = 28.dp, end = 28.dp),
                    )
                } else {
                    Text(
                        text = "Turn on \u201cLook for lyrics online\u201d in Settings to search for them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp, start = 28.dp, end = 28.dp),
                    )
                }
            }
        }
    }
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
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
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
