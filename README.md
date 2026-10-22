# MIMI

A local music player for Android. Plays your own files. No account, no ads.

It works entirely offline except for one thing: if a song has no lyrics in its file, the
menu on the now playing screen can look them up in two public databases. That only ever
happens one song at a time, when you ask. Nothing else touches the network.

## Status

**Beta.** The engine works and the feature surface is in place ahead of the visual
pass, on purpose: every screen was built and made to work while the app was still
plain, so the skin now has something finished to redraw. Nothing here is styled yet
and it is not meant to be.

## What works today

| Working | Not yet |
| --- | --- |
| Library scan, cached so it never rescans on open | Theming / Apple Music visuals |
| Eight library sections: songs, albums, artists, playlists, genres, composers, years, folders | Last.fm scrobbling |
| Albums and artists told apart by their own tags, so a compilation does not credit every guest | A second playback engine |
| Playlists: make, rename, delete, add, remove and reorder songs | Branding / app icon |
| Playlists exported as M3U files into a folder you choose, and imported back from a file another player wrote | |
| Backups of the playlists, the settings and the listening history, written to a file you choose, with automatic backups on a daily or weekly schedule | |
| A tag editor for a song or for a whole album, writing into the file where the system allows it and keeping the correction in the library where it does not | |
| Songs sorted by title, artist, album, length, date added or file name, ties broken predictably | R8 shrinking |
| Albums sorted by name, album artist or song count; artists by name, albums or song count | |
| Every list header counts its songs and running time, with sort and in-list search beside it | |
| Loved songs, with a shelf of their own on the home screen | |
| Search across titles, artists and albums, with recent searches offered back | |
| A search that finds nothing offers the nearest titles, artists and albums instead of a dead end | |
| Play / pause / next / previous / seek, with a draggable progress rule and a bead at its head | |
| Shuffle, repeat off/all/one, playback speed, sleep timer (including end of track) | |
| A speed and pitch sheet with the current value on each tile and the presets beneath it | Pitch shifting on a file the phone's decoder cannot time-stretch |
| Play or shuffle any album, artist, genre, year, folder or playlist | |
| Play or shuffle the loved songs, and pick a random album from the grid | |
| Up next queue: reorder, jump, remove, play next, add to queue, clear what is coming | |
| Keep the queue as a playlist | |
| Equaliser with a switch and seven presets, spread across however many bands the device has | |
| A bass boost with a subsonic filter and a preamp trim, so the weight cannot clip the rest of the mix | |
| Crossfade: the end of one track is faded down under the start of the next, over 3, 6, 9 or 12 seconds | The fade on play and pause, which is remembered but not applied |
| ReplayGain read from the file's own tags, by track or by album, with a preamp of your own | |
| Gapless playback that can be turned off, which leaves a short silence between the tracks | |
| Lyrics read out of the files themselves, timed or not | |
| Word-level lyrics read out of QRC files, with the words lighting up one at a time as the song plays | Word timings from the lyrics services, which only ever give whole lines |
| A line read from right to left swept from the right, and Korean and Japanese particles kept with the word they belong to | |
| Lyrics looked up for a song whose file has none, from the menu on the now playing screen, one song at a time and only when asked | |
| Found lyrics kept beside the app for that song, with a preview before one is chosen | |
| Transliterated lyrics, switched by a button over the words, working with no connection | |
| One credit line split into the artists and genres actually on it, with each list of separators editable | |
| Background playback, notification, lock screen | |
| Mini player with a progress rule, and a Now Playing screen where the lyrics are the screen | |
| The line being sung in the middle of the words, with the lines around it fading away from it | |
| The song's name and artist under the words, and the cover in their place when a song has no lyrics | |
| A volume slider and a file format line, both switched on from the Now playing settings page | |
| The queue behind its own button on the Now Playing screen, and a sideways swipe that reaches it | |
| Extra previous/next buttons in the mini player, switched from Now playing settings | |
| Resume last track, position, shuffle and repeat | |
| Keep shuffle mode across a new list, or start every list unshuffled | |
| Lower the music for a notification instead of stopping, or pause instead | |
| Start playing when a wired or bluetooth headset connects | |
| Cover art from the files, falling back to the device's album art index | |
| Listening history: play and skip counts, time heard per day, recently played, most played | |
| Statistics: what the library holds and how much of it has been played | |
| Insights: counts, formats and growth for the library, and listening charts, genres, artists and top songs over 7 days, 30 days or all time, switchable from About | |
| Last added and Recently played lists, sliced by a period chosen on the Playlists settings page | |
| Everything known about a song on one screen, and sharing its file | |
| Song details also say when a file was added to the device, when it was last changed, and how many channels it has | |
| Pick several songs at once and love them, queue them or put them in a playlist | |
| A duration range that hides short or long files from the scan and the cached library | |
| Blacklisted folders left out of the library without deleting anything | |
| Artists and albums hidden from every list, search and count without deleting anything | |
| A duplicate finder that groups copies by title, artist and length, and only reports them | |
| Each library section remembers where it was scrolled to, and starts afresh when the order changes | |
| A full-screen cover on tap, from the Now Playing screen | |
| A profile at the top right of the home screen: a name, a picture and a banner, kept on the device with no account | |
| A drawer of nine destinations, with counts on Library and Folders and nothing profile-shaped in it | |
| Settings behind one drawer row: an index, eight pages, and a search that opens the page with the row in view | |
| A What's new sheet on the About page, listing what changed in this build | |
| Bottom tabs and home shelves reordered and hidden from the Customization page | |
| Home screen widgets: what is playing, with play and next, and a scrollable list of the library's songs | |
| Android Auto and other media browsers: the library browsed as songs, albums and artists, with the music played from the browser | |

## Stack

- Kotlin, Jetpack Compose (Material 3)
- **Media3 / ExoPlayer** for playback, behind a `PlaybackEngine` abstraction so
  an FFmpeg-backed engine can be added later without touching the app.
- Room for the cached media library, and for the things a rescan must not touch:
  loved songs, playlists and listening history.
- AGP 9.3.1, Kotlin 2.4.10, Gradle 9.5.0, compileSdk 37.1, minSdk 31 (Android 12+)

## Building

Everything runs in the cloud. `main` is built by `.github/workflows/ci.yml`;
no local toolchain is required or used.

## Licence

GPL-3.0. See `NOTICE` for third-party attribution.
