package github.naixx.core

import kotlin.time.Clock

/**
 * The VIEW's clock as seen from the client. The frame progress is timed from the device's `captureStartTime`,
 * and the two clocks can disagree by seconds in the field (no NTP on the hotspot, GPS time updates are off
 * during a run, the RTC drifts). The device sends a status the moment each capture starts, so a changed
 * `captureStartTime` arriving pins the offset; until one does, the clocks are assumed to agree.
 */
class DeviceClock {
    private var offsetMs = 0L
    private var lastCaptureStart: Double? = null

    // The first status after (re)connecting may describe a capture that started a while ago.
    private var connected = false

    fun onStatus(captureStartTime: Double?, receivedAtMs: Long) {
        if (connected && captureStartTime != null && captureStartTime != lastCaptureStart) {
            offsetMs = (captureStartTime * 1000).toLong() - receivedAtMs
        }
        lastCaptureStart = captureStartTime
        connected = true
    }

    fun onDisconnected() {
        connected = false
    }

    fun nowMs(clientNowMs: Long = Clock.System.now().toEpochMilliseconds()) = clientNowMs + offsetMs
}
