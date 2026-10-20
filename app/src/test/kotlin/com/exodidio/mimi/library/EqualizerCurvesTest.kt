package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class EqualizerCurvesTest {

    private val ramp = listOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f)

    @Test
    fun `every band reads back its own gain exactly`() {
        EqualizerFrequenciesHz.forEachIndexed { index, frequency ->
            assertEquals(
                ramp[index],
                gainAtFrequency(ramp, frequency.toFloat()),
                0.001f,
            )
        }
    }

    @Test
    fun `between two bands the gain is read on a log scale`() {
        // 32 and 64 are one octave apart, so their geometric mean is the midpoint in
        // hearing terms: sqrt(32 * 64) = 45.25 Hz.
        val halfway = sqrt(32f * 64f)

        assertEquals(0.5f, gainAtFrequency(ramp, halfway), 0.001f)
    }

    @Test
    fun `linear midpoint would be wrong, which is the point of the log scale`() {
        val logMid = gainAtFrequency(ramp, sqrt(32f * 64f))
        val linearMid = gainAtFrequency(ramp, (32f + 64f) / 2f)

        // 48 Hz is past the log midpoint, so it already reads above halfway.
        assertTrue(linearMid > logMid)
    }

    @Test
    fun `below the lowest band the first gain is held`() {
        assertEquals(0f, gainAtFrequency(ramp, 10f), 0.001f)
        assertEquals(0f, gainAtFrequency(ramp, 0f), 0.001f)
    }

    @Test
    fun `above the highest band the last gain is held`() {
        assertEquals(9f, gainAtFrequency(ramp, 20_000f), 0.001f)
    }

    @Test
    fun `a curve with nothing in it gains nothing`() {
        assertEquals(0f, gainAtFrequency(emptyList(), 1_000f), 0.001f)
    }

    @Test
    fun `one point is the whole curve`() {
        assertEquals(4f, gainAtFrequency(listOf(4f), 100f), 0.001f)
    }

    @Test
    fun `gains are clamped to the ceiling`() {
        assertEquals(12f, normalizeGain(40f), 0.001f)
        assertEquals(-12f, normalizeGain(-40f), 0.001f)
    }

    @Test
    fun `gains snap to half a decibel`() {
        assertEquals(3.5f, normalizeGain(3.4f), 0.001f)
        assertEquals(3.5f, normalizeGain(3.6f), 0.001f)
        assertEquals(4f, normalizeGain(3.9f), 0.001f)
    }

    @Test
    fun `zero stays plain zero and never becomes negative zero`() {
        val zero = normalizeGain(-0.1f)

        assertEquals(0f, zero, 0.001f)
        // -0.0f prints as "-0.0", which is the reason this exists.
        assertTrue(zero.toString() == "0.0")
    }

    @Test
    fun `a curve survives being written down and read back`() {
        val curve = listOf(-1.5f, 0f, 3f, 12f, -12f, 0.5f, 1f, 2f, 3f, 4f)

        assertEquals(curve, decodeCurve(encodeCurve(curve)))
    }

    @Test
    fun `text that is not a curve is refused`() {
        assertNull(decodeCurve(null))
        assertNull(decodeCurve(""))
        assertNull(decodeCurve("1,2,3"))
        assertNull(decodeCurve("a,b,c,d,e,f,g,h,i,j"))
    }

    @Test
    fun `the listener's profiles survive being written down`() {
        val mine = listOf(
            EqualizerProfile("user_one", "Bedroom", listOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f), isBuiltIn = false),
            EqualizerProfile("user_two", "Train", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), isBuiltIn = false),
        )

        val read = decodeProfiles(encodeProfiles(mine))

        assertEquals(mine.map { it.key }, read.map { it.key })
        assertEquals(mine.map { it.name }, read.map { it.name })
        assertEquals(mine.first().gainsDb, read.first().gainsDb)
        assertTrue(read.none { it.isBuiltIn })
    }

    @Test
    fun `a broken profile is dropped rather than taking the others with it`() {
        val text = "user_one|Bedroom|1,1,1,1,1,1,1,1,1,1;rubbish;user_two|Train|0,0,0,0,0,0,0,0,0,0"

        val read = decodeProfiles(text)

        assertEquals(listOf("user_one", "user_two"), read.map { it.key })
    }

    @Test
    fun `nothing stored means no profiles`() {
        assertTrue(decodeProfiles(null).isEmpty())
        assertTrue(decodeProfiles("").isEmpty())
    }

    @Test
    fun `a name is stripped of the characters the storage uses`() {
        assertEquals("Night bus", cleanProfileName("Night|bus"))
        assertEquals("Quiet;car", cleanProfileName("Quiet;car"))
        assertEquals("Bedroom", cleanProfileName("  Bedroom  "))
    }

    @Test
    fun `a name of only spaces is refused`() {
        assertNull(cleanProfileName("   "))
        assertNull(cleanProfileName(""))
    }

    @Test
    fun `every built-in curve has ten gains`() {
        EqualizerPresets.forEach { preset ->
            assertEquals(preset.key, EqualizerFrequenciesHz.size, preset.gainsDb.size)
        }
    }

    @Test
    fun `the built-in curves all stay inside the ceiling`() {
        EqualizerPresets.forEach { preset ->
            preset.gainsDb.forEach { gain ->
                assertTrue("${preset.key} wants $gain dB", gain in -EQUALIZER_LIMIT_DB..EQUALIZER_LIMIT_DB)
            }
        }
    }

    @Test
    fun `flat is the first curve and gains nothing anywhere`() {
        val flat = EqualizerPresets.first { it.key == EQUALIZER_FLAT_KEY }

        assertTrue(flat.gainsDb.all { it == 0f })
    }

    @Test
    fun `no two built-in curves share a key`() {
        assertEquals(EqualizerPresets.size, EqualizerPresets.map { it.key }.toSet().size)
    }

    @Test
    fun `a frequency is labelled in the unit people read it in`() {
        assertEquals("32 Hz", frequencyLabel(32))
        assertEquals("500 Hz", frequencyLabel(500))
        assertEquals("1 kHz", frequencyLabel(1_000))
        assertEquals("16 kHz", frequencyLabel(16_000))
    }
}
