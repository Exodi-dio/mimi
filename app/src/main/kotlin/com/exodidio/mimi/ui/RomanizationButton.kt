package com.exodidio.mimi.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * The switch that puts the words into latin letters.
 *
 * A round button over the words rather than a row in settings, because the choice is about
 * the sheet in front of you and belongs next to it. It fills in while on and spins while
 * the dictionaries are being read, since the first press of a session waits on a megabyte
 * of them and a button that looks inert while it works invites a second press.
 *
 * The letters rather than a pictogram: a translate icon is a specific drawing that this
 * icon set may not have, and "A文" says the same thing with nothing to guess.
 */
@Composable
fun RomanizationButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 250),
        label = "romanization fill",
    )
    val ink = if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val description = when {
        loading -> "Reading the words out in latin letters"
        enabled -> "Hide the latin letters"
        else -> "Show the words in latin letters"
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics {
                selected = enabled
                stateDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "A\u6587",
            style = MaterialTheme.typography.labelLarge,
            color = ink,
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = ink,
                strokeWidth = 2.dp,
            )
        }
    }
}
