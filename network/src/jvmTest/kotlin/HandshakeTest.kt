import github.naixx.network.*
import kotlin.test.*

class HandshakeTest {

    @Test
    fun `the handshake authenticates and asks for the device state`() {
        assertEquals(listOf(Session("sess-1"), Get("camera"), Get("settings"), Get("battery"), Get("program")), handshake("sess-1"))
        assertEquals(Session(""), handshake(null).first())
    }
}
