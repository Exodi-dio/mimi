package com.exodidio.mimi.ui

import com.exodidio.mimi.library.ListenDay
import com.exodidio.mimi.library.PlayStat
import com.exodidio.mimi.library.Track
import com.exodidio.mimi.library.daysBetween
import com.exodidio.mimi.library.splitCredits
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * How much of the history a section is about.
 *
 * The two pickers use the same three choices but keep their own selection, because the
 * library grows on a slower scale than listening does and comparing the two over one shared
 * window would be a compromise neither wanted.
 */
enum class InsightPeriod(val label: String, val days: Int?) {
    WEEK("7 days", 7),
    MONTH("30 days", 30),
    ALL("All", null),
}

/** One point or bar on a chart, already labelled for the axis it will be drawn on. */
data class InsightBar(val label: String, val value: Long)

/** One named amount in a breakdown, such as a format or a genre. */
data class InsightSlice(val label: String, val value: Long)

/** An artist and the time heard from them in the chosen period. */
data class InsightArtist(val artist: String, val listenedMs: Long)

/** A song, how long it was heard and how many counted plays it had in the period. */
data class InsightTrack(val track: Track, val listenedMs: Long, val plays: Int)

/**
 * The formats the breakdown can tell apart.
 *
 * Bit depth is not in the cache, so quality is reported as the container the file is in.
 * That is the honest thing the library actually knows, and it still separates a lossless
 * shelf from a shelf of compressed copies.
 */
enum class AudioFormat(val label: String) {
    FLAC("FLAC"),
    MP3("MP3"),
    AAC("AAC/M4A"),
    OGG("OGG/Opus"),
    WAV("WAV"),
    OTHER("Other"),
}

/** What the library holds, counted and shaped for the library section. */
data class InsightLibrary(
    val songs: Int,
    val albums: Int,
    val artists: Int,
    val playlists: Int,
    val sizeBytes: Long,
    val growth: List<InsightBar>,
    val formats: List<InsightSlice>,
)

/** What was heard, shaped for the listening section. */
data class InsightListening(
    val listenedMs: Long,
    val plays: Int,
    val skipRatePercent: Int?,
    val averageSessionMs: Long,
    val streakDays: Int,
    val changePercent: Double?,
    val bars: List<InsightBar>,
    val genres: List<InsightSlice>,
    val artists: List<InsightArtist>,
    val topTracks: List<InsightTrack>,
    val hasHistory: Boolean,
)

/**
 * Adds the library up and builds its growth and format shapes.
 *
 * Counts arrive from the caller because albums and artists are derived in the view model
 * from visible songs, and counting them again here would count hidden names. Growth is
 * cumulative, so the line ends on the number of songs the library holds today rather than
 * on the number added in the last bucket.
 */
fun insightLibrary(
    tracks: List<Track>,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    period: InsightPeriod,
    today: LocalDate,
    zone: ZoneId,
): InsightLibrary = InsightLibrary(
    songs = tracks.size,
    albums = albumCount,
    artists = artistCount,
    playlists = playlistCount,
    sizeBytes = tracks.sumOf { it.sizeBytes.coerceAtLeast(0L) },
    growth = libraryGrowth(tracks, period, today, zone),
    formats = formatBreakdown(tracks),
)

/**
 * Cumulative songs over time, by day for the short periods and by month for all of it.
 *
 * A monthly bucket is the only readable axis once a library is years old, and the short
 * periods are short enough that a month would be a single point.
 */
fun libraryGrowth(
    tracks: List<Track>,
    period: InsightPeriod,
    today: LocalDate,
    zone: ZoneId,
): List<InsightBar> {
    if (tracks.isEmpty()) return emptyList()

    val added = tracks.map { track -> addedOn(track, today, zone) }
    return if (period.days == null) {
        val first = added.minOrNull() ?: today
        val firstMonth = YearMonth.from(first)
        val lastMonth = YearMonth.from(today)
        if (firstMonth.isAfter(lastMonth)) return emptyList()

        val buckets = mutableListOf<YearMonth>()
        var month = firstMonth
        while (!month.isAfter(lastMonth)) {
            buckets += month
            month = month.plusMonths(1)
        }
        buckets.map { bucket ->
            val end = bucket.atEndOfMonth()
            InsightBar(
                label = monthLabel(bucket),
                value = added.count { !it.isAfter(end) }.toLong(),
            )
        }
    } else {
        val start = today.minusDays((period.days - 1).toLong())
        val days = daysBetween(start, period.days)
        days.map { key ->
            val date = LocalDate.parse(key)
            InsightBar(
                label = date.dayOfMonth.toString(),
                value = added.count { !it.isAfter(date) }.toLong(),
            )
        }
    }
}

/** Counts of each format, largest first, with formats the library does not hold left out. */
fun formatBreakdown(tracks: List<Track>): List<InsightSlice> {
    val counts = AudioFormat.entries.associateWith { 0 }.toMutableMap()
    tracks.forEach { track ->
        val format = audioFormatOf(track)
        counts[format] = counts.getValue(format) + 1
    }

    return AudioFormat.entries
        .mapNotNull { format ->
            val count = counts.getValue(format)
            if (count == 0) null else InsightSlice(format.label, count.toLong())
        }
        .sortedByDescending { it.value }
}

