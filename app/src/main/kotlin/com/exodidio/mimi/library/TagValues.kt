package com.exodidio.mimi.library

/**
 * The tags a song carries, and what the app makes of them.
 *
 * The device reports tags in whatever shape its provider keeps them, which is not the
 * shape a person types: a year may arrive as a date, a track number as a fraction, and a
 * field nobody filled in as the word unknown. Reading them into one set of values is
 * what lets the rest of the app ask for a year rather than for a date.
 */

/** One field of a song's tags. */
enum class TagField {
    TITLE,
    ARTIST,
    ALBUM,
    ALBUM_ARTIST,
    YEAR,
    TRACK_NUMBER,
    GENRE,
}

/**
 * The tags of one song.
 *
 * A field with no value is absent rather than empty, because "no year" and "the year
 * zero" are different facts and only one of them is true.
 */
data class TagValues(
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val genre: String? = null,
)

/**
 * A year, as the number it stands for.
 *
 * An empty field means there is no year, and a year of zero is no year either. Anything
 * that starts with digits is read as one, so a date written into the year field gives
 * the year it belongs to rather than being thrown away.
 */
fun parseYear(raw: String?): Int? {
    val digits = raw.orEmpty().trim().takeWhile { it.isDigit() }
    if (digits.isEmpty()) return null
    val year = digits.toIntOrNull() ?: return null
    return year.takeIf { it > 0 }
}

/**
 * A track number, as the number it stands for.
 *
 * A number written as part of a fraction is read as the part before the slash, which is
 * how a file records the third song of twelve. An empty field means there is no number.
 */
fun parseTrackNumber(raw: String?): Int? {
    val digits = raw.orEmpty().trim().takeWhile { it.isDigit() }
    if (digits.isEmpty()) return null
    val number = digits.toIntOrNull() ?: return null
    return number.takeIf { it > 0 }
}

/**
 * The tags of a song, as the library holds them.
 *
 * A track with no number has the zeroth one, which is how the app says that it does not
 * know where the song sits in its record.
 */
fun Track.tagValues(): TagValues = TagValues(
    title = title,
    artist = artist,
    album = album,
    albumArtist = albumArtist,
    year = year,
    trackNumber = trackNumber.takeIf { it > 0 },
    genre = genre,
)

/**
 * The tags of a song, read from the columns the device reported.
 *
 * The columns are named rather than numbered, so the reading does not depend on the
 * order the provider happened to answer in. A column the provider did not have is read
 * as no value at all, which is the truth: the app does not know it.
 */
fun tagValuesFrom(raw: Map<String, String?>): TagValues = TagValues(
    title = raw["title"].orEmpty().trim(),
    artist = raw["artist"].cleanTag(),
    album = raw["album"].cleanTag(),
    albumArtist = raw["album_artist"].cleanTag(),
    year = parseYear(raw["year"]),
    trackNumber = parseTrackNumber(raw["track"]),
    genre = raw["genre"].cleanTag(),
)

/**
 * A tag value as it should be shown and stored.
 *
 * The word unknown is what the provider writes when it has nothing to say, so it is
 * treated as nothing rather than being shown to the listener as an artist.
 */
private fun String?.cleanTag(): String? {
    val text = this?.trim().orEmpty()
    if (text.isEmpty()) return null
    if (text.equals("<unknown>", ignoreCase = true)) return null
    return text
}

/** What the tag editor found in what was typed. */
sealed interface TagInput {

    /** The fields are good and these are the tags they stand for. */
    data class Valid(val values: TagValues) : TagInput

    /** One of the fields cannot be read, and this is why. */
    data class Invalid(val message: String) : TagInput
}

/**
 * The tags the editor's fields stand for.
 *
 * The year and the track number are checked rather than guessed at: a year that is not a
 * number is a mistake worth pointing out, and quietly reading it as no year would lose
 * what was typed.
 */
fun readTagInput(
    title: String,
    artist: String,
    album: String,
    albumArtist: String,
    year: String,
    trackNumber: String,
    genre: String,
): TagInput {
    val name = title.trim()
    if (name.isEmpty()) {
        return TagInput.Invalid("A song needs a title.")
    }

    val parsedYear = parseYear(year)
    if (year.isNotBlank() && parsedYear == null) {
        return TagInput.Invalid("The year has to be a number.")
    }

    val parsedTrack = parseTrackNumber(trackNumber)
    if (trackNumber.isNotBlank() && parsedTrack == null) {
        return TagInput.Invalid("The track number has to be a number.")
    }

    return TagInput.Valid(
        TagValues(
            title = name,
            artist = artist.trim().ifEmpty { null },
            album = album.trim().ifEmpty { null },
            albumArtist = albumArtist.trim().ifEmpty { null },
            year = parsedYear,
            trackNumber = parsedTrack,
            genre = genre.trim().ifEmpty { null },
        )
    )
}

/**
 * The fields on which two sets of tags differ.
 *
 * Only the fields that differ are written back to a file: rewriting a tag that has not
 * changed asks the platform to do work it has no reason to do, and on some devices
 * doing so is what loses the tags a file already had.
 */
fun TagValues.differencesFrom(other: TagValues): Set<TagField> {
    val changed = mutableSetOf<TagField>()
    if (title != other.title) changed += TagField.TITLE
    if (artist != other.artist) changed += TagField.ARTIST
    if (album != other.album) changed += TagField.ALBUM
    if (albumArtist != other.albumArtist) changed += TagField.ALBUM_ARTIST
    if (year != other.year) changed += TagField.YEAR
    if (trackNumber != other.trackNumber) changed += TagField.TRACK_NUMBER
    if (genre != other.genre) changed += TagField.GENRE
    return changed
}

/**
 * The value of one field, as two values can be compared.
 *
 * An absent value and an empty one are the same thing here, because that is how the
 * platform reports a tag nobody set.
 */
fun TagValues.valueOf(field: TagField): String? = when (field) {
    TagField.TITLE -> title
    TagField.ARTIST -> artist
    TagField.ALBUM -> album
    TagField.ALBUM_ARTIST -> albumArtist
    TagField.YEAR -> year?.toString()
    TagField.TRACK_NUMBER -> trackNumber?.toString()
    TagField.GENRE -> genre
}

/**
 * Whether the platform now reports the tags that were asked for.
 *
 * Only the fields that were changed are looked at: a field the listener did not touch
 * may well be reported differently by the device than the app had it, and that is not a
 * sign that the write failed.
 */
fun readBackMatches(requested: TagValues, actual: TagValues, changed: Set<TagField>): Boolean =
    changed.all { field -> requested.valueOf(field) == actual.valueOf(field) }
