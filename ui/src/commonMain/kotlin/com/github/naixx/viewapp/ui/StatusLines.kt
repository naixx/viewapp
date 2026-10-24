package com.github.naixx.viewapp.ui

import github.naixx.network.ConnectedMessage

// The one-line texts of the Status screen (and CameraControls); pure, tested in :app (StatusLinesTest, FramesLineTest).

/**
 * The running time-lapse's camera line. When the camera drops off (powered off, cable) the device sends
 * `camera {connected:false, model:""}`, which used to read "running | unknown connected".
 */
fun cameraStatusLine(statusMessage: String?, camera: ConnectedMessage): String {
    val s = statusMessage ?: "unknown"
    return if (camera.connected) "$s | ${camera.model.ifEmpty { "unknown" }} connected"
    else "$s | camera disconnected"
}

/**
 * The camera's drive mode. The VIEW switches any drive mode it knows to Single before it shoots (sony.js capture),
 * so a continuous or bracketing mode is only until the next shot; one it doesn't know (UNKNOWN) it leaves alone.
 */
fun driveModeLine(driveMode: String?): String? = when (driveMode) {
    null -> null
    "Single", "UNKNOWN" -> "Drive mode: $driveMode"
    else -> "Drive mode: $driveMode (Single from the next shot)"
}

/**
 * Frames so far and the clip length they make at 30 fps (it used to multiply in the interval: capture time / 30).
 * One decimal, rounded half up, always with a point: integer tenths instead of a locale-dependent `%.1f`.
 */
fun framesLine(frames: Int): String {
    val tenths = (frames * 10 + 15) / 30
    return "$frames frames (${tenths / 10}.${tenths % 10}s @30fps)"
}
