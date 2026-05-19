package com.example.tfgfrontend.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleValidatorTest {

    @Test
    fun `valid hour accepts hh colon mm format`() {
        assertTrue(ScheduleValidator.isValidHour("00:00"))
        assertTrue(ScheduleValidator.isValidHour("09:30"))
        assertTrue(ScheduleValidator.isValidHour("23:59"))
    }

    @Test
    fun `invalid hour rejects malformed values`() {
        assertFalse(ScheduleValidator.isValidHour("9:00"))
        assertFalse(ScheduleValidator.isValidHour("24:00"))
        assertFalse(ScheduleValidator.isValidHour("12:60"))
        assertFalse(ScheduleValidator.isValidHour("ab:cd"))
        assertFalse(ScheduleValidator.isValidHour(""))
    }

    @Test
    fun `open must be before close`() {
        assertTrue(ScheduleValidator.isOpenBeforeClose("09:00", "18:00"))
        assertFalse(ScheduleValidator.isOpenBeforeClose("18:00", "09:00"))
        assertFalse(ScheduleValidator.isOpenBeforeClose("10:00", "10:00"))
    }
}
