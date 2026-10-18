package com.exodidio.mimi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exodidio.mimi.library.Track
import kotlinx.coroutines.flow.Flow

/**
 * A slice of the library, watched for as long as the screen showing it is.
 *
 * A detail screen is opened with a key — an album id, an artist's name, a folder — and the
 * query for it is remembered against that key, so reopening the same screen reuses the
 * subscription while a different one starts its own.
 */
@Composable
fun <K> tracksOf(key: K, flowFor: (K) -> Flow<List<Track>>): List<Track> =
    remember(key) { flowFor(key) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
        .value
