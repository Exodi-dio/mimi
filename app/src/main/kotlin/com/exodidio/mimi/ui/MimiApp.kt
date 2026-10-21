package com.exodidio.mimi.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.User
import com.exodidio.mimi.library.PlaylistSummary
import com.exodidio.mimi.library.Profile
import com.exodidio.mimi.library.ThemeMode
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.addingHiddenName
import com.exodidio.mimi.library.splitCredits
import com.exodidio.mimi.ui.theme.MimiTheme
import kotlinx.coroutines.launch

/**
 * The shell.
 *
 * Five items at the bottom, which is what fits: home, the library, the songs that were
 * loved, search, and the profile, which opens the drawer rather than going somewhere of
 * its own. How the library is sliced — songs, albums, artists, playlists, genres,
 * composers, years, folders — is a section switcher inside it rather than more tabs, so
 * tabs stay about going somewhere and the switcher stays about looking at the same
 * collection differently.
 *
 * Settings live behind that fifth item because they are five pages now, and a drawer shows
 * all five without spending a tab or a row of pills on the way in.
 */
@Composable
fun MimiApp() {
    val settingsViewModel: SettingsViewModel = viewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // The theme is one of the settings, so the app applies its own rather than taking the
    // one the activity set. Everything inside reads the settings from here, which is also
    // what keeps a display choice from being threaded through every screen that draws.
    MimiTheme(darkTheme = dark) {
        CompositionLocalProvider(
            LocalSettings provides settings,
            LocalSettingsChange provides settingsViewModel::change,
        ) {
            MimiAppContent()
        }
    }
}

