package com.exodidio.mimi.ui

/**
 * One part of the changelog: a heading and the lines under it.
 *
 * The heading is stored as it is written rather than as it is shown, so the sheet can
 * decide how a label looks instead of the text carrying the styling with it.
 */
data class ChangelogSection(
    val label: String,
    val entries: List<String>,
)

/**
 * What changed in this build.
 *
 * Written from the commits that shipped rather than from what was planned, so every
 * line here is something the app actually does. It lives in the source rather than in a
 * file to be parsed, because a changelog that cannot be read without a parser is one
 * that gets left out of the next build.
 */
val changelogSections: List<ChangelogSection> = listOf(
    ChangelogSection(
        label = "New",
        entries = listOf(
            "ReplayGain is read from the file's own tags, by track or by album, with a " +
                "preamp of your own.",
            "Tracks can be crossfaded, so the end of one fades down under the start of the next.",
            "A bass boost with a subsonic filter and a trim, so the weight cannot clip the " +
                "rest of the mix.",
            "Gapless playback can be turned off, which leaves a short silence between the tracks.",
            "Song details show when a file was added to the device and when it was last changed.",
            "Song details show how many channels a file has, read from the file itself.",
            "Search offers near matches when a query finds nothing.",
            "A What's new sheet on the About page lists what changed in this build.",
            "Speed and pitch sit in one sheet, with the current value and the presets side by side.",
            "The player is built around the lyrics, with the line being sung in the middle " +
                "and the lines around it fading away from it.",
            "A profile sits at the top right of the home screen, with a picture and a " +
                "banner that can be cropped and zoomed.",
            "Charts of the library and of listening over seven days, thirty days or all time.",
            "Settings moved behind a drawer, with the profile out of the navigation.",
            "Lyrics can be looked up for a song whose file has none, one song at a time, " +
                "and kept beside the app.",
            "Chinese and Korean lyrics can be read in latin letters.",
            "Artists and genres can be split at separators of your own choosing.",
            "The equaliser uses the band count the phone reports, and each band can be " +
                "dragged by hand.",
            "Folders can be left out of the library without deleting anything.",
            "Library lists can be sorted, searched in place and summarised in their header.",
            "Duplicate copies of a song can be found and reviewed without deleting anything.",
            "Two widgets for the home screen: what is playing, with play and next beside it, " +
                "and a scrolling list of the library's songs.",
            "The library can be browsed from a car or another app, as songs, albums and " +
                "artists, and the music can be played from there.",
        ),
    ),
    ChangelogSection(
        label = "Tweaks",
        entries = listOf(
            "Every settings page now has one shape: a title, a line about the page, and " +
                "one card per section.",
            "The song details screen lists the file's own facts: format, size, dates and " +
                "where it lives.",
            "The letter rail jumps to a song without holding the screen up while it goes.",
            "Hidden artists and albums stay hidden everywhere, counts included.",
        ),
    ),
    ChangelogSection(
        label = "Fixed",
        entries = listOf(
            "The transport icons on the player draw, and the lyrics stop following a " +
                "finger that is dragging them.",
            "Auto play on a wired headset reads the state the platform actually sends.",
            "A lyrics answer that never finished is dropped rather than shown half read.",
            "The latin-letter reading is back in the lyrics panel, and it reads the " +
                "setting when the panel opens.",
            "The listening streak counts the days that were actually heard.",
            "The excluded folders choice is listed in the dialog that offers it.",
        ),
    ),
)

/**
 * How many lines a section keeps while it is closed.
 *
 * Enough to say what changed without turning the sheet into a wall of text, with the
 * rest one tap away.
 */
private const val COLLAPSED_ENTRIES = 3

/**
 * The lines of a section that are shown.
 *
 * A closed section keeps its first lines and a shown one keeps all of them, so the sheet
 * and the pill at the bottom of it cannot disagree about what is on screen.
 */
fun visibleChangelogEntries(entries: List<String>, expanded: Boolean): List<String> =
    if (expanded) entries else entries.take(COLLAPSED_ENTRIES)

/**
 * Whether a section has lines the pill can open up.
 *
 * A section short enough to fit is not given a pill that would do nothing when it is
 * pressed.
 */
fun changelogHasMore(entries: List<String>): Boolean = entries.size > COLLAPSED_ENTRIES

/**
 * What the pill at the bottom of the sheet says.
 *
 * The words name the action rather than the state, so the pill says what pressing it
 * will do.
 */
fun changelogToggleLabel(expanded: Boolean): String = if (expanded) "Show less ^" else "Show more"
