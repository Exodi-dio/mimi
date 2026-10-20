package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.ListEnd
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X

/**
 * What can be done to songs that have been picked out.
 *
 * It sits where the mini player sits, so picking songs does not move the list. The count
 * leads because it is the thing that is easy to lose track of: a bar that only says what
 * the buttons do leaves you guessing how much you are about to act on.
 */
@Composable
fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onLove: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 3.dp) {
        Column {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Lucide.X, contentDescription = "Stop picking")
                }
                Text(
                    text = if (count == 1) "1 song picked" else "$count songs picked",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onLove) {
                    Icon(Lucide.Heart, contentDescription = "Love the picked songs")
                }
                IconButton(onClick = onAddToPlaylist) {
                    Icon(Lucide.ListPlus, contentDescription = "Add them to a playlist")
                }
                IconButton(onClick = onAddToQueue) {
                    Icon(Lucide.ListEnd, contentDescription = "Add them to the queue")
                }
            }
        }
    }
}