/**
 * The format a file is in, read from its MIME type first and its name second.
 *
 * MediaStore sometimes reports a wildcard audio type or nothing at all, so the extension is
 * the fallback rather than a second opinion. Anything unrecognised is Other instead of being
 * guessed at.
 */
fun audioFormatOf(track: Track): AudioFormat =
    audioFormatOf(track.mimeType, track.fileName)

/** The classification behind [audioFormatOf], kept separate so it can be checked alone. */
fun audioFormatOf(mimeType: String?, fileName: String?): AudioFormat {
    val mime = mimeType.orEmpty().lowercase()
    when {
        "flac" in mime -> return AudioFormat.FLAC
        "mpeg" in mime || "mp3" in mime -> return AudioFormat.MP3
        "mp4" in mime || "m4a" in mime || "aac" in mime -> return AudioFormat.AAC
        "ogg" in mime || "opus" in mime -> return AudioFormat.OGG
        "wav" in mime || "wave" in mime -> return AudioFormat.WAV
    }

    return when (fileName.orEmpty().substringAfterLast('.', "").lowercase()) {
        "flac" -> AudioFormat.FLAC
        "mp3" -> AudioFormat.MP3
        "m4a", "mp4", "aac" -> AudioFormat.AAC
        "ogg", "oga", "opus" -> AudioFormat.OGG
        "wav", "wave" -> AudioFormat.WAV
        else -> AudioFormat.OTHER
    }
}

/**
 * Folds the daily rows into everything the listening section shows.
 *
 * Only rows whose song is still in [tracks] are counted, which is the same rule the
 * statistics page uses: history belongs to a song that can still be played. The skip rate
 * is the one number that is all time rather than period bound, because skips were never
 * given a day of their own and pretending a period for them would be an invention.
 */
fun insightListening(
    period: InsightPeriod,
    today: LocalDate,
    days: List<ListenDay>,
    tracks: List<Track>,
    playStats: Map<Long, PlayStat>,
    separators: List<String>,
): InsightListening {
    val present = tracks.mapTo(HashSet()) { it.id }
    val known = days.filter { it.trackId in present }

    val start = period.days?.let { today.minusDays((it - 1).toLong()) }
    val current = known.filter { day -> start == null || day.day >= start.toString() }
    val previous = if (period.days == null || start == null) {
        emptyList()
    } else {
        val previousStart = today.minusDays((period.days * 2 - 1).toLong())
        known.filter { it.day >= previousStart.toString() && it.day < start.toString() }
    }

    val listenedMs = current.sumOf { it.listenedMs.coerceAtLeast(0L) }
    val plays = current.sumOf { it.plays.coerceAtLeast(0) }
    val previousListenedMs = previous.sumOf { it.listenedMs.coerceAtLeast(0L) }

    val visibleStats = playStats.filterKeys { it in present }
    val totalPlays = visibleStats.values.sumOf { it.playCount.toLong() }
    val totalSkips = visibleStats.values.sumOf { it.skipCount.toLong() }
    val attempts = totalPlays + totalSkips

    val listenedDays = known
        .filter { it.listenedMs > 0L }
        .mapTo(HashSet()) { it.day }

    return InsightListening(
        listenedMs = listenedMs,
        plays = plays,
        skipRatePercent = if (attempts <= 0L) {
            null
        } else {
            (totalSkips * 100.0 / attempts).roundToInt()
        },
        averageSessionMs = if (plays <= 0) 0L else listenedMs / plays,
        streakDays = currentStreak(listenedDays, today),
        changePercent = if (previousListenedMs <= 0L) {
            null
        } else {
            (listenedMs - previousListenedMs) * 100.0 / previousListenedMs
        },
        bars = listeningBars(period, today, known),
        genres = genreBreakdown(current, tracks, separators),
        artists = topArtists(current, tracks, separators),
        topTracks = topTracks(current, tracks),
        hasHistory = known.any { it.listenedMs > 0L || it.plays > 0 },
    )
}

/**
 * A run of days ending today, or ending yesterday when today has not been listened to yet.
 *
 * Counting from yesterday keeps a streak alive through the morning rather than breaking it
 * before the first song. Two empty days end it.
 */
