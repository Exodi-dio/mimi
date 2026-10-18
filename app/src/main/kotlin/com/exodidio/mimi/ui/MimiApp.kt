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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings
import com.exodidio.mimi.library.PlaylistSummary
import com.exodidio.mimi.library.Track

/**
 * The shell.
 *
 * Four tabs, because four is what fits: the library, the songs that were loved, search
 * and settings. How the library is sliced — songs, albums, artists, playlists — is a
 * section switcher inside it rather than more tabs, so tabs stay about going somewhere
 * and the switcher stays about looking at the same collection differently.
 */
@Composable
fun MimiApp() {
    val libraryViewModel: LibraryViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()
    val navigator: NavigationViewModel = viewModel()

    val tracks by libraryViewModel.tracks.collectAsStateWithLifecycle()
    val albums by libraryViewModel.albums.collectAsStateWithLifecycle()
    val artists by libraryViewModel.artists.collectAsStateWithLifecycle()
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val lovedTracks by libraryViewModel.lovedTracks.collectAsStateWithLifecycle()
    val favoriteIds by libraryViewModel.favoriteIds.collectAsStateWithLifecycle()
    val scanning by libraryViewModel.scanning.collectAsStateWithLifecycle()
    val searchQuery by libraryViewModel.query.collectAsStateWithLifecycle()
    val searchResults by libraryViewModel.searchResults.collectAsStateWithLifecycle()
    val playback by playerViewModel.state.collectAsStateWithLifecycle()

    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    // Which song is waiting to be put in a playlist, if any.
    var addTarget by remember { mutableStateOf<Track?>(null) }
    var creatingPlaylist by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaylistSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<PlaylistSummary?>(null) }

    val hasTrack = playback.current != null
    val screen = navigator.current
    val playingMediaId = playback.current?.mediaId

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
                            Screen.Library -> LibraryHome(
                                libraryViewModel = libraryViewModel,
                                section = navigator.librarySection,
                                onSectionChange = navigator::selectSection,
                                tracks = tracks,
                                albums = albums,
                                artists = artists,
                                playlists = playlists,
                                favoriteIds = favoriteIds,
                                scanning = scanning,
                                playingMediaId = playingMediaId,
                                onPlay = { list, index -> playerViewModel.play(list, index) },
                                onAddToPlaylist = { addTarget = it },
                                onOpenAlbum = { navigator.push(Screen.AlbumDetail(it.albumId)) },
                                onOpenArtist = { navigator.push(Screen.ArtistDetail(it.artist)) },
                                onOpenPlaylist = { navigator.push(Screen.PlaylistDetail(it.id)) },
                                onCreatePlaylist = { creatingPlaylist = true },
                            )

                            Screen.Loved -> LovedScreen(
                                tracks = lovedTracks,
                                favoriteIds = favoriteIds,
                                playingMediaId = playingMediaId,
                                onToggleLove = libraryViewModel::toggleLoved,
                                onAddToPlaylist = { addTarget = it },
                                onTrackClick = { index -> playerViewModel.play(lovedTracks, index) },
                            )

                            Screen.Search -> SearchScreen(
                                query = searchQuery,
                                onQueryChange = libraryViewModel::onQueryChange,
                                results = searchResults,
                                playingMediaId = playingMediaId,
                                favoriteIds = favoriteIds,
                                onToggleLove = libraryViewModel::toggleLoved,
                                onAddToPlaylist = { addTarget = it },
                                onTrackClick = { index -> playerViewModel.play(searchResults, index) },
                            )

                            Screen.Settings -> SettingsScreen(
                                trackCount = tracks.size,
                                scanning = scanning,
                                onRescan = { libraryViewModel.refresh(force = true) },
                            )

                            is Screen.AlbumDetail -> AlbumDetailScreen(
                                albumId = screen.albumId,
                                tracksForAlbum = libraryViewModel::tracksForAlbum,
                                playingMediaId = playingMediaId,
                                favoriteIds = favoriteIds,
                                onToggleLove = libraryViewModel::toggleLoved,
                                onAddToPlaylist = { addTarget = it },
                                onBack = navigator::pop,
                                onPlay = { list, index -> playerViewModel.play(list, index) },
                            )

                            is Screen.ArtistDetail -> ArtistDetailScreen(
                                artist = screen.artist,
                                tracksForArtist = libraryViewModel::tracksForArtist,
                                albumsForArtist = libraryViewModel::albumsForArtist,
                                playingMediaId = playingMediaId,
                                favoriteIds = favoriteIds,
                                onToggleLove = libraryViewModel::toggleLoved,
                                onAddToPlaylist = { addTarget = it },
                                onBack = navigator::pop,
                                onAlbumClick = { navigator.push(Screen.AlbumDetail(it.albumId)) },
                                onPlay = { list, index -> playerViewModel.play(list, index) },
                            )

                            is Screen.PlaylistDetail -> {
                                val playlist = playlists.firstOrNull { it.id == screen.playlistId }
                                PlaylistDetailScreen(
                                    playlistId = screen.playlistId,
                                    name = playlist?.name ?: "Playlist",
                                    tracksForPlaylist = libraryViewModel::tracksInPlaylist,
                                    playingMediaId = playingMediaId,
                                    onBack = navigator::pop,
                                    onPlay = { list, index -> playerViewModel.play(list, index) },
                                    onRemove = { trackId ->
                                        libraryViewModel.removeFromPlaylist(screen.playlistId, trackId)
                                    },
                                    onRename = { renameTarget = playlist },
                                    onDelete = { deleteTarget = playlist },
                                )
                            }
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

            addTarget?.let { track ->
                AddToPlaylistDialog(
                    playlists = playlists,
                    onPick = { playlist ->
                        libraryViewModel.addToPlaylist(playlist.id, track.id)
                        addTarget = null
                    },
                    onCreate = { name ->
                        libraryViewModel.createPlaylist(name) { playlistId ->
                            libraryViewModel.addToPlaylist(playlistId, track.id)
                        }
                        addTarget = null
                    },
                    onDismiss = { addTarget = null },
                )
            }

            if (creatingPlaylist) {
                NameDialog(
                    title = "New playlist",
                    confirmLabel = "Create",
                    onConfirm = { name ->
                        libraryViewModel.createPlaylist(name)
                        creatingPlaylist = false
                    },
                    onDismiss = { creatingPlaylist = false },
                )
            }

            renameTarget?.let { playlist ->
                NameDialog(
                    title = "Rename playlist",
                    confirmLabel = "Save",
                    initialValue = playlist.name,
                    onConfirm = { name ->
                        libraryViewModel.renamePlaylist(playlist.id, name)
                        renameTarget = null
                    },
                    onDismiss = { renameTarget = null },
                )
            }

            deleteTarget?.let { playlist ->
                ConfirmDialog(
                    title = "Delete \u201c${playlist.name}\u201d?",
                    message = "The playlist goes away. The songs stay on your device.",
                    confirmLabel = "Delete",
                    onConfirm = {
                        libraryViewModel.deletePlaylist(playlist.id)
                        deleteTarget = null
                        navigator.popToLibrary()
                    },
                    onDismiss = { deleteTarget = null },
                )
            }
        }
    }
}

