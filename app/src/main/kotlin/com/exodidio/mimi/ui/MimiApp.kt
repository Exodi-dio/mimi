package com.exodidio.mimi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MimiApp() {
    val libraryViewModel: LibraryViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()

    val tracks by libraryViewModel.tracks.collectAsStateWithLifecycle()
    val scanning by libraryViewModel.scanning.collectAsStateWithLifecycle()
    val playback by playerViewModel.state.collectAsStateWithLifecycle()

    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    val hasTrack = playback.current != null

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AudioPermissionGate {
            BackHandler(enabled = showNowPlaying) { showNowPlaying = false }

            if (showNowPlaying && hasTrack) {
                NowPlayingScreen(
                    state = playback,
                    onClose = { showNowPlaying = false },
                    onPlayPause = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::next,
                    onPrevious = playerViewModel::previous,
                    onSeek = playerViewModel::seekTo,
                    onToggleShuffle = playerViewModel::toggleShuffle,
                    onCycleRepeat = playerViewModel::cycleRepeat,
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LibraryScreen(
                        tracks = tracks,
                        scanning = scanning,
                        playingMediaId = playback.current?.mediaId,
                        onRefresh = { libraryViewModel.refresh(force = true) },
                        onTrackClick = { index -> playerViewModel.play(tracks, index) },
                        modifier = Modifier.weight(1f),
                    )
                    if (hasTrack) {
                        MiniPlayer(
                            state = playback,
                            onExpand = { showNowPlaying = true },
                            onPlayPause = playerViewModel::togglePlayPause,
                            onNext = playerViewModel::next,
                        )
                    }
                }
            }
        }
    }
}
