# Oto Music parity plan

Oto Music's own feature list, taken from its store listing, mapped against what MIMI does
today. This is the working order for closing the gap, not a promise about dates.

The criterion for the order is the one the request named: features that make a library
*clean* before features that make it *pretty*. Splitting artists and genres changes what
every other list shows, so it goes first. Decoration comes last.

## Already there

| Feature | Where |
| --- | --- |
| Synchronised and plain lyrics | `ui/LyricsPanel.kt`, `lyrics/LyricsParser.kt` |
| Word-level lyrics, swept word by word | `lyrics/QrcParser.kt`, `lyrics/KaraokeSweep.kt` |
| Finding lyrics for a song that has none | `ui/FindLyricsDialog.kt` (two databases) |
| Lyrics kept per song | `lyrics/LyricsStore.kt` |
| Transliterated lyrics | `ui/RomanizationButton.kt`, `lyrics/RomanizeLyrics.kt` |
| Equaliser | `ui/EqualizerDialog.kt`, `playback/TrackEqualizer.kt` |
| Sleep timer | `playback/SleepTimer.kt` |
| Queue with reordering, removal, save-as-playlist | `ui/QueuePanel.kt` |
| Playlists that survive a rescan | `library/Playlist.kt` |
| Sorting by title, artist, album, year, added, length | `library/SongSorting.kt` |
| Folders, genres, composers, years, album artist | `ui/Browse.kt`, `library/Groupings.kt` |
| Excluding folders | `ui/ExcludedFoldersDialog.kt` |
| Statistics and listening history | `ui/StatisticsScreen.kt`, `library/PlayStat.kt` |
| Playback speed | `ui/NowPlayingScreen.kt` |
| Loved songs | `ui/LovedScreen.kt` |
| Search with recent searches | `ui/SearchScreen.kt`, `library/RecentSearches.kt` |
| Themes, including a dark one | `ui/theme/` |

## The gap, in the order worth doing it

### 1. Splitting names — the "clean library" core
- **Multiple artists with configurable separators.** A tag reading
  `A & B feat. C` is three artists, and today it is one artist literally called that.
  Every artist list, artist page and search should treat it as three.
- **Multiple genres with configurable separators.** Same problem, same fix, and Oto
  ships both.
- Why first: it changes what the browse screens contain, so doing it after other list
  work would mean redoing that work.
- Risk to state plainly: `Earth, Wind & Fire` is one band and splitting on `,` makes it
  three. Oto has the same flaw, which is exactly why the separators are editable rather
  than fixed. Defaults stay conservative; the setting is the escape hatch.

### 2. Editing what is wrong
- **Song tag editor.** Title, artist, album, album artist, year, track number, genre.
  MediaStore rows can be written on Android 11+, and the scan then refreshes the cache.
- **Album tag editor.** One write applied across an album's tracks.
- This is what a library needs next, because a mis-split or a mangled artist is fixed by
  editing the tag, not by another setting.

### 3. Getting a library in and out
- **Playlist import and export (M3U/M3U8).** Reading inside a playlist file, writing one
  that other players accept.
- **Sharing a song, album or playlist** as a file or a list of names.

### 4. Sound
- **Gapless playback.** Media3 supports it; the engine has to stop seeking between tracks
  and hand the next item over before the first ends.
- **Crossfade** with a length setting, built on the same overlap.
- **Fade in and out** on pause and resume, which is the cheap half of the same idea.
- **ReplayGain.** Read the tag, apply the gain, with track and album modes.

### 5. Reaching it from outside
- **Home screen widgets**, at least one with artwork and transport controls, and one
  compact list.
- **Android Auto.** A media browser service over the existing repository.
- **Casting** is listed by Oto; it is also the only feature here that needs a device to
  test against, and it stays out until the rest is done.

### 6. The look
- **AMOLED black**, an accent and highlight colour picker, and a theme per surface.
- **Artist images and album information** fetched on request, which means the second and
  third reasons this app ever uses the network, and they get the same treatment as the
  lyrics finder: a menu action, never automatic.

## Not doing

- Accounts, sync, or a cloud library. The app reads the listener's files.
- Advertising, ever.
- A subscription tier. Oto has a paid unlock; there is nothing here to unlock.
