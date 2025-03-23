// New test file: network/src/test/java/github/naixx/network/MessagesTest.kt

import github.naixx.network.ConnectedMessage
import github.naixx.network.connecJson
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessagesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testConnectedDeserialization() {
        val result = json.decodeFromString<ConnectedMessage>(connecJson)

        assertEquals(true, result.connected)
        assertEquals("Sony A6600", result.model)
        assertEquals("3sxtjgfdr0", result.ack)
        assertEquals("camera", result.type)

        val features = result.supports!!
        assertEquals(true, features.shutter)
        assertEquals(true, features.aperture)
        assertEquals(true, features.iso)
        assertEquals(true, features.liveview)
        assertEquals(true, features.destination)
        assertEquals(true, features.focus)
        assertEquals(3000, features._bufTime)
        assertEquals(false, features.newISO)
    }

    @Test
    fun testCameraFeaturesDefaults() {
        val emptyJson = """{"connected":true,"model":"Test","type":"camera"}"""
        val result = json.decodeFromString<ConnectedMessage>(emptyJson)

        assertNull(result.supports)

        val partialJson = """
            {"connected":true,"model":"Test","type":"camera",
            "supports":{"shutter":true}}
        """.trimIndent()
        val partialResult = json.decodeFromString<ConnectedMessage>(partialJson)

        assertEquals(true, partialResult.supports?.shutter)
        assertEquals(false, partialResult.supports?.aperture)
        assertEquals(0, partialResult.supports?._bufTime)
    }



//    @Test
//    fun testSerialization() {
//        val original = Connected(
//            connected = true,
//            model = "Canon R5",
//            supports = CameraFeatures(
//                shutter = true,
//                iso = true
//            ),
//            type = "camera"
//        )
//
////        val serialized = Json.encodeToString(Connected.serializer(), original)
////        val deserialized = json.decodeFromString<Connected>(serialized)
////
////        assertEquals(original.connected, deserialized.connected)
////        assertEquals(original.model, deserialized.model)
////        assertEquals(original.supports?.shutter, deserialized.supports?.shutter)
////        assertEquals(original.supports?.iso, deserialized.supports?.iso)
//    }
}
