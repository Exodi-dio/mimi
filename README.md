# MIMI

A local, offline music player for Android. Plays your own files. No account, no
ads, no network.

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
| Albums and artists told apart by their own tags, so a compilation does not credit every guest | Equaliser |
| Playlists: make, rename, delete, add and remove songs | Crossfade and gapless |
| Songs sorted by title, artist, album, length, date added or file name, ties broken predictably | Branding / app icon |
| Loved songs, with a shelf of their own on the home screen | R8 shrinking |
| Search across titles, artists and albums | |
| Play / pause / next / previous / seek, and a ten second step either way | |
| Shuffle, repeat off/all/one, playback speed, sleep timer | |
| Play or shuffle any album, artist, genre, year, folder or playlist | |
| Up next queue: reorder, jump, remove, play next, add to queue, clear what is coming | |
| Keep the queue as a playlist | |
| Lyrics read out of the files themselves, timed or not | |
| Background playback, notification, lock screen | |
| Mini player with a progress rule, and a Now Playing screen with artwork, lyrics and queue faces | |
| Resume last track, position, shuffle and repeat | |
| Cover art from the files, falling back to the device's album art index | |
| Listening history: play and skip counts, recently played, most played | |
| Statistics: what the library holds and how much of it has been played | |
| Everything known about a song on one screen, and sharing its file | |
| Settings in tabs: library, playback, appearance, about | |

## Stack

- Kotlin, Jetpack Compose (Material 3)
- **Media3 / ExoPlayer** for playback, behind a `PlaybackEngine` abstraction so
  an FFmpeg-backed engine can be added later without touching the app.
- Room for the cached media library, and for the two things a rescan must not
  touch: loved songs and playlists.
- AGP 9.3.1, Kotlin 2.4.10, Gradle 9.5.0, compileSdk 37.1, minSdk 31 (Android 12+)

## Building

Everything runs in the cloud. `main` is built by `.github/workflows/ci.yml`;
no local toolchain is required or used.

## Licence

GPL-3.0. See `NOTICE` for third-party attribution.
