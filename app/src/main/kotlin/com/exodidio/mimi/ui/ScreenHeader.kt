package com.exodidio.mimi.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide

/**
 * The title block at the top of every screen. Passing [onBack] is what turns a root
 * screen into a pushed one, which keeps the two looking identical.
 *
 * A root screen carries the large title: thirty-four sp, bold, left aligned, with the
 * actions on the same line at the right. A pushed screen carries the bar title instead:
 * seventeen sp, bold, centred, with the way back on the left, because the large title is
 * for a place and the bar is for where inside it you have gone.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    if (onBack != null) {
        BarHeader(title = title, onBack = onBack, trailing = trailing, modifier = modifier)
    } else {
        LargeHeader(title = title, subtitle = subtitle, trailing = trailing, modifier = modifier)
    }
}

/**
 * The large title of a top-level screen.
 *
 * It is the same at every scroll position: the list underneath is the thing that scrolls,
 * and a title that shrank on the way would take the screen's name with it.
 */
@Composable
fun LargeHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        trailing?.invoke()
    }
}

/** The centred bar title of a pushed screen, with the way back on the left. */
@Composable
fun BarHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            trailing?.invoke()
        }
    }
}

/**
 * The large title that gives way to the bar title once the list has moved.
 *
 * The threshold is small on purpose: the title is a heading for what is at the top of the
 * list, so it should leave as soon as the top has been passed rather than linger over
 * songs it no longer introduces.
 */
@Composable
fun CollapsingHeader(
    title: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val collapsed by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
        }
    }

    Crossfade(targetState = collapsed, label = "large title") { small ->
        if (small) {
            BarHeader(title = title, trailing = trailing, modifier = modifier)
        } else {
            LargeHeader(
                title = title,
                subtitle = subtitle,
                trailing = trailing,
                modifier = modifier,
            )
        }
    }
}

/** "1 song" / "N songs", shared by every screen that counts tracks. */
fun songCountLabel(count: Int): String = if (count == 1) "1 song" else "$count songs"

/** The general form, for the groupings that only need the one wording. */
fun countLabel(count: Int, singular: String): String =
    if (count == 1) "1 $singular" else "$count ${singular}s"

/** "1 album" / "N albums". */
fun albumCountLabel(count: Int): String = if (count == 1) "1 album" else "$count albums"

/** "1 artist" / "N artists". */
fun artistCountLabel(count: Int): String = if (count == 1) "1 artist" else "$count artists"

/** "1 playlist" / "N playlists". */
fun playlistCountLabel(count: Int): String = if (count == 1) "1 playlist" else "$count playlists"

/**
 * A heading between two blocks, such as a shelf on the home screen.
 *
 * Twenty sp bold with room to breathe above it, because a section heading is read before
 * the row it introduces rather than as part of it.
 */
@Composable
fun ListSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
    )
}
