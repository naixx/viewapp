package com.github.naixx.viewapp.ui.components

import github.naixx.network.AutoSettings
import github.naixx.network.CameraDetails
import github.naixx.network.CameraSettings
import github.naixx.network.Shutter
import github.naixx.network.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameCycleTest {

    private fun status(running: Boolean = true, captureStart: Double? = 1000.0, shutterEv: Double? = -6.0) = Status(
        running = running,
        frames = 10,
        intervalMs = 10_000.0,
        message = "running",
        autoSettings = AutoSettings(paddingTimeMs = 2000.0),
        captureStartTime = captureStart,
        cameraSettings = CameraSettings(details = CameraDetails(shutter = shutterEv?.let { Shutter("1s", ev = it) })),
    )

    @Test
    fun shutterSecondsLikeTheVIEW() {
        assertEquals(1.0, shutterSeconds(-6.0), 1e-9)
        assertEquals(32.0, shutterSeconds(-11.0), 1e-9)
        assertEquals(1.0 / 64, shutterSeconds(0.0), 1e-9)
        assertEquals(0.1, shutterSeconds(5.0), 1e-9)
    }

    @Test
    fun splitsTheIntervalIntoExposureProcessingAndElapsed() {
        assertEquals(FrameCycle(0.1f, 0.2f, 0.25f), frameCycle(status(), 1_002_500))
    }

    @Test
    fun pinsTheMarkerAtTheEndWhenTheNextFrameIsLate() {
        assertEquals(1f, frameCycle(status(), 1_030_000)!!.elapsed)
    }

    @Test
    fun noExposurePartWithoutAShutterEv() {
        assertEquals(0f, frameCycle(status(shutterEv = null), 1_001_000)!!.exposure)
    }

    @Test
    fun nothingToShowWhenIdleOrBeforeTheFirstCapture() {
        assertNull(frameCycle(status(running = false), 1_001_000))
        assertNull(frameCycle(status(captureStart = null), 1_001_000))
    }
}
