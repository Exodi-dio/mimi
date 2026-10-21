package com.exodidio.mimi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.exodidio.mimi.library.ListenDay
import com.exodidio.mimi.library.PlayStat
import com.exodidio.mimi.library.Track
import java.time.LocalDate
import java.time.ZoneId

/**
 * What the library holds and what the listener has done with it.
 *
 * Two halves with a period picker each: the library grows on the scale of months, while
 * listening is looked at in weeks and days. Charts are drawn on a canvas rather than pulled
 * from a charting library, because the shapes here are a line, bars and a few rows, and a
 * dependency for that would be more code to keep than the drawing it replaces.
 *
 * Empty is a state the screen states plainly rather than avoids. A library that has never
 * been played still has counts and formats worth reading, and the listening half says that
 * history begins now instead of drawing a flat line as if it were a measurement.
 */
@Composable
fun InsightsScreen(
    tracks: List<Track>,
    albumCount: Int,
    artistCount: Int,
    playlistCount: Int,
    playStats: Map<Long, PlayStat>,
    listenDays: List<ListenDay>,
    separators: List<String>,
    onBack: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    modifier: Modifier = Modifier,
    playingMediaId: String? = null,
) {
    var libraryPeriodName by rememberSaveable { mutableStateOf(InsightPeriod.MONTH.name) }
    var listeningPeriodName by rememberSaveable { mutableStateOf(InsightPeriod.MONTH.name) }
    val libraryPeriod = InsightPeriod.valueOf(libraryPeriodName)
    val listeningPeriod = InsightPeriod.valueOf(listeningPeriodName)

    val today = LocalDate.now()
    val zone = remember { ZoneId.systemDefault() }

    val library = remember(tracks, albumCount, artistCount, playlistCount, libraryPeriod, today, zone) {
        insightLibrary(
            tracks = tracks,
            albumCount = albumCount,
            artistCount = artistCount,
            playlistCount = playlistCount,
            period = libraryPeriod,
            today = today,
            zone = zone,
        )
    }

    val listening = remember(
        listeningPeriod,
        today,
        listenDays,
        tracks,
        playStats,
        separators,
    ) {
        insightListening(
            period = listeningPeriod,
            today = today,
            days = listenDays,
            tracks = tracks,
            playStats = playStats,
            separators = separators,
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Insights",
            subtitle = "What the library holds and what you have listened to.",
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSectionHeading("The library")
            PillRow(
                options = InsightPeriod.entries,
                selected = libraryPeriod,
                label = { it.label },
                onSelect = { libraryPeriodName = it.name },
            )

            SettingsInfoRow(title = "Songs", value = library.songs.toString())
            SettingsInfoRow(title = "Albums", value = library.albums.toString())
            SettingsInfoRow(title = "Artists", value = library.artists.toString())
            SettingsInfoRow(title = "Playlists", value = library.playlists.toString())
            SettingsInfoRow(
                title = "On disk",
                value = formatFileSize(library.sizeBytes),
                subtitle = "The files themselves, not the database.",
            )

            SettingsSectionHeading("Growth")
            if (library.growth.isEmpty()) {
                SettingsNote("No songs yet, so there is no growth to draw.")
            } else {
                GrowthChart(bars = library.growth)
                ChartAxis(bars = library.growth)
                SettingsNote(
                    if (libraryPeriod == InsightPeriod.ALL) {
                        "Songs in the library at the end of each month, cumulatively."
                    } else {
                        "Songs in the library at the end of each day, cumulatively."
                    }
                )
            }

            SettingsSectionHeading("Formats")
            if (library.formats.isEmpty()) {
                SettingsNote("No formats to break down yet.")
            } else {
                library.formats.forEach { slice ->
                    HorizontalBar(
                        label = slice.label,
                        value = countLabel(slice.value.toInt(), "song"),
                        fraction = slice.value.toFloat() / library.songs.coerceAtLeast(1),
                    )
                }
            }

            SettingsSectionHeading("Listening")
            PillRow(
                options = InsightPeriod.entries,
                selected = listeningPeriod,
                label = { it.label },
                onSelect = { listeningPeriodName = it.name },
            )

            Text(
                text = when {
                    listening.listenedMs > 0L -> formatListDuration(listening.listenedMs)
                    listening.hasHistory -> "Nothing in this period"
                    else -> "Nothing yet"
                },
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            )
            Text(
                text = "Time listened",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp),
            )

            SettingsInfoRow(title = "Plays", value = listening.plays.toString())
            SettingsInfoRow(
                title = "Skip rate",
                value = listening.skipRatePercent?.let { "$it%" } ?: "No attempts yet",
                subtitle = "Of everything played or skipped, all time. Skips were never dated.",
            )
            SettingsInfoRow(
                title = "Average session",
                value = if (listening.averageSessionMs > 0L) {
                    formatListDuration(listening.averageSessionMs)
                } else {
                    "No plays yet"
                },
                subtitle = "Time heard in this period divided by its counted plays.",
            )
            SettingsInfoRow(
                title = "Current streak",
                value = if (listening.streakDays == 1) "1 day" else "${listening.streakDays} days",
                subtitle = "Days in a row with some listening, counting back from today.",
            )

            listening.changePercent?.let { change ->
                SettingsInfoRow(
                    title = "Change",
                    value = formatSignedPercent(change),
                    subtitle = "Against the previous ${listeningPeriod.label.lowercase()}.",
                )
            }

            if (!listening.hasHistory) {
                SettingsNote(
                    "No listening history yet. Time is recorded from now on; the play and " +
                        "skip counts kept by older versions carry no dates, so they cannot " +
                        "be placed on a chart."
                )
            } else {
                SettingsSectionHeading(
                    if (listeningPeriod == InsightPeriod.ALL) "Listening by month" else "Listening by day"
                )
                ListeningBarChart(bars = listening.bars)
                ChartAxis(bars = listening.bars)

                if (listening.genres.isEmpty()) {
                    SettingsNote("Nothing in this period has a genre to break down.")
                } else {
                    SettingsSectionHeading("Genres")
                    val most = listening.genres.maxOf { it.value }.coerceAtLeast(1L)
                    listening.genres.forEach { slice ->
                        HorizontalBar(
                            label = slice.label,
                            value = formatListDuration(slice.value),
                            fraction = slice.value.toFloat() / most.toFloat(),
                        )
                    }
                }

                if (listening.artists.isEmpty()) {
                    SettingsNote("No artist was heard in this period.")
                } else {
                    SettingsSectionHeading("Top artists")
                    listening.artists.forEach { artist ->
                        SettingsInfoRow(
                            title = artist.artist,
                            value = formatListDuration(artist.listenedMs),
                        )
                    }
                }

                if (listening.topTracks.isEmpty()) {
                    SettingsNote("No song was heard in this period.")
                } else {
                    SettingsSectionHeading("Top songs")
                    listening.topTracks.forEach { entry ->
                        TopTrackRow(
                            entry = entry,
                            playing = entry.track.contentUri == playingMediaId,
                            onClick = { onPlayTrack(entry.track) },
                        )
                    }
                }
            }

            SettingsPageFooter()
        }
    }
}

