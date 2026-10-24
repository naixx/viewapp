import github.naixx.core.MemoryClipRepository
import github.naixx.network.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MemoryClipRepositoryTest {

    private val repo = MemoryClipRepository()

    private fun clip(id: Int) = Clip(index = id, id = id, frames = 10 * id, name = "tl-$id", imageBase64 = null)

    // Program has no defaults for the device's always-present fields; a plain fixed-interval one.
    private fun program(frames: Int) = Program(
        rampMode = "fixed", lrtDirection = "auto", intervalMode = "fixed", rampAlgorithm = "lum", highlightProtection = false,
        interval = "5", dayInterval = 5.0, nightInterval = 30.0, frames = frames, destination = "camera",
        nightLuminance = -1.5, dayLuminance = 0.0, rampParameters = "S+I", hdrCount = 0, hdrStops = 1,
        trackingTarget = "moon", autoRestart = false,
    )

    @Test
    fun refreshReplacesTheListWithTheDevices() = runTest {
        repo.refreshClips(FakeViewApi(onClips = { listOf(clip(1), clip(2)) }))
        assertEquals(listOf("tl-1", "tl-2"), repo.getAllClips().first().map { it.name })

        repo.refreshClips(FakeViewApi(onClips = { listOf(clip(2), clip(3)) })) // tl-1 is gone from the device
        assertEquals(listOf("tl-2", "tl-3"), repo.getAllClips().first().map { it.name })

        repo.clearClips()
        assertEquals(emptyList(), repo.getAllClips().first())
    }

    @Test
    fun clipInfoIsKeyedByNameCaseInsensitively() = runTest {
        assertNull(repo.clipInfo("tl-5").first())

        val info = TimelapseClipInfo(
            id = 5, name = "tL-5", date = "2026-10-24T00:00:00Z",
            program = program(frames = 50),
            status = Status(running = false, frames = 50, intervalMs = 1000.0, message = "done", autoSettings = AutoSettings(0.0)),
            logfile = "", cameras = 1, primary_camera = 1, frames = 50,
        )
        repo.saveClipInfo(info)

        assertEquals(50, repo.clipInfo("TL-5").first()?.frames)
    }
}
