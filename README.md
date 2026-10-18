# MIMI

A local, offline music player for Android. Plays your own files. No account, no
ads, no network.

## Status

**Beta.** The engine works and the feature surface is filling in ahead of the
visual pass, on purpose: every screen gets built and made to work while the app
is still plain, so the skin later has something finished to redraw. Nothing here
is styled yet and it is not meant to be.

## What works today

| Working | Not yet |
| --- | --- |
| Library scan, cached so it never rescans on open | Theming / Apple Music visuals |
| Songs, albums, artists, playlists as library sections | Last.fm, EQ, crossfade |
| Playlists: make, rename, delete, add and remove songs | Branding / app icon |
| Loved songs | |
| Search across titles, artists and albums | |
| Play / pause / next / previous / seek | |
| Shuffle, repeat off/all/one | |
| Up next queue: reorder, jump, remove, play next, add to queue | |
| Lyrics read out of the files themselves, timed or not | |
| Background playback, notification, lock screen | |
| Mini player and a plain Now Playing screen | |
| Resume last track, position, shuffle and repeat | |
| Cover art, read from the files themselves | |

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
