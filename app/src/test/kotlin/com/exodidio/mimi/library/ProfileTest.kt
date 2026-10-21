package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileTest {

    @Test
    fun `a picture already smaller than the target is decoded whole`() {
        assertEquals(1, sampleSizeFor(longestEdge = 512, targetPx = 512))
        assertEquals(1, sampleSizeFor(longestEdge = 400, targetPx = 512))
    }

    @Test
    fun `a large picture is halved until the next halving would go under the target`() {
        // 4000 -> 2000 -> 1000 -> 500, and 500 is under 512, so the decode is at 1000.
        assertEquals(4, sampleSizeFor(longestEdge = 4000, targetPx = 512))
        // 4096 lands exactly on the target at the third halving, which is allowed: the
        // picture is never decoded smaller than asked for.
        assertEquals(8, sampleSizeFor(longestEdge = 4096, targetPx = 512))
    }

    @Test
    fun `a picture that is exactly twice the target is halved once`() {
        assertEquals(2, sampleSizeFor(longestEdge = 1024, targetPx = 512))
    }

    @Test
    fun `a picture with no measured size is decoded whole rather than divided by zero`() {
        assertEquals(1, sampleSizeFor(longestEdge = 0, targetPx = 512))
        assertEquals(1, sampleSizeFor(longestEdge = -1, targetPx = 512))
        assertEquals(1, sampleSizeFor(longestEdge = 4000, targetPx = 0))
    }

    @Test
    fun `the initial is the first letter of the name, uppercased`() {
        assertEquals("A", profileInitial("ada"))
        assertEquals("L", profileInitial("  love"))
        assertEquals("É", profileInitial("émile"))
    }

    @Test
    fun `a blank name has no initial to draw`() {
        assertNull(profileInitial(""))
        assertNull(profileInitial("   "))
    }
}
