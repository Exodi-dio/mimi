# Reference: the screens to match

Taken from eleven screenshots supplied on 2026-10-21. This is the layout and the feature
set to reproduce. Read it as a specification, not as inspiration: positions, grouping,
wording and ranges are as shown.

---

## 1. Drawer

A full-height panel, roughly 60% of the width, dark, rounded on its **right** edge only.

```
Oto Music                      <- app name, alone, large, ~28sp, left inset 24dp
────────────────────────────   <- hairline rule, full width, ~20dp below the title

[  Library                 39 ] <- selected row: filled pill (rounded 28dp), icon + label
                                    on the left, count on the right, 56dp tall
  Folders                  1
  Playing queue
  Equalizer
  Listening stats
  Scan media
  Settings
  About
  Donate
```

- Rows are 56dp, icon 24dp with 20dp gap to the label, label ~17sp.
- The **selected** row is the only one with a background; everything else is plain.
- Counts appear only on Library and Folders.
- No profile, no avatar, no banner in the drawer. Nothing below Donate.
- The drawer overlays the app; the bottom navigation is not visible behind it.

**MIMI's change:** the profile is *not* in navigation at all. It moves to the **top-right
corner of the home screen**, and the drawer takes this shape instead of the settings-only
panel it currently is.

---

## 2. Settings page shape (the pattern every screen follows)

Every settings screen is the same skeleton:

```
<-                                    <- back arrow, 24dp, top left, no title beside it

Library                               <- huge bold title, ~40sp, weight 700
What surfaces in your library, and    <- one or two sentence subtitle, muted, ~16sp,
how artists and genres are grouped.      wraps naturally

MULTIPLE ARTISTS                      <- section heading: ~13sp, letterspaced, uppercase,
                                         muted green
┌──────────────────────────────────┐
│ Multiple artists support    ( o) │ <- card: filled, rounded 24dp, inset 16dp
│ Split artist and album-artist    │    row title bold ~18sp, subtitle muted ~14sp
│ tags with ( , & ; ) separators.  │    toggle on the right, vertically centred
├──────────────────────────────────┤ <- hairline divider inset from both edges
│ Symbols to split artists      >  │
│ Add separators based on which    │
│ artists will be split            │
├──────────────────────────────────┤
│ Always navigate to first artist  │ <- disabled state: title and subtitle dimmed,
│ When go to artist option is      │    toggle greyed
│ clicked, always navigate to the  │
│ first artist                     │
└──────────────────────────────────┘
```

- Groups of rows live inside **one card per section**, not one card per row.
- A row with a value shows it right-aligned in an accent colour plus `>`; a row with a
  toggle shows the toggle; a row that opens a screen shows `>` only.
- A section heading goes **outside and above** its card, never inside it.
- Info notes are their own filled card in a warm tint, icon on the left, muted text.

---

## 3. Library

`Library` / "What surfaces in your library, and how artists and genres are grouped."

**MULTIPLE ARTISTS**
- `Multiple artists support` toggle — "Split artist and album-artist tags with ( , & ; ) separators."
- `Symbols to split artists` `>` — "Add separators based on which artists will be split"
- `Always navigate to first artist` toggle (disabled/greyed) — "When go to artist option is clicked, always navigate to the first artist"

**MULTIPLE GENRES**
- `Multiple genres support` toggle — "Split genre tags with ( , & ; ) separators."
- `Symbols to split genres` `>` — "Add separators based on which genres will be split"

**FILTER**
- `Blacklist` `>` — "Songs from the blacklisted folders will be hidden from your library"
- `Duration range` — value "off" — "Hide songs outside this duration. Drag either thumb; pull the right one to ∞ for no upper limit."
  - A **dual-thumb** slider beneath the row, tinted accent, with ticks `OFF · 1m · 5m · 1h · ∞`.

---

## 4. Audio

`Audio` / "How tracks crossfade, duck for system sounds, and which engine decodes them."

**PLAYBACK**
- `Fade music on play / pause` `BETA` — value `off` — "Smooth fade in/out instead of cutting abruptly."
  - Slider `OFF · 250 · 500 · 750 · 1000`
- `Crossfade` `BETA` — value `off` — "Overlap the end of one track with the start of the next."
  - Slider `OFF · 3s · 6s · 9s · 12s`
- `Gapless playback` toggle — "Remove gaps of silence between the tracks. Might cause playback issues on some devices."
- `Keep shuffle mode` toggle — "Shuffle mode won't turn off when playing a new list of songs."
- `Equalizer` `>` — "Tune EQ bands, bass boost, reverb, and virtualizer."

**ENGINE**
- `Playback engine` — value `System (MediaPlayer)` `>` — "Select the Enhanced engine for best quality and broadest format support."
- `EQ Band Count` — value `10 Bands` `>` — "More bands = finer tone control, slightly more CPU."

**REPLAYGAIN**
- `ReplayGain source mode` — value `None` `>` — "Normalize track-to-track loudness."
- `ReplayGain preamp` `>` (dimmed) — "Set preamp when ReplayGain is enabled"

**MISC**
- `Lower volume on focal loss` toggle — "Lower the volume when a system sound is played or a notification is received"
- `Auto play` toggle — "Start playing immediately when a wired headset or bluetooth audio device is plugged in."

---

## 5. Images

`Images` / "Album artwork, metadata downloads, and what shows on the lock screen."

**STORAGE**
- `Cache images` toggle — "Faster loading of images but will increase app size due to caching."

