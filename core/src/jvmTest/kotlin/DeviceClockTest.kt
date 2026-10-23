import github.naixx.core.DeviceClock
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceClockTest {

    @Test
    fun assumesTheClocksAgreeUntilACaptureStarts() {
        val clock = DeviceClock()
        clock.onStatus(1000.0, receivedAtMs = 1_005_000)
        assertEquals(1_006_000, clock.nowMs(1_006_000))
    }

    @Test
    fun aNewCaptureStartPinsTheOffset() {
        val clock = DeviceClock()
        clock.onStatus(1000.0, receivedAtMs = 997_000)
        clock.onStatus(1010.0, receivedAtMs = 1_007_000) // the device is 3 s ahead
        assertEquals(1_011_000, clock.nowMs(1_008_000))
    }

    @Test
    fun theEndOfFrameStatusKeepsTheOffset() {
        val clock = DeviceClock()
        clock.onStatus(1000.0, receivedAtMs = 997_000)
        clock.onStatus(1010.0, receivedAtMs = 1_007_000)
        clock.onStatus(1010.0, receivedAtMs = 1_009_500) // same capture, sent when the frame is saved
        assertEquals(1_011_000, clock.nowMs(1_008_000))
    }

    @Test
    fun theFirstStatusAfterReconnectingDoesNotRepin() {
        val clock = DeviceClock()
        clock.onStatus(1000.0, receivedAtMs = 997_000)
        clock.onStatus(1010.0, receivedAtMs = 1_007_000)
        clock.onDisconnected()
        clock.onStatus(1100.0, receivedAtMs = 1_105_000) // a capture that started before the reconnect
        assertEquals(1_011_000, clock.nowMs(1_008_000))
    }
}
