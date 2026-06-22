// New test file: network/src/test/java/github/naixx/network/MessagesTest.kt

import github.naixx.network.*
import github.naixx.network.internal.UnknownMessageSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessagesTest {
    private val json = Json { ignoreUnknownKeys = true }

    // Mirrors the polymorphic Json configured in di.kt's networkModule.
    private val polyJson = Json {
        serializersModule = SerializersModule {
            polymorphic(BaseMessage::class) {
                defaultDeserializer { UnknownMessageSerializer() }
            }
        }
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Test
    fun testIntervalometerErrorDeserialization() {
        // The shape the firmware sends on capture failure: app.send('intervalometerError', {msg: msg})
        val raw =
            """{"type":"intervalometerError","msg":"An error occurred during capture.\nThe time-lapse will attempt to continue anyway.\nSystem message: timeout"}"""

        val result = polyJson.decodeFromString<BaseMessage>(raw)

        assertTrue(result is IntervalometerError)
        assertEquals("intervalometerError", result.type)
        assertEquals(
            "An error occurred during capture.\nThe time-lapse will attempt to continue anyway.\nSystem message: timeout",
            result.msg
        )
    }

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



    @Test
    fun testTimelapseProgramKeepsRawAndTyped() {
        val result = polyJson.decodeFromString<BaseMessage>(programJson)

        assertTrue(result is TimelapseProgram)
        // typed view still works
        assertEquals("auto", result.program.rampMode)
        assertEquals(300, result.program.frames)
        // raw is the inner program object, including fields ViewApp does NOT model
        assertEquals("auto", result.raw["rampMode"]?.jsonPrimitive?.content)
        assertTrue(result.raw.containsKey("keyframes"), "raw must preserve unmodeled 'keyframes'")
        assertTrue(result.raw.containsKey("savedExposurePlans"))
        assertTrue(result.raw.containsKey("coords"))
    }

    @Test
    fun testProgramDraftEditIsLossless() {
        val tp = polyJson.decodeFromString<BaseMessage>(programJson) as TimelapseProgram
        val original = tp.raw
        val edited = ProgramDraft(original).set("interval", "12").set("frames", 500).json

        // edited keys changed
        assertEquals("12", edited["interval"]?.jsonPrimitive?.content)
        assertEquals(500, edited["frames"]?.jsonPrimitive?.int)
        // every untouched key is preserved byte-for-byte (incl. unmodeled keyframes/coords)
        for ((key, value) in original) {
            if (key == "interval" || key == "frames") continue
            assertEquals(value, edited[key], "key '$key' must be preserved unchanged")
        }
        assertEquals(original["keyframes"], edited["keyframes"])
    }

    @Test
    fun testProgramDraftReadsMixedNumberStringEncodings() {
        // fixture has nightLuminance as a quoted string "-1.3" and interval as "5"
        val draft = ProgramDraft((polyJson.decodeFromString<BaseMessage>(programJson) as TimelapseProgram).raw)
        assertEquals(-1.3, draft.nightLuminance)
        assertEquals("5", draft.interval)
        assertEquals(true, draft.highlightProtection)
    }

    @Test
    fun testProgramOutMessageSerialization() {
        val program = (polyJson.decodeFromString<BaseMessage>(programJson) as TimelapseProgram).raw

        val run = polyJson.encodeToString<OutMessage>(Run(program, date = 123L))
        assertTrue(run.contains("\"type\":\"run\""), run)
        assertTrue(run.contains("\"date\":123"))
        assertTrue(run.contains("\"program\""))

        assertTrue(polyJson.encodeToString<OutMessage>(Stop()).contains("\"type\":\"stop\""))
        assertTrue(polyJson.encodeToString<OutMessage>(SaveProgram(program)).contains("\"type\":\"save-program\""))
        assertTrue(polyJson.encodeToString<OutMessage>(ReconfigureProgram(program)).contains("\"type\":\"reconfigureProgram\""))

        val dc = polyJson.encodeToString<OutMessage>(DynamicChange("interval", JsonPrimitive(12), 30))
        assertTrue(dc.contains("\"type\":\"dynamicChange\""), dc)
        assertTrue(dc.contains("\"parameter\":\"interval\""))
        assertTrue(dc.contains("\"frames\":30"))
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