**METADATA**
- `Use MediaStore provided art` toggle — "Faster artwork loading time, but can show incorrect artwork"
- `Auto download metadata` — value `Always` `>` — "Auto download metadata(artist images, artist bio etc)"

**LOCKSCREEN**
- Info card: "Adds album art to the media notification and controls. It may not appear on the lock screen itself on Android 11 and newer."
- `Lockscreen album art` toggle — "Show album art on the lockscreen"
- `Blur album cover` toggle — "Blur album art on the lockscreen"

---

## 6. Customization

`Customization` / "Tailor what you see on the home and details screens."

**HOME**
- `Manage bottom tabs` `>` — "Reorder and set visibility of bottom tabs"
- `Manage home rows` `>` — "Reorder or hide sections shown on the home tab"

**DETAILS**
- `Show See all button` toggle — "Show or hide See all button in details page."

---

## 7. Now playing (settings for it)

`Now playing` / "How the now-playing screen looks and what controls it shows."

One card:
- `Now Playing Theme` — value `Default` `>`
- `Show song info` toggle — "Show song information such as file format, bitrate and frequency in the main player"
- `Show volume slider` toggle — "Show volume slider in the main player"
- `Extra controls` toggle — "Add previous and forward button in the mini player"

---

## 8. Playlists

`Playlists` / "Where exported playlists are saved, and how recent counts are sliced."

One card:
- `Playlist export path` `>` — "Not set"
- `Last added playlist` — value `Past three months` `>`
- `Recently played` — value `Past three months` `>`

---

## 9. Backup & restore

`Backup & restore` / "Save your library to a file, and bring it back any time."

Info card: "Audio files aren't backed up, only your library data. Songs stay on your device."

**MANUAL**
- `Create backup` `>` — "Save a backup file to a location you choose"
- `Restore from backup` `>` — "Merge data from a backup file into the app"

**AUTOMATIC**
- `Automatic backups` toggle — "Periodically save a backup to a folder"
- `Backup frequency` — value `Daily` `>` (dimmed when automatic is off)
- `Backup folder` `>` — "Not set" (dimmed when automatic is off)

---

## 10. The now playing screen itself

```
            Whoa, hmm                  <- current line: bright, ~34sp, bold, centred

      You been running                 <- surrounding lines: heavily faded, ~34sp bold
      'round, running                  <- centred, they scroll with the song
      'round, running

           Attention                   <- title, ~24sp bold, centred, below the lyrics
          Charlie Puth                 <- artist, ~16sp, muted

  ⟳    ⏮    ( ▶ )    ⏭    ⤨          <- repeat, previous, 64dp filled circle play/pause,
                                         next, shuffle; side buttons ~28sp muted

  ●───────────────────────────────     <- thin progress line, filled dot at the head
  00:08                       03:31    <- elapsed left, total right, ~14sp muted

     FLAC · 940 kb/s · 44.1 kHz        <- one muted line, centred

  ≡    ♥    ☾    ⇄    ⋯               <- queue, heart, sleep timer, queue list, more
```

- The lyrics are the screen. Artwork is not shown while lyrics are up.
- The current line is the only bright one; three or so lines either side are visible and
  fade with distance.
- Title and artist sit **below** the lyric block, above the controls.
- Format, bitrate and sample rate sit below the scrubber.
- The bottom navigation bar is hidden on this screen; the five icons at the very bottom
  are the player's own action row.

---

## 11. Changelog sheet

A bottom sheet titled with the version, sections `NEW`, `TWEAKS`, `FIXED` as small
uppercase labels, each a bullet list, and a `Show less ^` / `Show more` pill at the
bottom. Items claimed:

**NEW**
- Support for QRC (QQ Music) word-level lyric files.
- Song details show when a track was added and when it was last modified, and show
  correct channels.
- Cast almost all audio formats.
- Search offers "Did you mean" suggestions when a query finds nothing.

**TWEAKS**
- Bass boost tighter: a subsonic filter and an automatic preamp trim keep it from
  clipping the rest of the chain.
- Speed & pitch sheet redesigned with split hero tiles and grouped cards.
- The app opens noticeably faster — the now-playing screen is no longer built before the
  first frame is drawn.

**FIXED**
- Cast playback stalling at the end of a track; a track skipped after a temporary cast
  error.
- The tag editor rewriting an untouched year as a truncated date.
- Karaoke lyrics: right-to-left lines sweep word by word, and a Korean or Japanese
  particle no longer breaks off the word it belongs to.

---

## 12. Feature list (the app's own)

Android Auto · Chromecast · Gapless and crossfade · Multiple now playing themes · Fade in
and out on pause/resume · Normal and synchronised lyrics · Multiple artists with custom
separators · Multiple genres with custom separators · Download and edit lyrics and tags
in-app · AMOLED theme · Accent and highlight colour · Sleep timer · ReplayGain · Inbuilt
equaliser · 5+ widgets · Custom playlists that are not deleted · Import and export
playlists · Multiple sorting options · Light, dark, battery saver and system themes ·
Dedicated folders section · Animations and animated icons · Song and album tag editor ·
Download artist images, artist info and album info · Material guidelines · Small size.

---

## User instructions for this round (verbatim)

> "first profile should not be in the navigation, can be on the top right corner of the
> home screen, speaking of Wich remove the shuffle all button in the homescreen, the
> banner and pfp picker or profile maker works flawlessly, but it's still lacking, add
> rezising options after picking an image for both the banner and pfp, next the images
> I've sent here you're gonna use as reference for both features, layout and positioning
> in EVERY aspect. Everything you see here, implement into Mimi as is, together with it's
> feature list and new feature and improvements list, were almost close to skinning"
