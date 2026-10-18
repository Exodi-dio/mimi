package com.exodidio.mimi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.Disc3
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MicVocal
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Settings

@Composable
fun MimiApp() {
    val libraryViewModel: LibraryViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()
    val navigator: NavigationViewModel = viewModel()

    val tracks by libraryViewModel.tracks.collectAsStateWithLifecycle()
    val albums by libraryViewModel.albums.collectAsStateWithLifecycle()
    val artists by libraryViewModel.artists.collectAsStateWithLifecycle()
    val scanning by libraryViewModel.scanning.collectAsStateWithLifecycle()
    val playback by playerViewModel.state.collectAsStateWithLifecycle()

    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    val hasTrack = playback.current != null
    val screen = navigator.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AudioPermissionGate {
            // The player is an overlay, so it takes over the back gesture before the
            // navigation stack does.
            BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
            BackHandler(enabled = !showNowPlaying && navigator.canGoBack) { navigator.pop() }

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
                    Box(modifier = Modifier.weight(1f)) {
                        when (screen) {
                            Screen.Songs -> LibraryScreen(
                                tracks = tracks,
                                scanning = scanning,
                                playingMediaId = playback.current?.mediaId,
                                onRefresh = { libraryViewModel.refresh(force = true) },
                                onTrackClick = { index -> playerViewModel.play(tracks, index) },
                            )

                            Screen.Albums -> AlbumsScreen(
                                albums = albums,
                                scanning = scanning,
                                onAlbumClick = { navigator.push(Screen.AlbumDetail(it.albumId)) },
                            )

                            Screen.Artists -> ArtistsScreen(
                                artists = artists,
                                scanning = scanning,
                                onArtistClick = { navigator.push(Screen.ArtistDetail(it.artist)) },
                            )

                            Screen.Settings -> SettingsScreen(
                                trackCount = tracks.size,
                                scanning = scanning,
                                onRescan = { libraryViewModel.refresh(force = true) },
                            )

                            is Screen.AlbumDetail -> AlbumDetailScreen(
                                albumId = screen.albumId,
                                tracksForAlbum = libraryViewModel::tracksForAlbum,
                                playingMediaId = playback.current?.mediaId,
                                onBack = navigator::pop,
                                onPlay = { list, index -> playerViewModel.play(list, index) },
                            )

                            is Screen.ArtistDetail -> ArtistDetailScreen(
                                artist = screen.artist,
                                tracksForArtist = libraryViewModel::tracksForArtist,
                                albumsForArtist = libraryViewModel::albumsForArtist,
                                playingMediaId = playback.current?.mediaId,
                                onBack = navigator::pop,
                                onAlbumClick = { navigator.push(Screen.AlbumDetail(it.albumId)) },
                                onPlay = { list, index -> playerViewModel.play(list, index) },
                            )
                        }
                    }

                    if (hasTrack) {
                        MiniPlayer(
                            state = playback,
                            onExpand = { showNowPlaying = true },
                            onPlayPause = playerViewModel::togglePlayPause,
                            onNext = playerViewModel::next,
                        )
                    }

                    NavigationBar {
                        Tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = navigator.rootTab == tab.screen,
                                onClick = { navigator.selectTab(tab.screen) },
                                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private class Tab(val screen: Screen, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab(Screen.Songs, "Songs", Lucide.Music),
    Tab(Screen.Albums, "Albums", Lucide.Disc3),
    Tab(Screen.Artists, "Artists", Lucide.MicVocal),
    Tab(Screen.Settings, "Settings", Lucide.Settings),
)
