package com.exodidio.mimi.library

/**
 * Songs that look like the same recording.
 *
 * Title, artist and length are the three things a listener uses to recognise a duplicate,
 * and a length within [duplicateToleranceMs] allows for the one or two seconds that
 * different encoders shave off the same master. Nothing here decides which copy to keep:
 * it only groups them, because a finder that deletes is a finder nobody should trust.
 */
data class DuplicateGroup(
    val title: String,
    val artist: String?,
    val tracks: List<Track>,
)

/** How far apart two copies of the same recording may be and still be called the same. */
const val DUPLICATE_TOLERANCE_MS = 2_000L

/**
 * Groups songs sharing a title and artist whose lengths are within [toleranceMs].
 *
 * The comparison ignores case and surrounding space. Lengths are clustered from the
 * shortest copy rather than chained from neighbour to neighbour, so a run of gradually
 * longer files cannot drag a four second difference into one group.
 *
 * Pure, so the matching can be tested without a device.
 */
fun List<Track>.duplicateGroups(toleranceMs: Long = DUPLICATE_TOLERANCE_MS): List<DuplicateGroup> {
    val byRecording = groupBy { track ->
        track.title.trim().lowercase() + "\u0000" + track.artist.orEmpty().trim().lowercase()
    }

    return byRecording.values
        .flatMap { sameName ->
            if (sameName.size < 2) return@flatMap emptyList()

            val clusters = mutableListOf<MutableList<Track>>()
            sameName.sortedBy { it.durationMs }.forEach { track ->
                val current = clusters.lastOrNull()
                if (current != null && track.durationMs - current.first().durationMs <= toleranceMs) {
                    current += track
                } else {
                    clusters += mutableListOf(track)
                }
            }

            clusters
                .filter { it.size > 1 }
                .map { DuplicateGroup(it.first().title, it.first().artist, it.toList()) }
        }
        .sortedWith(
            compareBy<DuplicateGroup> { it.title.lowercase() }
                .thenBy { it.artist.orEmpty().lowercase() },
        )
}
