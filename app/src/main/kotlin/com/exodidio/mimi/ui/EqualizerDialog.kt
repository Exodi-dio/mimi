package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.exodidio.mimi.library.EQUALIZER_FLAT_KEY
import com.exodidio.mimi.library.EqualizerFrequenciesHz
import com.exodidio.mimi.library.EqualizerProfile
import com.exodidio.mimi.library.EQUALIZER_LIMIT_DB
import com.exodidio.mimi.library.Settings
import com.exodidio.mimi.library.cleanProfileName
import com.exodidio.mimi.library.frequencyLabel
import com.exodidio.mimi.library.normalizeGain

/**
 * The equaliser, as ten bands you can drag.
 *
 * Laid out as a column of labelled sliders rather than the usual row of vertical bars. Ten
 * vertical bars fit on a phone only if the labels are turned sideways, and the thing a
 * listener actually wants to know while dragging is which frequency they are on and how
 * many decibels they have given it — both of which need to be readable at a glance.
 *
 * The curve is edited in place: dragging a band moves that band and leaves the shape it
 * came from as the preset's name with "(edited)" after it, so it is always clear that the
 * sound is no longer the one that was picked, and Reset puts it back.
 */
@Composable
fun EqualizerDialog(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onDismiss: () -> Unit,
) {
    var naming by remember { mutableStateOf(false) }

    val chosen = settings.equalizerChoices.firstOrNull { it.key == settings.equalizerPresetKey }
    val edited = chosen != null && chosen.gainsDb != settings.equalizerGains

    if (naming) {
        NameDialog(
            title = "New curve",
            confirmLabel = "Save",
            onConfirm = { name ->
                naming = false
                cleanProfileName(name)?.let { cleaned ->
                    val profile = EqualizerProfile(
                        // Named after its own gains from the start, so it survives being
                        // picked and then dragged like any other curve.
                        key = "user_" + cleaned.lowercase().replace(' ', '_') + "_" +
                            settings.equalizerProfiles.size,
                        name = cleaned,
                        gainsDb = settings.equalizerGains,
                        isBuiltIn = false,
                    )
                    onChange {
                        it.copy(
                            equalizerProfiles = it.equalizerProfiles + profile,
                            equalizerPresetKey = profile.key,
                        )
                    }
                }
            },
            onDismiss = { naming = false },
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Equaliser", style = MaterialTheme.typography.titleLarge)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shape the sound", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Off unless you ask for it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.equalizerEnabled,
                        onCheckedChange = { on -> onChange { it.copy(equalizerEnabled = on) } },
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Curve",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = (chosen?.name ?: "Flat") + if (edited) " (edited)" else "",
                    style = MaterialTheme.typography.bodyLarge,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(onClick = { naming = true }) { Text("Save as new") }
                    TextButton(
                        onClick = {
                            // A built-in curve goes back to what it was; the listener's own
                            // is simply rebuilt from the gains it was saved with, which is
                            // the same thing from their side.
                            chosen?.let { curve ->
                                val gains = if (curve.isBuiltIn) {
                                    curve.gainsDb
                                } else {
                                    settings.equalizerProfiles
                                        .firstOrNull { it.key == curve.key }?.gainsDb
                                        ?: curve.gainsDb
                                }
                                onChange { it.copy(equalizerGains = gains) }
                            }
                        },
                    ) { Text(if (edited) "Reset" else "Flat") }
                    if (chosen != null && !chosen.isBuiltIn) {
                        TextButton(
                            onClick = {
                                onChange {
                                    it.copy(
                                        equalizerProfiles = it.equalizerProfiles.filterNot { p ->
                                            p.key == chosen.key
                                        },
                                        equalizerPresetKey = EQUALIZER_FLAT_KEY,
                                        equalizerGains = com.exodidio.mimi.library.EQUALIZER_FLAT_CURVE,
                                    )
                                }
                            },
                        ) { Text("Delete") }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Bands",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                EqualizerFrequenciesHz.forEachIndexed { index, frequency ->
                    val gain = settings.equalizerGains.getOrElse(index) { 0f }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = frequencyLabel(frequency),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = signedDecibels(gain),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (gain == 0f) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            textAlign = TextAlign.End,
                        )
                    }

                    Slider(
                        value = gain,
                        onValueChange = { value ->
                            onChange { current ->
                                current.copy(
                                    equalizerGains = current.equalizerGains.withGain(
                                        index,
                                        normalizeGain(value),
                                    ),
                                )
                            }
                        },
                        valueRange = -EQUALIZER_LIMIT_DB..EQUALIZER_LIMIT_DB,
                        // Half a decibel between the ends, so a drag lands on a number
                        // worth reading rather than on 3.47.
                        steps = 47,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                // Every curve as a line of text rather than a dropdown: there are thirty-odd
                // of them and a list you can scroll reads better than a dialog inside a
                // dialog.
                settings.equalizerChoices.forEach { profile ->
                    val selected = profile.key == settings.equalizerPresetKey
                    TextButton(
                        onClick = {
                            onChange {
                                it.copy(
                                    equalizerPresetKey = profile.key,
                                    equalizerGains = profile.gainsDb,
                                    equalizerEnabled = true,
                                )
                            }
                        },
                    ) {
                        Text(
                            text = profile.name,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Done") }
            }
        }
    }
}

/** Rewrites one band, leaving the other nine alone. */
internal fun List<Float>.withGain(index: Int, gain: Float): List<Float> =
    mapIndexed { at, existing -> if (at == index) gain else existing }

/** A gain as it is read out: sign always shown, so a row of them lines up. */
internal fun signedDecibels(gain: Float): String =
    if (gain > 0f) "+${trimZero(gain)} dB" else "${trimZero(gain)} dB"

/** 3.0 reads as 3, 3.5 stays 3.5. */
internal fun trimZero(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
