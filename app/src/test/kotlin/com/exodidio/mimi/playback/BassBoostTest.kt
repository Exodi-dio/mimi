package com.exodidio.mimi.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BassBoostTest {

    @Test
    fun `no boost means no trim at all`() {
        assertEquals(1f, bassBoostTrimScale(0), 0.001f)
        assertEquals(0f, bassBoostTrimDb(0), 0.001f)
    }

    @Test
    fun `the trim grows as the boost rises`() {
        val scales = (0..100 step 10).map { bassBoostTrimScale(it) }

        assertEquals(scales.sortedDescending(), scales)
        assertTrue(bassBoostTrimScale(100) < bassBoostTrimScale(50))
    }

    @Test
    fun `the trim never makes the music louder than it was`() {
        (0..100).forEach { strength ->
            assertTrue(
                "A trim of $strength took the level above one.",
                bassBoostTrimScale(strength) <= 1f,
            )
            assertTrue(bassBoostTrimScale(strength) > 0f)
        }
    }

    @Test
    fun `the trim is taken off before the boost, not after`() {
        assertTrue(bassBoostTrimDb(50) < 0f)
        assertEquals(BASS_BOOST_MAX_TRIM_DB, -bassBoostTrimDb(100), 0.001f)
    }

    @Test
    fun `the subsonic filter takes nothing above its corner`() {
        assertEquals(0f, subsonicCutDb(SUBSONIC_CORNER_HZ), 0.001f)
        assertEquals(0f, subsonicCutDb(500f), 0.001f)
    }

    @Test
    fun `the subsonic filter takes the rumble away`() {
        assertTrue(subsonicCutDb(20f) < 0f)
        assertTrue(subsonicCutDb(20f) < subsonicCutDb(35f))
    }
}
