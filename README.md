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
| Albums and artists told apart by their own tags, so a compilation does not credit every guest | Crossfade and gapless |
| Playlists: make, rename, delete, add, remove and reorder songs | Branding / app icon |
| Songs sorted by title, artist, album, length, date added or file name, ties broken predictably | R8 shrinking |
| Albums sorted by name, album artist or song count; artists by name, albums or song count | |
| Loved songs, with a shelf of their own on the home screen | |
| Search across titles, artists and albums, with recent searches offered back | |
| Play / pause / next / previous / seek, and a ten second step either way | |
| Shuffle, repeat off/all/one, playback speed, sleep timer (including end of track) | |
| Play or shuffle any album, artist, genre, year, folder or playlist | |
| Up next queue: reorder, jump, remove, play next, add to queue, clear what is coming | |
| Keep the queue as a playlist | |
| Equaliser with a switch and seven presets, spread across however many bands the device has | |
| Lyrics read out of the files themselves, timed or not | |
| Lyrics looked up for a song whose file has none, from the menu on the now playing screen, one song at a time and only when asked | |
| Found lyrics kept beside the app for that song, with a preview before one is chosen | |
| Transliterated lyrics, switched by a button over the words, working with no connection | |
| One credit line split into the artists and genres actually on it, with the separators editable | |
| Background playback, notification, lock screen | |
| Mini player with a progress rule, and a Now Playing screen with artwork, lyrics and queue faces | |
| Resume last track, position, shuffle and repeat | |
| Cover art from the files, falling back to the device's album art index | |
| Listening history: play and skip counts, recently played, most played | |
| Statistics: what the library holds and how much of it has been played | |
| Everything known about a song on one screen, and sharing its file | |
| Pick several songs at once and love them, queue them or put them in a playlist | |
| Folders left out of the library without deleting anything | |
| Settings in tabs: library, playback, appearance, about | |

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
