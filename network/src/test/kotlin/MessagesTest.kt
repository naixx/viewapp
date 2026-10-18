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
    fun testIntervalometerStatusDeserialization() {
        // Regression: Status.currentPlanIndex was declared `@Contextual Any?`, which has no
        // registered serializer, so every real-device `intervalometerStatus` frame failed to
        // deserialize with "Serializer for class 'Any' is not found" (WebSocketClient receive loop).
        // The fixture is a real frame captured from the device — currentPlanIndex is a number while
        // an exposure plan is active.
        val raw = MessagesTest::class.java.getResource("/intervalometerStatus.json")!!.readText()

        val result = polyJson.decodeFromString<BaseMessage>(raw)

        assertTrue(result is IntervalometerStatus)
        assertEquals("intervalometerStatus", result.type)
        assertEquals(0, result.status.currentPlanIndex)
        assertTrue(result.status.running)
        assertEquals(1782769255.377, result.status.captureStartTime)
    }

    @Test
    fun testIdleIntervalometerStatusDeserialization() {
        // Regression: when idle the device sends a minimal status — `exposure` is {status:{},config:{}}
        // and tlName/cameraSettings/hdr*/pan*/tilt*/tracking* are all omitted. The model must tolerate it.
        val raw = MessagesTest::class.java.getResource("/intervalometerStatus_idle.json")!!.readText()
        val result = polyJson.decodeFromString<BaseMessage>(raw)
        assertTrue(result is IntervalometerStatus)
        val s = (result as IntervalometerStatus).status
        assertEquals(false, s.running)
        assertNull(s.tlName)
        assertNull(s.cameraSettings)
    }

    @Test
    fun testFullSettingsDeserialization() {
        // Regression: a real camera's full settings include an "UNKNOWN (<code>)" entry with `ev: null`
        // in the aperture/shutter lists. Those entries decode with the strict Json.Default
        // (DynamicListSerializer), so the *Entry.ev fields must be nullable or the whole frame is dropped.
        val raw = MessagesTest::class.java.getResource("/settings_full.json")!!.readText()
        val result = polyJson.decodeFromString<BaseMessage>(raw)
        assertTrue(result is SettingsMessage)
        val s = (result as SettingsMessage).settings!!
        assertEquals("1/1600", s.shutter)
        assertEquals(55, s.details?.shutter?.list?.size)
        assertEquals(89, s.details?.iso?.list?.size)
        assertEquals(37, s.details?.aperture?.list?.size)
    }

    @Test
    fun testIdleSettingsDeserialization() {
        // Regression: idle `settings` send shutter/aperture/iso = null and details.{shutter,iso} = null.
        val raw = MessagesTest::class.java.getResource("/settings_idle.json")!!.readText()
        val result = polyJson.decodeFromString<BaseMessage>(raw)
        assertTrue(result is SettingsMessage)
        assertNull((result as SettingsMessage).settings!!.shutter)
        assertNull(result.settings!!.iso)
    }

    @Test
    fun testNoCameraSettingsDeserialization() {
        // Regression: with no camera connected the device replies to Get("settings") with
        // `{"settings":null}` — the app requests settings on every (re)connect.
        val result = polyJson.decodeFromString<BaseMessage>("""{"settings":null,"type":"settings"}""")
        assertTrue(result is SettingsMessage)
        assertNull((result as SettingsMessage).settings)
    }

    @Test
    fun testFractionalStatusNumbers() {
        // Regression: exposure.status.highlightProtection steps by 0.333 while highlights clip
        // (exposure.js), and trackingPan/Tilt accumulate fractional degrees during sun/moon tracking.
        // Integer fields dropped every such intervalometerStatus frame (seen on device 2026-08/09).
        val raw = MessagesTest::class.java.getResource("/intervalometerStatus.json")!!.readText()
            .replace("\"highlightProtection\":0,", "\"highlightProtection\":0.666,")
            .replace("\"trackingPan\":0,", "\"trackingPan\":12.34,")
            .replace("\"trackingTilt\":0,", "\"trackingTilt\":-3.5,")
        val result = polyJson.decodeFromString<BaseMessage>(raw)
        assertTrue(result is IntervalometerStatus)
        val s = (result as IntervalometerStatus).status
        assertEquals(0.666, s.exposure.status.highlightProtection)
        assertEquals(12.34, s.trackingPan)
        assertEquals(-3.5, s.trackingTilt)
    }

    @Test
    fun testTimelapseProgramTypedDecode() {
        // Regression: a real device program (numeric `interval`, eclipse plans/keyframes, and which may
        // omit tracking/delay/axes/focusPos) must still decode into the typed Program.
        val raw = MessagesTest::class.java.getResource("/timelapseProgram.json")!!.readText()
        val result = polyJson.decodeFromString<BaseMessage>(raw)
        assertTrue(result is TimelapseProgram)
        assertEquals("auto", (result as TimelapseProgram).program.rampMode)
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

    @Test
    fun testCameraControlOutMessageSerialization() {
        val set = polyJson.encodeToString<OutMessage>(SetSetting("iso", "100"))
        assertTrue(set.contains("\"type\":\"set\""), set)
        assertTrue(set.contains("\"key\":\"iso\""), set)
        assertTrue(set.contains("\"val\":\"100\""), set) // wire key is `val`, not `value`

        assertTrue(polyJson.encodeToString<OutMessage>(Capture()).contains("\"type\":\"capture\""))
        assertTrue(polyJson.encodeToString<OutMessage>(CaptureTest()).contains("\"type\":\"capture-test\""))
        assertTrue(polyJson.encodeToString<OutMessage>(SetEvUp()).contains("\"type\":\"setEvUp\""))
        assertTrue(polyJson.encodeToString<OutMessage>(SetEvDown()).contains("\"type\":\"setEvDown\""))

        val setEv = polyJson.encodeToString<OutMessage>(SetEv(1.5))
        assertTrue(setEv.contains("\"type\":\"setEv\""), setEv)
        assertTrue(setEv.contains("\"ev\":1.5"), setEv)

        // The firmware gates focus on `key === 'manual'` and reads `repeat`; both must survive encoding.
        val focus = polyJson.encodeToString<OutMessage>(ManualFocus("manual", value = -1, repeat = 2))
        assertTrue(focus.contains("\"type\":\"focus\""), focus)
        assertTrue(focus.contains("\"key\":\"manual\""), focus)
        assertTrue(focus.contains("\"val\":-1"), focus)
        assertTrue(focus.contains("\"repeat\":2"), focus)

        assertTrue(polyJson.encodeToString<OutMessage>(Preview()).contains("\"type\":\"preview\""))
        assertTrue(polyJson.encodeToString<OutMessage>(PreviewStop()).contains("\"type\":\"previewStop\""))
    }

    @Test
    fun testPreviewThumbnailDeserialization() {
        // Regression: live-view ("preview") and test-shot ("test") frames omit frameIndex/tlName
        // (those exist only for running-timelapse "thumbnail" frames), so the model must default them.
        val raw = """{"type":"thumbnail","imageType":"test","jpeg":"abc123"}"""
        val result = polyJson.decodeFromString<BaseMessage>(raw)
        assertTrue(result is Thumbnail)
        assertEquals("test", (result as Thumbnail).imageType)
        assertEquals("abc123", result.imageBase64)
    }

    @Test
    fun testCaptureErrorDeserialization() {
        // `msg` may be a plain string …
        val str = polyJson.decodeFromString<BaseMessage>("""{"type":"captureError","msg":"Camera busy"}""")
        assertTrue(str is CaptureError)
        assertEquals("Camera busy", (str as CaptureError).message)

        // … or an object the device emits on hardware errors, e.g. {"errno":2}.
        val obj = polyJson.decodeFromString<BaseMessage>("""{"type":"captureError","msg":{"errno":2}}""")
        assertTrue(obj is CaptureError)
        assertTrue((obj as CaptureError).message.contains("errno"), obj.message)
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
