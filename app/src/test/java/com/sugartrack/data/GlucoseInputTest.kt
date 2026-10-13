package com.sugartrack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlucoseInputTest {
    @Test
    fun normalizeDigits_acceptsWesternArabicIndicAndPersianDigits() {
        assertEquals("125", GlucoseInput.normalizeDigits("125"))
        assertEquals("125", GlucoseInput.normalizeDigits("١٢٥"))
        assertEquals("125", GlucoseInput.normalizeDigits("۱۲۵"))
    }

    @Test
    fun maximumValidation_blocksValuesAbove600_butAllowsTypingIntermediates() {
        assertTrue(GlucoseInput.isWithinMaximum(""))
        assertTrue(GlucoseInput.isWithinMaximum("6"))
        assertTrue(GlucoseInput.isWithinMaximum("600"))
        assertTrue(GlucoseInput.isWithinMaximum("600."))
        assertFalse(GlucoseInput.isWithinMaximum("601"))
        assertFalse(GlucoseInput.isWithinMaximum("٦٠١"))
        assertFalse(GlucoseInput.isWithinMaximum("600.1"))
    }
}
