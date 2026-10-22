package com.exodidio.mimi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The rates the speed tile offers.
 *
 * Half speed to double, in the steps people listen at. They are stops rather than a
 * free scale because a sheet of choices is quicker to use than a slider that has to be
 * dragged to a number nobody can remember.
 */
val speedPresets: List<Float> = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

/**
 * The shifts the pitch tile offers, in semitones up.
 *
 * Zero is off, and the rest go up in steps of a semitone. Downward shifts are left out
 * on purpose: they are the same work with the other sign, and half the choices are
 * enough to say what the tile does.
 */
val pitchPresets: List<Int> = listOf(0, 1, 2, 3, 4, 5)

/**
 * How a rate is written on the tile and on its chips.
 *
 * Whole rates lose their decimal, because "2×" is how a speed is said and "2.0×" only
 * adds a digit. The rest keep the two decimals they are worth.
 */
fun speedLabel(speed: Float): String {
    val rounded = (speed * 100f).roundToInt() / 100f
    val text = if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
    return "$text\u00d7"
}

/**
 * How a shift is written on the tile and on its chips.
 *
 * No shift is called off rather than zero, because zero is a number and off is what it
 * means. The rest are written as they are said: up, by that many semitones.
 */
fun pitchLabel(semitones: Int): String = if (semitones <= 0) "Off" else "+$semitones"

/**
 * The speed and the pitch, in one sheet.
 *
 * They belong together because they are the two ways a listener changes how a song
 * sounds, and until now the speed was a plain list of rates behind a menu item. Each
 * one is a tile with the current value on it and the stops beneath it, so the value
 * being heard and the choice that set it are seen at the same time.
 *
 * The sheet stays open while a value is chosen, because the point of a tile is to see
 * what it now says.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedPitchSheet(
    speed: Float,
    pitchSemitones: Int,
    onSpeed: (Float) -> Unit,
    onPitch: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = "Speed and pitch",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SpeedPitchTile(
                        label = "Speed",
                        value = speedLabel(speed),
                        subtitle = "How fast it plays",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PresetChips(
                        options = speedPresets,
                        selected = speedPresets.firstOrNull { isSameRate(it, speed) },
                        label = { speedLabel(it) },
                        onPick = onSpeed,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    SpeedPitchTile(
                        label = "Pitch",
                        value = pitchLabel(pitchSemitones),
                        subtitle = "Semitones up, speed held",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PresetChips(
                        options = pitchPresets,
                        selected = pitchPresets.firstOrNull { it == pitchSemitones },
                        label = { pitchLabel(it) },
                        onPick = onPitch,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

/**
 * One of the two big numbers at the top of the sheet.
 *
 * The value is the largest thing on the tile because it is the thing being asked about:
 * how fast the music is running, and how far the pitch has been moved.
 */
@Composable
private fun SpeedPitchTile(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * A row of the stops a tile offers.
 *
 * The row scrolls sideways rather than wrapping, because six stops do not fit under
 * half a phone and a sheet that hides two of them is a sheet that cannot be used.
 */
@Composable
private fun <T> PresetChips(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Surface(
                shape = CircleShape,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (active) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clickable { onPick(option) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Whether a stored rate is the same as one of the stops.
 *
 * The player reports a rate as a float and the stops are written out, so the two are
 * compared with a small allowance rather than for equality: a rate that is a hair away
 * from a stop is that stop as far as a listener can tell.
 */
private fun isSameRate(first: Float, second: Float): Boolean = abs(first - second) < 0.01f
