package com.github.naixx.viewapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriveModeLineTest {

    @Test
    fun aContinuousModeIsSwitchedAtTheNextShot() {
        // the A6600 was in Continuous Shooting Lo on 2026-10-18
        assertEquals("Drive mode: Continuous Shooting Lo (Single from the next shot)", driveModeLine("Continuous Shooting Lo"))
    }

    @Test
    fun singleAndUnknownStayAsTheyAre() {
        assertEquals("Drive mode: Single", driveModeLine("Single"))
        assertEquals("Drive mode: UNKNOWN", driveModeLine("UNKNOWN"))
    }

    @Test
    fun olderFirmwareSendsNone() {
        assertNull(driveModeLine(null))
    }
}