/**
 * The library, with its section switcher above whichever slice is showing.
 *
 * Extracted from [MimiApp] because six parameters of list data would otherwise push the
 * shell past the point of being readable.
 */
@Composable
private fun LibraryHome(
    libraryViewModel: LibraryViewModel,
    section: LibrarySection,
    onSectionChange: (LibrarySection) -> Unit,
    tracks: List<Track>,
    albums: List<com.exodidio.mimi.library.AlbumSummary>,
    artists: List<com.exodidio.mimi.library.ArtistSummary>,
    playlists: List<PlaylistSummary>,
    favoriteIds: Set<Long>,
    scanning: Boolean,
    playingMediaId: String?,
    onPlay: (List<Track>, Int) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onOpenAlbum: (com.exodidio.mimi.library.AlbumSummary) -> Unit,
    onOpenArtist: (com.exodidio.mimi.library.ArtistSummary) -> Unit,
    onOpenPlaylist: (PlaylistSummary) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LibrarySectionSwitcher(selected = section, onSelect = onSectionChange)

        when (section) {
            LibrarySection.SONGS -> LibraryScreen(
                tracks = tracks,
                scanning = scanning,
                favoriteIds = favoriteIds,
                onToggleLove = libraryViewModel::toggleLoved,
                onAddToPlaylist = onAddToPlaylist,
                onRefresh = { libraryViewModel.refresh(force = true) },
                playingMediaId = playingMediaId,
                onTrackClick = { index -> onPlay(tracks, index) },
            )

            LibrarySection.ALBUMS -> AlbumsScreen(
                albums = albums,
                scanning = scanning,
                onAlbumClick = onOpenAlbum,
            )

            LibrarySection.ARTISTS -> ArtistsScreen(
                artists = artists,
                scanning = scanning,
                onArtistClick = onOpenArtist,
            )

            LibrarySection.PLAYLISTS -> PlaylistsScreen(
                playlists = playlists,
                onPlaylistClick = onOpenPlaylist,
                onCreate = onCreatePlaylist,
            )
        }
    }
}

private class Tab(val screen: Screen, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab(Screen.Library, "Library", Lucide.Music),
    Tab(Screen.Loved, "Loved", Lucide.Heart),
    Tab(Screen.Search, "Search", Lucide.Search),
    Tab(Screen.Settings, "Settings", Lucide.Settings),
)