/**
 * A cumulative count over time, drawn as a filled area under a line.
 *
 * The fill carries the shape and the line carries the exact edge, which is what makes a
 * small library and a large one readable without an axis in the way. A single point is
 * drawn as a dot rather than as a path, because a one point line is invisible.
 */
@Composable
private fun GrowthChart(bars: List<InsightBar>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val fill = line.copy(alpha = 0.16f)
    val baseline = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp),
    ) {
        val max = bars.maxOf { it.value }.coerceAtLeast(1L).toFloat()
        val step = if (bars.size <= 1) 0f else size.width / (bars.lastIndex)
        val yOf = { value: Long -> size.height - (value / max) * size.height }

        drawLine(
            color = baseline,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )

        if (bars.size == 1) {
            drawCircle(color = line, radius = 4.dp.toPx(), center = Offset(0f, yOf(bars.first().value)))
            return@Canvas
        }

        val path = Path()
        val area = Path()
        bars.forEachIndexed { index, bar ->
            val x = index * step
            val y = yOf(bar.value)
            if (index == 0) {
                path.moveTo(x, y)
                area.moveTo(x, size.height)
                area.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                area.lineTo(x, y)
            }
        }
        area.lineTo(size.width, size.height)
        area.close()

        drawPath(path = area, color = fill)
        drawPath(path = path, color = line, style = Stroke(width = 2.dp.toPx()))
    }
}

/**
 * Listening as bars, one per day or month.
 *
 * Bars are separated by a gap that grows with the bar width, so a short period reads as a
 * row of days rather than a solid block, and a long one still shows that the axis is made of
 * separate buckets.
 */
@Composable
private fun ListeningBarChart(bars: List<InsightBar>, modifier: Modifier = Modifier) {
    val colour = MaterialTheme.colorScheme.primary
    val baseline = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp),
    ) {
        drawLine(
            color = baseline,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )
        if (bars.isEmpty()) return@Canvas

        val max = bars.maxOf { it.value }.coerceAtLeast(1L).toFloat()
        val slot = size.width / bars.size
        val width = (slot * 0.62f).coerceAtLeast(2.dp.toPx())
        bars.forEachIndexed { index, bar ->
            if (bar.value <= 0L) return@forEachIndexed
            val height = (bar.value / max) * size.height
            drawRect(
                color = colour,
                topLeft = Offset(index * slot + (slot - width) / 2f, size.height - height),
                size = Size(width, height),
            )
        }
    }
}

/**
 * The first and last label under a chart.
 *
 * A label for every bucket would turn a thirty day axis into a paragraph. The ends are
 * enough to read the span, and the screen already names the period above the chart.
 */
@Composable
private fun ChartAxis(bars: List<InsightBar>, modifier: Modifier = Modifier) {
    if (bars.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = bars.first().label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (bars.size > 1) {
            Text(
                text = bars.last().label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One named amount as a horizontal bar.
 *
 * The bar is measured against the largest amount in its own group rather than against the
 * total, because a breakdown is read as a ranking and a set of shares would make every bar
 * short. [fraction] is expected to be between zero and one.
 */
@Composable
private fun HorizontalBar(
    label: String,
    value: String,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val colour = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val clamped = fraction.coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp),
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(10.dp),
        ) {
            drawRect(color = track, size = size)
            drawRect(color = colour, size = Size(size.width * clamped, size.height))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .width(88.dp)
                .padding(start = 8.dp),
        )
    }
}

/**
 * One song in the top list, which plays it.
 *
 * The time is the point of the row and the play count is the supporting fact, so the time
 * sits where a value sits on every other row of the screen.
 */
@Composable
private fun TopTrackRow(
    entry: InsightTrack,
    playing: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (playing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val byline = listOfNotNull(
                    entry.track.artist,
                    countLabel(entry.plays, "play"),
                ).joinToString(" \u00b7 ")
                if (byline.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = byline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = formatListDuration(entry.listenedMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
