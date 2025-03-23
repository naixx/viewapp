import github.naixx.network.SettingsMessage
import github.naixx.network.settingJson
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SettingsMessageTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSettingsDeserialization() {
        val result = json.decodeFromString<SettingsMessage>(settingJson)

        assertEquals("settings", result.type)

        // Test camera settings values
        val settings = result.settings
        assertEquals("30s", settings.shutter)
        assertEquals("UNKNOWN", settings.aperture)
        assertEquals("100", settings.iso)
        assertEquals(72.0, settings.battery)
        assertEquals(0.0, settings.focusPos)

        // Test details
        val details = settings.details

        // Test shutter details
        val shutter = details.shutter
        assertEquals("30s", shutter.name)
        assertEquals(-11.0, shutter.ev)
        assertEquals(19660810, shutter.code)
        assertEquals(32000, shutter.duration_ms)
        assertEquals("30s", shutter.cameraName)
        assertNotNull(shutter.list)
        assertEquals(55, shutter.list.size)

        // Check first shutter entry
        val firstShutter = shutter.list[0]
        assertEquals("30s", firstShutter.name)
        assertEquals(-11.0, firstShutter.ev)
        assertEquals(19660810, firstShutter.code)
        assertEquals(32000, firstShutter.duration_ms)
        assertEquals("30s", firstShutter.cameraName)

        // Test aperture details
        val aperture = details.aperture
        assertEquals("UNKNOWN", aperture?.name)
        assertEquals(null, aperture?.ev)
        assertEquals(0, aperture?.code)
        assertNotNull(aperture?.list)
        assertEquals(37, aperture?.list?.size)

        // Check a sample aperture entry
        val sampleAperture = aperture?.list?.first()
        assertEquals("1.0", sampleAperture?.name)
        assertEquals(-8.0, sampleAperture?.ev)
        assertEquals(100, sampleAperture?.code)
        assertEquals("1.0", sampleAperture?.cameraName)

        // Test ISO details
        val iso = details.iso
        assertEquals("100", iso.name)
        assertEquals(0.0, iso.ev)
        assertEquals(100, iso.code)
        assertEquals("100", iso.cameraName)
        assertNotNull(iso.list)

        // Lists should be populated
//        assertNotNull(settings.lists)
//        assertEquals(55, settings.lists.shutter.size)
//        assertEquals(37, settings.lists.aperture.size)
//        assertEquals(89, settings.lists.iso.size)
    }
}
