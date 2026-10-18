package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.lyrics.Lyrics

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
fun rememberLyrics(mediaId: String?): LyricsState {
    val context = LocalContext.current
    val loader = remember(context) { (context.applicationContext as MimiApplication).lyrics }

    return produceState<LyricsState>(initialValue = LyricsState.Loading, mediaId) {
        val id = mediaId
        if (id == null) {
            value = LyricsState.None
            return@produceState
        }
        val loaded = loader.load(id)
        value = if (loaded.isEmpty) LyricsState.None else LyricsState.Found(loaded)
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
    if (!lyrics.synced) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            lyrics.lines.forEach { line ->
                LyricText(
                    text = line.text,
                    current = false,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val currentIndex = lyrics.indexAt(positionMs)

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) listState.animateScrollToItem(currentIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            LyricText(
                text = line.text,
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
private fun LyricText(text: String, current: Boolean, modifier: Modifier = Modifier) {
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
        modifier = modifier.padding(horizontal = 8.dp),
    )
}