fun currentStreak(days: Set<String>, today: LocalDate): Int {
    var cursor = today
    if (today.toString() !in days) cursor = today.minusDays(1)

    var streak = 0
    while (cursor.toString() in days) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

/**
 * Listening per day, or per month when the period is all of it.
 *
 * Empty days are included in the short periods so the shape shows rest as well as use. The
 * all-time axis runs from the first day heard to today for the same reason.
 */
fun listeningBars(
    period: InsightPeriod,
    today: LocalDate,
    days: List<ListenDay>,
): List<InsightBar> {
    if (period.days == null) {
        if (days.isEmpty()) return emptyList()
        val first = days.minOf { it.day }
        val firstMonth = YearMonth.from(LocalDate.parse(first))
        val lastMonth = YearMonth.from(today)
        if (firstMonth.isAfter(lastMonth)) return emptyList()

        val totals = HashMap<YearMonth, Long>()
        days.forEach { row ->
            val month = YearMonth.from(LocalDate.parse(row.day))
            totals[month] = totals.getOrDefault(month, 0L) + row.listenedMs.coerceAtLeast(0L)
        }

        val bars = mutableListOf<InsightBar>()
        var month = firstMonth
        while (!month.isAfter(lastMonth)) {
            bars += InsightBar(monthLabel(month), totals.getOrDefault(month, 0L))
            month = month.plusMonths(1)
        }
        return bars
    }

    val start = today.minusDays((period.days - 1).toLong())
    val totals = days.groupBy { it.day }
        .mapValues { (_, rows) -> rows.sumOf { it.listenedMs.coerceAtLeast(0L) } }

    return daysBetween(start, period.days).map { key ->
        InsightBar(
            label = LocalDate.parse(key).dayOfMonth.toString(),
            value = totals.getOrDefault(key, 0L),
        )
    }
}

/** The five genres heard longest, with everything after them folded into Other. */
fun genreBreakdown(
    days: List<ListenDay>,
    tracks: List<Track>,
    separators: List<String>,
    limit: Int = 5,
): List<InsightSlice> {
    val byTrack = days.groupBy { it.trackId }
        .mapValues { (_, rows) -> rows.sumOf { it.listenedMs.coerceAtLeast(0L) } }
    val byTrackById = tracks.associateBy { it.id }

    val totals = LinkedHashMap<String, Long>()
    byTrack.forEach { (trackId, listenedMs) ->
        if (listenedMs <= 0L) return@forEach
        val track = byTrackById[trackId] ?: return@forEach
        val genre = splitCredits(track.genre, separators).firstOrNull() ?: "No genre"
        totals[genre] = totals.getOrDefault(genre, 0L) + listenedMs
    }

    return topWithOther(totals, limit)
}

/** The artists heard longest, by the first credit on each song. */
fun topArtists(
    days: List<ListenDay>,
    tracks: List<Track>,
    separators: List<String>,
    limit: Int = 5,
): List<InsightArtist> {
    val byTrack = days.groupBy { it.trackId }
        .mapValues { (_, rows) -> rows.sumOf { it.listenedMs.coerceAtLeast(0L) } }
    val byTrackById = tracks.associateBy { it.id }

    val totals = LinkedHashMap<String, Long>()
    byTrack.forEach { (trackId, listenedMs) ->
        if (listenedMs <= 0L) return@forEach
        val track = byTrackById[trackId] ?: return@forEach
        val artist = splitCredits(track.artist, separators).firstOrNull() ?: "Unknown artist"
        totals[artist] = totals.getOrDefault(artist, 0L) + listenedMs
    }

    return totals.entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { InsightArtist(it.key, it.value) }
}

/** The songs heard longest in the period, with the plays that were counted for them. */
fun topTracks(days: List<ListenDay>, tracks: List<Track>, limit: Int = 5): List<InsightTrack> {
    val byTrack = days.groupBy { it.trackId }
    val byTrackById = tracks.associateBy { it.id }

    return byTrack.mapNotNull { (trackId, rows) ->
        val track = byTrackById[trackId] ?: return@mapNotNull null
        val listened = rows.sumOf { it.listenedMs.coerceAtLeast(0L) }
        val plays = rows.sumOf { it.plays.coerceAtLeast(0) }
        if (listened <= 0L && plays <= 0) {
            null
        } else {
            InsightTrack(track, listened, plays)
        }
    }
        .sortedWith(
            compareByDescending<InsightTrack> { it.listenedMs }
                .thenByDescending { it.plays }
                .thenBy { it.track.title.lowercase() }
        )
        .take(limit)
}

/** The day a song joined the library, falling back to when its file last changed. */
private fun addedOn(track: Track, today: LocalDate, zone: ZoneId): LocalDate {
    val seconds = track.dateAdded?.takeIf { it > 0L }
        ?: track.dateModified.takeIf { it > 0L }
    if (seconds == null) return today

    val date = Instant.ofEpochSecond(seconds).atZone(zone).toLocalDate()
    return if (date.isAfter(today)) today else date
}

/** Sorts amounts largest first and folds everything past [limit] into one Other slice. */
private fun topWithOther(totals: Map<String, Long>, limit: Int): List<InsightSlice> {
    val sorted = totals.entries.sortedByDescending { it.value }
    if (sorted.size <= limit) {
        return sorted.map { InsightSlice(it.key, it.value) }
    }

    val top = sorted.take(limit).map { InsightSlice(it.key, it.value) }
    val other = sorted.drop(limit).sumOf { it.value }
    return top + InsightSlice("Other", other)
}

/** A month as an axis label, in english so the calendar does not change shape by locale. */
private fun monthLabel(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH))
