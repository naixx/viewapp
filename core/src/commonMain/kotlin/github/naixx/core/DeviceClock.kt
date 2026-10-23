package github.naixx.core

import kotlin.time.Clock

/**
 * The VIEW's clock as seen from the client. The frame progress is timed from the device's `captureStartTime`,
 * and the two clocks can disagree by seconds in the field (no NTP on the hotspot, GPS time updates are off
 * during a run, the RTC drifts). The device sends a status the moment each capture starts, so a changed
 * `captureStartTime` arriving pins the offset; until one does, the clocks are assumed to agree.
 *
 * @param clock the client's clock; tests pass a fake.
 */
class DeviceClock(private val clock: Clock = Clock.System) {
    private var offsetMs = 0L
    private var lastCaptureStart: Double? = null

    // The first status after (re)connecting may describe a capture that started a while ago.
    private var connected = false

    /** Called when an `intervalometerStatus` arrives, with the device's `captureStartTime` from it. */
    fun onStatus(captureStartTime: Double?) {
        if (connected && captureStartTime != null && captureStartTime != lastCaptureStart) {
            offsetMs = (captureStartTime * 1000).toLong() - clock.now().toEpochMilliseconds()
        }
        lastCaptureStart = captureStartTime
        connected = true
    }

    fun onDisconnected() {
        connected = false
    }

    /** The device's current time in epoch milliseconds, as far as the client can tell. */
    fun nowMs() = clock.now().toEpochMilliseconds() + offsetMs
}
