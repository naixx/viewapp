package com.github.naixx.viewapp.ui

import github.naixx.network.ConnectedMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraStatusLineTest {

    private fun camera(connected: Boolean, model: String) = ConnectedMessage(connected, model, type = "camera")

    @Test
    fun connectedCameraShowsModel() {
        assertEquals("running | Sony A6600 connected", cameraStatusLine("running", camera(true, "Sony A6600")))
    }

    @Test
    fun disconnectedCameraSaysSo() {
        // What the device sends when the camera powers off mid-run: {"connected":false,"model":"","supports":{}}
        assertEquals("running | camera disconnected", cameraStatusLine("running", camera(false, "")))
    }

    @Test
    fun missingStatusAndModelFallBackToUnknown() {
        assertEquals("unknown | unknown connected", cameraStatusLine(null, camera(true, "")))
    }
}
