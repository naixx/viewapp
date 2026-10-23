import github.naixx.core.DeviceClock
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceClockTest {

    private val time = FakeClock()
    private val clock = DeviceClock(time)

    private fun statusAt(receivedAtMs: Long, captureStartTime: Double) {
        time.nowMs = receivedAtMs
        clock.onStatus(captureStartTime)
    }

    private fun deviceNowAt(clientMs: Long): Long {
        time.nowMs = clientMs
        return clock.nowMs()
    }

    @Test
    fun assumesTheClocksAgreeUntilACaptureStarts() {
        statusAt(1_005_000, 1000.0)
        assertEquals(1_006_000, deviceNowAt(1_006_000))
    }

    @Test
    fun aNewCaptureStartPinsTheOffset() {
        statusAt(997_000, 1000.0)
        statusAt(1_007_000, 1010.0) // the device is 3 s ahead
        assertEquals(1_011_000, deviceNowAt(1_008_000))
    }

    @Test
    fun theEndOfFrameStatusKeepsTheOffset() {
        statusAt(997_000, 1000.0)
        statusAt(1_007_000, 1010.0)
        statusAt(1_009_500, 1010.0) // same capture, sent when the frame is saved
        assertEquals(1_011_000, deviceNowAt(1_008_000))
    }

    @Test
    fun theFirstStatusAfterReconnectingDoesNotRepin() {
        statusAt(997_000, 1000.0)
        statusAt(1_007_000, 1010.0)
        clock.onDisconnected()
        statusAt(1_105_000, 1100.0) // a capture that started before the reconnect
        assertEquals(1_011_000, deviceNowAt(1_008_000))
    }
}
