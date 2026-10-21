package com.exodidio.mimi.library

import org.junit.Assert.assertEquals
import org.junit.Test

class CreditSeparatorEditingTest {

    private val separators = Settings.DEFAULT_CREDIT_SEPARATORS

    @Test
    fun `a single comma can be added although it was never typeable as a list`() {
        assertEquals(separators + ",", addCreditSeparator(separators, ","))
    }

    @Test
    fun `a blank value adds nothing`() {
        assertEquals(separators, addCreditSeparator(separators, ""))
        assertEquals(separators, addCreditSeparator(separators, "   "))
    }

    @Test
    fun `a duplicate in another case adds nothing`() {
        assertEquals(separators, addCreditSeparator(separators, "FEAT."))
        assertEquals(separators, addCreditSeparator(separators, "Featuring"))
    }

    @Test
    fun `a multi-word separator keeps the space inside it`() {
        assertEquals(
            separators + "with special guest",
            addCreditSeparator(separators, "  with special guest  "),
        )
    }
}
