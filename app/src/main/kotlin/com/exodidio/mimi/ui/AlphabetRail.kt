package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One letter on the jump rail and the list position its first item sits at. */
data class AlphabetStop(val letter: Char, val index: Int)

/**
 * The first item under each letter, in the order the rail reads.
 *
 * Letters run A to Z and anything that does not begin with one shares the `#` stop at the
 * end. Only letters the list actually contains appear, so the rail never offers a jump
 * that would land nowhere.
 *
 * Pure, so the positions can be checked without a device.
 */
fun alphabetStops(labels: List<String>): List<AlphabetStop> {
    val firstIndex = LinkedHashMap<Char, Int>()
    labels.forEachIndexed { index, label ->
        val letter = alphabetLetter(label)
        if (letter !in firstIndex) firstIndex[letter] = index
    }

    return firstIndex.keys
        .sortedWith(compareBy<Char> { if (it == '#') 1 else 0 }.thenBy { it })
        .map { letter -> AlphabetStop(letter, firstIndex.getValue(letter)) }
}

/** The rail letter a label belongs under: its first letter, or `#` for anything else. */
fun alphabetLetter(label: String): Char {
    val first = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
    return if (first in 'A'..'Z') first else '#'
}

/**
 * The letter to emphasise for a list scrolled to [firstVisibleIndex].
 *
 * It is the last stop at or before the top of the viewport, which is the letter the
 * listener is actually inside rather than the next one coming up.
 */
fun activeAlphabetLetter(stops: List<AlphabetStop>, firstVisibleIndex: Int): Char? =
    stops.lastOrNull { it.index <= firstVisibleIndex }?.letter ?: stops.firstOrNull()?.letter

/**
 * A slim strip of letters over the end of a long list.
 *
 * It sits in the space the list's own end padding leaves, so it never covers a row, and it
 * lives inside the list's box rather than above the mini player, so it cannot reach the
 * transport. Every letter is its own clickable target with a spoken label, which is what
 * makes the rail usable without seeing it.
 */
@Composable
fun AlphabetRail(
    stops: List<AlphabetStop>,
    active: Char?,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp),
    ) {
        stops.forEach { stop ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(onClickLabel = "Jump to ${stop.letter}") { onJump(stop.index) }
                    .semantics { contentDescription = "Jump to ${stop.letter}" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stop.letter.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                    ),
                    fontWeight = if (stop.letter == active) FontWeight.Bold else FontWeight.Normal,
                    color = if (stop.letter == active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/** Whether a list is long enough to be worth a jump rail. */
fun alphabetRailVisible(count: Int): Boolean = count >= ALPHABET_RAIL_MIN_ITEMS

/** Below this a list is short enough to scroll by hand. */
const val ALPHABET_RAIL_MIN_ITEMS = 30