@Composable
private fun MimiAppContent() {
    val libraryViewModel: LibraryViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()
    val navigator: NavigationViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    val tracks by libraryViewModel.tracks.collectAsStateWithLifecycle()
    val albums by libraryViewModel.albums.collectAsStateWithLifecycle()
    val artists by libraryViewModel.artists.collectAsStateWithLifecycle()
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val lovedTracks by libraryViewModel.lovedTracks.collectAsStateWithLifecycle()
    val favoriteIds by libraryViewModel.favoriteIds.collectAsStateWithLifecycle()
    val recentlyAdded by libraryViewModel.recentlyAdded.collectAsStateWithLifecycle()
    val recentlyPlayed by libraryViewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val mostPlayed by libraryViewModel.mostPlayed.collectAsStateWithLifecycle()
    val playStats by libraryViewModel.playStats.collectAsStateWithLifecycle()
    val listenDays by libraryViewModel.listenDays.collectAsStateWithLifecycle()
    val scanning by libraryViewModel.scanning.collectAsStateWithLifecycle()
    val lastScanDropped by libraryViewModel.lastScanDropped.collectAsStateWithLifecycle()
    val searchQuery by libraryViewModel.query.collectAsStateWithLifecycle()
    val searchResults by libraryViewModel.searchResults.collectAsStateWithLifecycle()
    val recentSearches by libraryViewModel.recentSearches.collectAsStateWithLifecycle()
    val folders by libraryViewModel.folders.collectAsStateWithLifecycle()
    val playback by playerViewModel.state.collectAsStateWithLifecycle()
    val sleepRemaining by playerViewModel.sleepRemaining.collectAsStateWithLifecycle()
    val sleepAtEndOfTrack by playerViewModel.sleepAtEndOfTrack.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val context = LocalContext.current

    // The profile is one value read by the drawer, the profile page and the fifth nav item,
    // so the files are looked up only when the profile says they changed.
    val storedProfile by profileViewModel.profile.collectAsStateWithLifecycle()
    val profile = storedProfile ?: Profile()
    val pictureRevision = profileViewModel.pictureRevision
    val profileAvatarFile = remember(profile.hasAvatar, pictureRevision) {
        profileViewModel.avatarFile()
    }
    val profileBannerFile = remember(profile.hasBanner, pictureRevision) {
        profileViewModel.bannerFile()
    }
    val navAvatar = rememberProfileImage(profileAvatarFile, 128, pictureRevision)

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    /** Closes the drawer and opens [destination], unless it is already the open screen. */
    fun goFromDrawer(destination: Screen) {
        scope.launch { drawerState.close() }
        if (navigator.current != destination) navigator.openFromDrawer(destination)
    }

    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    // Which face the player shows: the artwork, the words, or what is coming up. The skin
    // turns this into a swipe between pages, but it flips the same state either way.
    var playerFace by rememberSaveable { mutableStateOf(PlayerFace.ARTWORK) }

    // Which song is waiting to be put in a playlist, if any.
    var addTarget by remember { mutableStateOf<Track?>(null) }
    var creatingPlaylist by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaylistSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<PlaylistSummary?>(null) }
    // The queue can be kept, but only once the listener has named it.
    var savingQueue by remember { mutableStateOf(false) }
    // Songs picked out by long-pressing a row. Held as ids rather than songs so a rescan
    // or a re-sort under the list cannot leave the selection pointing at stale rows.
    var picked by remember { mutableStateOf(emptySet<Long>()) }
    var addManyToPlaylist by remember { mutableStateOf<List<Track>?>(null) }

    val hasTrack = playback.current != null
    val screen = navigator.current
    val playingMediaId = playback.current?.mediaId

    // Worked out each time it is read, so an action applies to what the lists are showing
    // now rather than to what they showed when the first row was picked.
    val pickedTracks = tracks.pickedBy(picked) { it.id }

    /**
     * Leaves the player and lands on a screen inside the library.
     *
     * Asking about the song that is playing is a question about the library, so it is
     * answered there rather than in a place of its own.
     */
    fun closePlayerTo(destination: Screen, section: LibrarySection) {
        showNowPlaying = false
        playerFace = PlayerFace.ARTWORK
        navigator.selectTab(Screen.Library)
        navigator.selectSection(section)
        navigator.push(destination)
    }

    /**
     * Hands a song's file to another app.
     *
     * The song is addressed by its media store uri rather than by its path, so the app
     * receiving it is granted the one read it needs and nothing more. Wrapped, because
     * having no app that accepts audio is not a reason to fall over.
     */
    fun share(track: Track) {
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = track.mimeType ?: "audio/*"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(track.contentUri))
                putExtra(Intent.EXTRA_TITLE, track.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // A song's details are the same request wherever the song is found, so the answer
        // is handed down rather than threaded through every list that draws a row.
        CompositionLocalProvider(
            LocalTrackActions provides TrackActions(
                onShowInfo = { navigator.push(Screen.TrackInfo(it.id)) },
                isSelected = { it in picked },
                isSelecting = { picked.isNotEmpty() },
                onSelect = { picked = picked.toggling(it.id) },
            ),
        ) {
        AudioPermissionGate {
            // The drawer is on top of whatever is behind it, so it takes the back gesture
            // first. The other two are off while it is open rather than racing it.
            BackHandler(enabled = drawerState.isOpen) {
                scope.launch { drawerState.close() }
            }
            // The player is an overlay, so it takes over the back gesture before the
            // navigation stack does.
            BackHandler(enabled = !drawerState.isOpen && showNowPlaying) {
                showNowPlaying = false
            }
            BackHandler(
                enabled = !drawerState.isOpen && !showNowPlaying && navigator.canGoBack
            ) {
                navigator.pop()
            }

            if (showNowPlaying && hasTrack) {
                NowPlayingScreen(
                    state = playback,
                    onClose = { showNowPlaying = false; playerFace = PlayerFace.ARTWORK },
                    onPlayPause = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::next,
                    onPrevious = playerViewModel::previous,
                    onSeek = playerViewModel::seekTo,
                    onSeekBy = playerViewModel::seekBy,
                    onToggleShuffle = playerViewModel::toggleShuffle,
                    onCycleRepeat = playerViewModel::cycleRepeat,
                    face = playerFace,
                    onShowFace = { playerFace = it },
                    onQueueItemClick = playerViewModel::jumpToQueueItem,
                    onQueueMoveUp = { index -> playerViewModel.moveInQueue(index, index - 1) },
                    onQueueMoveDown = { index -> playerViewModel.moveInQueue(index, index + 1) },
                    onQueueRemove = playerViewModel::removeFromQueue,
                    onSaveQueue = { savingQueue = true },
                    onClearUpcoming = playerViewModel::clearUpcoming,
                    sleepRemaining = sleepRemaining,
                    sleepAtEndOfTrack = sleepAtEndOfTrack,
                    onStartSleepTimer = playerViewModel::startSleepTimer,
                    onCancelSleepTimer = playerViewModel::cancelSleepTimer,
                    onStopAfterSong = playerViewModel::stopAfterSong,
                    onSetSpeed = playerViewModel::setSpeed,
                    // Found by id at the moment it is asked for rather than tracked
                    // alongside the player, which reports a new position twice a second.
                    onGoToAlbum = {
                        val track = tracks.firstOrNull { it.contentUri == playback.current?.mediaId }
                        track?.let {
                            closePlayerTo(Screen.AlbumDetail(it.albumId), LibrarySection.ALBUMS)
                        }
                    },
                    onGoToArtist = {
                        // A tag can credit several people, and a screen opens on one of
                        // them: the first name is the one the track is billed under.
                        val credit = tracks
                            .firstOrNull { it.contentUri == playback.current?.mediaId }
                            ?.artist
                        splitCredits(credit, settings.creditSeparators)
                            .firstOrNull()
                            ?.let { closePlayerTo(Screen.ArtistDetail(it), LibrarySection.ARTISTS) }
                    },
                )
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    modifier = Modifier.fillMaxSize(),
                    drawerContent = {
                        AppDrawer(
                            profile = profile,
                            avatarFile = profileAvatarFile,
                            bannerFile = profileBannerFile,
                            pictureRevision = pictureRevision,
                            current = screen,
                            showInsights = settings.showInsights,
                            onOpenProfile = { goFromDrawer(Screen.Profile) },
                            onEditProfile = { goFromDrawer(Screen.Profile) },
                            onSearchSettings = { goFromDrawer(Screen.SettingsSearch) },
                            onOpenSection = { group, section ->
                                goFromDrawer(
                                    Screen.SettingsGroup(
                                        group = group,
                                        reveal = SettingsReveal.Section(section),
                                    )
                                )
                            },
                            onOpenInsights = { goFromDrawer(Screen.Insights) },
                        )
                    },
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            when (screen) {
                                Screen.Home -> HomeScreen(
                                    recentlyAdded = recentlyAdded,
                                    recentlyPlayed = recentlyPlayed,
                                    mostPlayed = mostPlayed,
                                    loved = lovedTracks,
                                    hasLibrary = tracks.isNotEmpty(),
                                    onShuffleAll = { playerViewModel.shuffleAll(tracks) },
                                    onTrackClick = { list, index -> playerViewModel.play(list, index) },
                                )

                                Screen.Library -> LibraryHome(
                                    libraryViewModel = libraryViewModel,
                                    navigator = navigator,
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
                                    onPlayNext = playerViewModel::playNext,
                                    onAddToQueue = playerViewModel::addToQueue,
                                    onAddToPlaylist = { addTarget = it },
                                    onOpenAlbum = { navigator.push(Screen.AlbumDetail(it.albumId)) },
                                    onOpenArtist = { navigator.push(Screen.ArtistDetail(it.artist)) },
                                    onOpenPlaylist = { navigator.push(Screen.PlaylistDetail(it.id)) },
                                    onCreatePlaylist = { creatingPlaylist = true },
                                    onPlayRandomAlbum = {
                                        libraryViewModel.playRandomAlbum { songs ->
                                            playerViewModel.play(songs, 0)
                                        }
                                    },
                                    onOpenScreen = { navigator.push(it) },
                                )

                                Screen.Loved -> LovedScreen(
                                    tracks = lovedTracks,
                                    favoriteIds = favoriteIds,
                                    playingMediaId = playingMediaId,
                                    onToggleLove = libraryViewModel::toggleLoved,
                                    onPlayNext = playerViewModel::playNext,
                                    onAddToQueue = playerViewModel::addToQueue,
                                    onAddToPlaylist = { addTarget = it },
                                    onTrackClick = { index -> playerViewModel.play(lovedTracks, index) },
                                    onShuffle = { list -> playerViewModel.shuffleAll(list) },
                                )

                                Screen.Search -> SearchScreen(
                                    query = searchQuery,
                                    onQueryChange = libraryViewModel::onQueryChange,
                                    results = searchResults,
                                    playingMediaId = playingMediaId,
                                    favoriteIds = favoriteIds,
                                    onToggleLove = libraryViewModel::toggleLoved,
                                    onPlayNext = playerViewModel::playNext,
                                    onAddToQueue = playerViewModel::addToQueue,
                                    onAddToPlaylist = { addTarget = it },
                                    onTrackClick = { index ->
                                        // A result being worth playing is the sign the search
                                        // was worth making, so that is when it is remembered.
                                        libraryViewModel.rememberSearch()
                                        playerViewModel.play(searchResults, index)
                                    },
                                    recentSearches = recentSearches,
                                    onPickRecent = libraryViewModel::onQueryChange,
                                    onForgetRecent = libraryViewModel::forgetSearches,
                                    onSubmit = libraryViewModel::rememberSearch,
                                )

                                Screen.SettingsSearch -> SettingsSearchScreen(
                                    onPick = { entry ->
                                        // The result opens the page it lives on, with the
                                        // row it names already in view.
                                        navigator.openFromDrawer(
                                            Screen.SettingsGroup(
                                                group = entry.group,
                                                reveal = SettingsReveal.Row(entry.key),
                                            )
                                        )
                                    },
                                    onBack = navigator::pop,
                                )

                                Screen.Profile -> ProfileScreen(
                                    profileViewModel = profileViewModel,
                                    onBack = navigator::pop,
                                )

                                is Screen.SettingsGroup -> SettingsGroupScreen(
                                    group = screen.group,
                                    settings = settings,
                                    onChange = settingsViewModel::change,
                                    trackCount = tracks.size,
                                    tracks = tracks,
                                    scanning = scanning,
                                    lastScanDropped = lastScanDropped,
                                    onRescan = { libraryViewModel.refresh(force = true) },
                                    onOpenStatistics = { navigator.push(Screen.Statistics) },
                                    folders = folders,
                                    onToggleFolder = { folder ->
                                        // An excluded folder is a statement about the library,
                                        // so it is written as soon as it is made.
                                        settingsViewModel.change { chosen ->
                                            chosen.copy(
                                                excludedFolders = if (folder in chosen.excludedFolders) {
                                                    chosen.excludedFolders - folder
                                                } else {
                                                    chosen.excludedFolders + folder
                                                },
                                            )
                                        }
                                    },
                                    onBack = navigator::pop,
                                    reveal = screen.reveal,
                                )

                                Screen.Statistics -> StatisticsScreen(
                                    stats = libraryStats(
                                        songs = tracks,
                                        albumCount = albums.size,
                                        artistCount = artists.size,
                                        lovedIds = favoriteIds,
                                        playlistCount = playlists.size,
                                        playStats = playStats,
                                    ),
                                    mostPlayed = mostPlayed,
                                    playingMediaId = playingMediaId,
                                    onBack = navigator::pop,
                                    onPlayTrack = { playerViewModel.play(listOf(it), 0) },
                                )

                                Screen.Insights -> InsightsScreen(
                                    tracks = tracks,
                                    albumCount = albums.size,
                                    artistCount = artists.size,
                                    playlistCount = playlists.size,
                                    playStats = playStats,
                                    listenDays = listenDays,
                                    separators = settings.creditSeparators,
                                    playingMediaId = playingMediaId,
                                    onBack = navigator::pop,
                                    onPlayTrack = { playerViewModel.play(listOf(it), 0) },
                                )

                                is Screen.AlbumDetail -> AlbumDetailScreen(
                                    albumId = screen.albumId,
                                    tracksForAlbum = libraryViewModel::tracksForAlbum,
                                    playingMediaId = playingMediaId,
                                    favoriteIds = favoriteIds,
                                    onToggleLove = libraryViewModel::toggleLoved,
                                    onPlayNext = playerViewModel::playNext,
                                    onAddToQueue = playerViewModel::addToQueue,
                                    onAddToPlaylist = { addTarget = it },
                                    onBack = navigator::pop,
                                    onPlay = { list, index -> playerViewModel.play(list, index) },
                                    onShuffle = { list -> playerViewModel.shuffleAll(list) },
                                    onHideAlbum = { name ->
                                        settingsViewModel.change {
                                            it.copy(hiddenAlbums = it.hiddenAlbums.addingHiddenName(name))
                                        }
                                        navigator.pop()
                                    },
                                )

                                is Screen.ArtistDetail -> ArtistDetailScreen(
                                    artist = screen.artist,
                                    tracksForArtist = libraryViewModel::tracksForArtist,
                                    albumsForArtist = libraryViewModel::albumsForArtist,
                                    playingMediaId = playingMediaId,
                                    favoriteIds = favoriteIds,
                                    onToggleLove = libraryViewModel::toggleLoved,
                                    onPlayNext = playerViewModel::playNext,
                                    onAddToQueue = playerViewModel::addToQueue,
                                    onAddToPlaylist = { addTarget = it },
                                    onBack = navigator::pop,
                                    onAlbumClick = { navigator.push(Screen.AlbumDetail(it.albumId)) },
                                    onPlay = { list, index -> playerViewModel.play(list, index) },
                                    onShuffle = { list -> playerViewModel.shuffleAll(list) },
                                    onHideArtist = { name ->
                                        settingsViewModel.change {
                                            it.copy(hiddenArtists = it.hiddenArtists.addingHiddenName(name))
                                        }
                                        navigator.pop()
                                    },
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
                                        onShuffle = { list -> playerViewModel.shuffleAll(list) },
                                        onPlayNext = playerViewModel::playNext,
                                        onAddToQueue = playerViewModel::addToQueue,
                                        onRemove = { trackId ->
                                            libraryViewModel.removeFromPlaylist(screen.playlistId, trackId)
                                        },
                                        onMove = { trackId, toIndex ->
                                            libraryViewModel.moveInPlaylist(
                                                screen.playlistId,
                                                trackId,
                                                toIndex,
                                            )
                                        },
                                        onRename = { renameTarget = playlist },
                                        onDelete = { deleteTarget = playlist },
                                    )
                                }

                                is Screen.GenreDetail -> TrackListScreen(
                                    title = screen.genre,
                                    tracks = tracksOf(screen.genre, libraryViewModel::tracksInGenre),
                                    playingMediaId = playingMediaId,
                                    favoriteIds = favoriteIds,
                                    onBack = navigator::pop,
                                    onPlay = { list, index -> playerViewModel.play(list, index) },
                                    onShuffle = { list -> playerViewModel.shuffleAll(list) },
                                    onToggleLove = libraryViewModel::toggleLoved,
                                    onPlayNext = playerViewModel::playNext,
                                    onAddToQueue = playerViewModel::addToQueue,
                                    onAddToPlaylist = { addTarget = it },
                                    emptyMessage = "No songs are filed under this genre.",
                                )

                                is Screen.ComposerDetail -> TrackListScreen(
                                    title = screen.composer,
                                    tracks = tracksOf(screen.composer, libraryViewModel::tracksByComposer),
                                    playingMediaId = playingMediaId,
                                    favoriteIds = favoriteIds,
                                    onBack = navigator::pop,
                                    onPlay = { list, index -> playerViewModel.play(list, index) },
                                    onShuffle = { list -> playerViewModel.shuffleAll(list) },
                                    onToggleLove = libraryViewModel::toggleLoved,
                                    onPlayNext = playerViewModel::playNext,
                                    onAddToQueue = playerViewModel::addToQueue,
                                    onAddToPlaylist = { addTarget = it },
                                    emptyMessage = "No songs are credited to this composer.",
                                )

                                is Screen.YearDetail -> TrackListScreen(
                                    title = screen.year.toString(),
                                    tracks = tracksOf(screen.year, libraryViewModel::tracksInYear),
                                    playingMediaId = playingMediaId,
                                    favoriteIds = favoriteIds,
                                    onBack = navigator::pop,
                                    onPlay = { list, index -> playerViewModel.play(list, index) },
                                    onShuffle = { list -> playerViewModel.shuffleAll(list) },
                                    onToggleLove = libraryViewModel::toggleLoved,
                                    onPlayNext = playerViewModel::playNext,
                                    onAddToQueue = playerViewModel::addToQueue,
                                    onAddToPlaylist = { addTarget = it },
                                    emptyMessage = "No songs are tagged with this year.",
                                )

                                is Screen.FolderDetail -> {
                                    val name = screen.folder.substringAfterLast('/').ifEmpty { screen.folder }
                                    TrackListScreen(
                                        title = name,
                                        // The whole path, because two folders can share a last
                                        // segment. Falls back to the song count when it is just
                                        // the folder's own name again.
                                        subtitle = screen.folder.takeIf { it != name },
                                        tracks = tracksOf(screen.folder, libraryViewModel::tracksInFolder),
                                        playingMediaId = playingMediaId,
                                        favoriteIds = favoriteIds,
                                        onBack = navigator::pop,
                                        onPlay = { list, index -> playerViewModel.play(list, index) },
                                        onShuffle = { list -> playerViewModel.shuffleAll(list) },
                                        onToggleLove = libraryViewModel::toggleLoved,
                                        onPlayNext = playerViewModel::playNext,
                                        onAddToQueue = playerViewModel::addToQueue,
                                        onAddToPlaylist = { addTarget = it },
                                        emptyMessage = "This folder holds no music.",
                                    )
                                }

                                is Screen.TrackInfo -> {
                                    val track = tracks.firstOrNull { it.id == screen.trackId }
                                    TrackInfoScreen(
                                        track = track,
                                        stat = playStats[screen.trackId],
                                        playingMediaId = playingMediaId,
                                        onBack = navigator::pop,
                                        onPlay = { track?.let { playerViewModel.play(listOf(it), 0) } },
                                        onPlayNext = { track?.let(playerViewModel::playNext) },
                                        onAddToQueue = { track?.let(playerViewModel::addToQueue) },
                                        onAddToPlaylist = { track?.let { addTarget = it } },
                                        onShare = { track?.let(::share) },
                                        onGoToAlbum = {
                                            track?.let {
                                                navigator.pop()
                                                navigator.push(Screen.AlbumDetail(it.albumId))
                                            }
                                        },
                                        onGoToArtist = {
                                            track?.let {
                                                splitCredits(it.artist, settings.creditSeparators)
                                                    .firstOrNull()
                                                    ?.let { artist ->
                                                        navigator.pop()
                                                        navigator.push(Screen.ArtistDetail(artist))
                                                    }
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        if (picked.isNotEmpty()) {
                            SelectionBar(
                                count = picked.size,
                                onClose = { picked = emptySet() },
                                onLove = {
                                    // One button for both directions: if everything picked is
                                    // already loved it un-loves, and otherwise it loves what is
                                    // not, so a second press undoes the first.
                                    val allLoved = pickedTracks.isNotEmpty() &&
                                        pickedTracks.all { it.id in favoriteIds }
                                    pickedTracks.forEach { track ->
                                        if ((track.id in favoriteIds) == allLoved) {
                                            libraryViewModel.toggleLoved(track.id)
                                        }
                                    }
                                    picked = emptySet()
                                },
                                onAddToPlaylist = { addManyToPlaylist = pickedTracks },
                                onAddToQueue = {
                                    pickedTracks.forEach(playerViewModel::addToQueue)
                                    picked = emptySet()
                                },
                            )
                        } else if (hasTrack) {
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
                            // The fifth item is not a destination but the way into the
                            // drawer, so it lights up while the drawer is out and not for
                            // whatever page is behind it. The avatar, when there is one,
                            // makes the whole of settings read as "yours" rather than as
                            // a wrench in the corner.
                            NavigationBarItem(
                                selected = drawerState.isOpen,
                                onClick = { scope.launch { drawerState.open() } },
                                icon = {
                                    if (navAvatar != null) {
                                        Image(
                                            bitmap = navAvatar,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape),
                                        )
                                    } else {
                                        Icon(imageVector = Lucide.User, contentDescription = null)
                                    }
                                },
                                label = { Text("Profile") },
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

            addManyToPlaylist?.let { chosen ->
                AddToPlaylistDialog(
                    playlists = playlists,
                    onPick = { playlist ->
                        chosen.forEach { libraryViewModel.addToPlaylist(playlist.id, it.id) }
                        addManyToPlaylist = null
                        picked = emptySet()
                    },
                    onCreate = { name ->
                        libraryViewModel.createPlaylist(name) { playlistId ->
                            chosen.forEach { libraryViewModel.addToPlaylist(playlistId, it.id) }
                        }
                        addManyToPlaylist = null
                        picked = emptySet()
                    },
                    onDismiss = { addManyToPlaylist = null },
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

            if (savingQueue) {
                NameDialog(
                    title = "Keep the queue",
                    confirmLabel = "Save",
                    initialValue = "Queued songs",
                    onConfirm = { name ->
                        playerViewModel.saveQueueAsPlaylist(name)
                        savingQueue = false
                    },
                    onDismiss = { savingQueue = false },
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
    navigator: NavigationViewModel,
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
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onOpenAlbum: (com.exodidio.mimi.library.AlbumSummary) -> Unit,
    onOpenArtist: (com.exodidio.mimi.library.ArtistSummary) -> Unit,
    onOpenPlaylist: (PlaylistSummary) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlayRandomAlbum: () -> Unit,
    onOpenScreen: (Screen) -> Unit,
) {
    // Collected here rather than passed in: these four are only ever shown in this one
    // place, and the shell is already carrying enough lists.
    val genres by libraryViewModel.genres.collectAsStateWithLifecycle()
    val composers by libraryViewModel.composers.collectAsStateWithLifecycle()
    val years by libraryViewModel.years.collectAsStateWithLifecycle()
    val folders by libraryViewModel.folders.collectAsStateWithLifecycle()

    // The position is read once, when this section is rebuilt. Keeping it keyed by the
    // order and the hidden names means a reordering starts at the top instead of at an
    // offset that now points somewhere else.
    val settings = LocalSettings.current
    val scrollKey = libraryScrollKey(section, settings)
    val initialScroll = remember(scrollKey) { navigator.scrollIndex(scrollKey) }
    val keepScroll: (Int) -> Unit = remember(scrollKey) {
        { index -> navigator.rememberScroll(scrollKey, index) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LibrarySectionSwitcher(selected = section, onSelect = onSectionChange)
        // A rule under the pills, so they read as a way of looking at the library rather
        // than as the first thing in it. The settings pages are arranged the same way.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when (section) {
            LibrarySection.SONGS -> LibraryScreen(
                tracks = tracks,
                scanning = scanning,
                favoriteIds = favoriteIds,
                onToggleLove = libraryViewModel::toggleLoved,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onRefresh = { libraryViewModel.refresh(force = true) },
                playingMediaId = playingMediaId,
                onPlay = onPlay,
                initialScrollIndex = initialScroll,
                onScrollIndex = keepScroll,
            )

            LibrarySection.ALBUMS -> AlbumsScreen(
                albums = albums,
                scanning = scanning,
                onAlbumClick = onOpenAlbum,
                onPlayRandomAlbum = onPlayRandomAlbum,
                initialScrollIndex = initialScroll,
                onScrollIndex = keepScroll,
            )

            LibrarySection.ARTISTS -> ArtistsScreen(
                artists = artists,
                scanning = scanning,
                onArtistClick = onOpenArtist,
                initialScrollIndex = initialScroll,
                onScrollIndex = keepScroll,
            )

            LibrarySection.PLAYLISTS -> PlaylistsScreen(
                playlists = playlists,
                onPlaylistClick = onOpenPlaylist,
                onCreate = onCreatePlaylist,
            )

            LibrarySection.GENRES -> BrowseScreen(
                title = "Genres",
                entries = genres.genreEntries(),
                subtitle = countLabel(genres.size, "genre"),
                emptyMessage = "No genres are tagged in your music.",
                onEntryClick = { onOpenScreen(Screen.GenreDetail(it.key)) },
            )

            LibrarySection.COMPOSERS -> BrowseScreen(
                title = "Composers",
                entries = composers.composerEntries(),
                subtitle = countLabel(composers.size, "composer"),
                emptyMessage = "No composers are credited in your music.",
                onEntryClick = { onOpenScreen(Screen.ComposerDetail(it.key)) },
            )

            LibrarySection.YEARS -> BrowseScreen(
                title = "Years",
                entries = years.yearEntries(),
                subtitle = countLabel(years.size, "year"),
                emptyMessage = "No years are tagged in your music.",
                onEntryClick = { entry ->
                    entry.key.toIntOrNull()?.let { onOpenScreen(Screen.YearDetail(it)) }
                },
            )

            LibrarySection.FOLDERS -> BrowseScreen(
                title = "Folders",
                entries = folders.folderEntries(),
                subtitle = countLabel(folders.size, "folder"),
                emptyMessage = "No folders were reported for your music.",
                leadingIcon = Lucide.Folder,
                onEntryClick = { onOpenScreen(Screen.FolderDetail(it.key)) },
            )
        }
    }
}

private class Tab(val screen: Screen, val label: String, val icon: ImageVector)

/**
 * The destinations in the navigation bar.
 *
 * The fifth item is not here on purpose: it opens the drawer rather than going somewhere,
 * so it has no screen to be lit for and is drawn beside these rather than from this list.
 */
private val Tabs = listOf(
    Tab(Screen.Home, "Home", Lucide.House),
    Tab(Screen.Library, "Library", Lucide.Music),
    Tab(Screen.Loved, "Loved", Lucide.Heart),
    Tab(Screen.Search, "Search", Lucide.Search),
)
