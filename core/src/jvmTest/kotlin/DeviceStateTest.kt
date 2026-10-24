import github.naixx.core.*
import github.naixx.network.*
import github.naixx.network.AddressResponse.Address
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.io.encoding.Base64
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceStateTest {

    private class FakeSession : DeviceSession {
        override val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val messages = MutableSharedFlow<BaseMessage>(extraBufferCapacity = 64)
        val sent = mutableListOf<OutMessage>()
        override fun send(message: OutMessage) { sent += message }
        override val controllable = false
        override val subnetLabel = MutableStateFlow<String?>(null)
        override fun connect() = Unit
        override fun disconnect() = Unit
        fun receive(message: BaseMessage) = check(messages.tryEmit(message))
    }

    private val storage = object : StorageProvider {
        override fun session() = "sess-1"
        override fun session(session: String) = Unit
        override fun lastSuccessfulAddress(address: String) = Unit
        override fun email() = null
        override fun password() = null
    }

    private val connected = ConnectionState.Connected(Address("ws://10.0.0.1:8101", "http://10.0.0.1/"), true)
    private val session = FakeSession()
    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private val time = FakeClock(1_000_000)
    private val frames = MemoryFrameStore()
    private val state = DeviceState(session, storage, frames, scope, clock = time)

    @AfterTest
    fun tearDown() = scope.cancel()

    private fun status(captureStartTime: Double? = null, frames: Int = 1) = IntervalometerStatus(
        Status(running = true, frames = frames, intervalMs = 1000.0, message = "running", autoSettings = AutoSettings(0.0), captureStartTime = captureStartTime),
        type = "intervalometerStatus",
    )

    @Test
    fun messagesFanOutByClassAndUnknownOnesAreKept() {
        session.receive(Battery(50f, charging = false, type = "battery"))
        session.receive(ConnectedMessage(connected = true, model = "A6600", type = "camera"))
        session.receive(UnknownMessage("""{"type":"anystringtype"}"""))

        assertEquals(50f, state.battery.value?.percentage)
        assertEquals("A6600", state.camera.value?.model)
        assertNull(state.intervalometerStatus.value)
    }

    @Test
    fun disconnectingResetsEverything() {
        session.state.value = connected
        session.receive(Battery(50f, charging = false, type = "battery"))
        session.receive(status(captureStartTime = 1000.0))

        session.state.value = ConnectionState.Disconnected

        assertNull(state.battery.value)
        assertNull(state.intervalometerStatus.value)
    }

    @Test
    fun everyConnectSendsTheHandshake() {
        val handshake = listOf(Session("sess-1"), Get("camera"), Get("settings"), Get("battery"), Get("program"))

        session.state.value = ConnectionState.Connecting
        assertEquals(emptyList(), session.sent)

        session.state.value = connected
        assertEquals(handshake, session.sent)

        session.state.value = ConnectionState.Connecting
        session.state.value = connected
        assertEquals(handshake + handshake, session.sent)
    }

    @Test
    fun captureErrorsAreToastedAndIntervalometerErrorsAlsoStayAsNotifications() {
        val toasts = mutableListOf<String>()
        state.errors.onEach { toasts += it }.launchIn(scope)

        session.receive(IntervalometerError(msg = "Camera went away", type = "intervalometerError"))
        session.receive(IntervalometerError(msg = null, type = "intervalometerError"))
        session.receive(CaptureError(type = "captureError"))

        assertEquals(listOf("Camera went away", "An error occurred during capture", "Capture failed"), toasts)
        assertEquals(listOf("Camera went away", "An error occurred during capture"), state.notifications.value.map { it.message })
        assertTrue(state.notifications.value.none { it.read })

        state.markNotificationsRead()
        assertTrue(state.notifications.value.all { it.read })
        state.clearNotifications()
        assertEquals(emptyList(), state.notifications.value)
    }

    @Test
    fun theDeviceClockFollowsCaptureStarts() {
        session.state.value = connected
        time.nowMs = 997_000
        session.receive(status(captureStartTime = 1000.0))
        time.nowMs = 1_007_000
        session.receive(status(captureStartTime = 1010.0, frames = 2)) // the device is 3 s ahead

        time.nowMs = 1_008_000
        assertEquals(1_011_000, state.deviceClock.nowMs())
    }

    @Test
    fun sendGoesToTheSession() {
        state.send(Stop())
        assertIs<Stop>(session.sent.single())
    }

    @Test
    fun liveFramesAreCachedOnceAndAnnounced() = runBlocking {
        val written = mutableListOf<LiveFrame>()
        state.frameWritten.onEach { written += it }.launchIn(scope)
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)

        session.receive(Thumbnail(imageType = "thumbnail", type = "thumbnail", frameIndex = 7, tlName = "tL-414", imageBase64 = Base64.Default.encode(jpeg)))
        session.receive(Thumbnail(imageType = "preview", type = "thumbnail", imageBase64 = Base64.Default.encode(jpeg))) // live view: no frame
        session.receive(Thumbnail(imageType = "thumbnail", type = "thumbnail", frameIndex = 8, tlName = "tL-414", imageBase64 = "not base64!"))

        assertContentEquals(jpeg, frames.read("tl-414", 7))
        assertEquals(listOf(7), frames.indices("TL-414"))
        assertEquals(listOf(LiveFrame("tL-414", 7)), written)
        assertEquals(8, state.thumbnail.value?.frameIndex) // the message itself is still the latest thumbnail
    }
}
