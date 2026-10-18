# MIMI

A local, offline music player for Android. Plays your own files. No account, no
ads, no network.

## Status

**Pre-beta.** The foundations build is in place; the beta-1 feature surface
(library scan, playback, queue, shuffle/repeat, background playback, plain Now
Playing) is being built step by step.

## Beta 1 scope

| In | Out (for now) |
| --- | --- |
| Device library scan + cached results | Theming / Apple Music visuals |
| Play / pause / next / previous / seek | Lyrics |
| Shuffle, repeat | Artists, albums, playlists |
| Background playback + notification | Last.fm, EQ, crossfade |
| Plain Now Playing screen | Branding / app icon |
| Resume last track | |

## Stack

- Kotlin, Jetpack Compose (Material 3)
- **Media3 / ExoPlayer** for playback, behind a `PlaybackEngine` abstraction so
  an FFmpeg-backed engine can be added later without touching the app.
- Room for the cached media library
- AGP 9.3.1, Kotlin 2.4.10, Gradle 9.5.0, compileSdk 36, minSdk 31 (Android 12+)

## Building

Everything runs in the cloud. `main` is built by `.github/workflows/ci.yml`;
no local toolchain is required or used.

## Licence

GPL-3.0. See `NOTICE` for third-party attribution.
