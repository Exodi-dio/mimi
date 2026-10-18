package com.exodidio.mimi.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MimiApp() {
    val libraryViewModel: LibraryViewModel = viewModel()

    val tracks by libraryViewModel.tracks.collectAsStateWithLifecycle()
    val scanning by libraryViewModel.scanning.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AudioPermissionGate {
            LibraryScreen(
                tracks = tracks,
                scanning = scanning,
                onRefresh = { libraryViewModel.refresh(force = true) },
                // Playback lands in the next milestone.
                onTrackClick = { },
            )
        }
    }
}
