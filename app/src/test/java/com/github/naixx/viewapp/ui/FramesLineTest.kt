package com.github.naixx.viewapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FramesLineTest {

    @Test
    fun clipLengthAt30FpsDoesNotDependOnTheInterval() {
        // Was "57 frames (114.0s @30fps)" with a 60 s interval: capture time / 30.
        assertEquals("57 frames (1.9s @30fps)", framesLine(57))
    }

    @Test
    fun roundsToTenthsOfASecond() {
        assertEquals("219 frames (7.3s @30fps)", framesLine(219))
        assertEquals("0 frames (0.0s @30fps)", framesLine(0))
    }
}
