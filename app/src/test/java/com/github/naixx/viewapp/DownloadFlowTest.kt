package com.github.naixx.viewapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.naixx.viewapp.encoding.VideoExportRepository
import com.github.naixx.viewapp.ui.TimelapseViewModel
import com.github.naixx.viewapp.ui.components.sampleClipInfo
import com.github.naixx.viewapp.utils.EventBus
import github.naixx.core.DeviceSession
import github.naixx.core.DeviceState
import github.naixx.core.FileSystemFrameStore
import github.naixx.core.FrameStore
import github.naixx.db.TimelapseClipInfoDao
import github.naixx.network.*
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.io.files.Path
import org.koin.dsl.bind
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the clip frame-download flow in [TimelapseViewModel]. These cover the behaviours that
 * were buggy in the field: the download not surfacing its in-flight state, silently no-op'ing while
 * a previous download was alive, and grinding through every batch on a dead connection.
 *
 * The IO dispatcher is injected so all the per-frame `async` work runs on the same test scheduler as
 * the ViewModel's Main dispatcher, making [advanceUntilIdle] fully deterministic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class DownloadFlowTest : KoinTest {

    private val dispatcher = StandardTestDispatcher()

    private val viewApi = mockk<ViewApi>(relaxed = true)
    private val dao = mockk<TimelapseClipInfoDao>(relaxed = true)
    private val exportRepo = mockk<VideoExportRepository>(relaxed = true)

    private lateinit var context: Context

    /** No device in these tests: the session never connects and sends nothing. */
    private val idleSession = object : DeviceSession {
        override val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val messages = MutableSharedFlow<BaseMessage>()
        override fun send(message: OutMessage) = Unit
        override val controllable = false
        override fun connect() = Unit
        override fun disconnect() = Unit
    }

    private val testModule = module {
        factory<ViewApi> { (_: String) -> viewApi }
        single<TimelapseClipInfoDao> { dao }
        single<VideoExportRepository> { exportRepo }
        single { FileSystemFrameStore(Path(context.filesDir.path), dispatcher) } bind FrameStore::class
        single { DeviceState(idleSession, mockk(relaxed = true), get(), CoroutineScope(dispatcher)) }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        every { dao.getTimelapseClipInfo(any()) } returns flowOf(null)
        coEvery { dao.insertTimelapseClipInfo(any()) } just Runs
        clearTimelapseCache()
        stopKoin()
        startKoin { modules(testModule) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
        clearTimelapseCache()
        clearAllMocks()
    }

    private fun clearTimelapseCache() {
        File(context.filesDir, "timelapses").deleteRecursively()
    }

    private fun vm(frames: Int, name: String = "tl-1") =
        TimelapseViewModel(clip(frames, name), dispatcher)

    private fun clip(frames: Int, name: String = "tl-1") =
        Clip(index = 0, id = 1, frames = frames, name = name, imageBase64 = null)

    private fun connected() = ConnectionState.Connected(
        address = AddressResponse.Address(address = "ws://host:8101", fromUrl = "http://host/"),
        isLocal = true
    )

    private fun cacheDir(name: String = "tl-1") = File(context.filesDir, "timelapses/$name")

    /** Subscribe to EventBus before any emission so toasts raised during the test are captured. */
    private fun TestScope.recordEvents(): List<String> {
        val messages = mutableListOf<String>()
        backgroundScope.launch { EventBus.events.collect { messages += it.first } }
        runCurrent()
        return messages
    }

    @Test
    fun `download while disconnected gives immediate feedback and never starts`() = runTest(dispatcher) {
        val events = recordEvents()

        val model = vm(frames = 5)
        model.downloadFrames(context, ConnectionState.Disconnected)
        advanceUntilIdle()

        assertFalse(model.isDownloading.value, "should not enter downloading state")
        assertTrue(model.downloadedFrames.value.isEmpty())
        coVerify(exactly = 0) { viewApi.clipInfo(any()) }
        assertTrue(events.any { it.contains("Not connected") }, "expected a not-connected toast, got $events")
    }

    @Test
    fun `download fetches all missing frames and ends not-downloading`() = runTest(dispatcher) {
        coEvery { viewApi.clipInfo(any()) } returns sampleClipInfo.copy(name = "tl-1", frames = 5)
        coEvery { viewApi.download(any()) } returns byteArrayOf(0x1, 0x2)

        val model = vm(frames = 5)
        model.downloadFrames(context, connected())
        advanceUntilIdle()

        assertEquals((1..5).toList(), model.downloadedFrames.value)
        assertFalse(model.isDownloading.value)
        coVerify(exactly = 5) { viewApi.download(any()) }
        assertEquals(5, cacheDir().listFiles()?.size)
    }

    @Test
    fun `download skips frames already cached on disk`() = runTest(dispatcher) {
        coEvery { viewApi.clipInfo(any()) } returns sampleClipInfo.copy(name = "tl-1", frames = 5)
        coEvery { viewApi.download(any()) } returns byteArrayOf(0x1)
        cacheDir().apply { mkdirs() }.also { dir ->
            File(dir, "frame_1.jpg").writeBytes(byteArrayOf(0))
            File(dir, "frame_2.jpg").writeBytes(byteArrayOf(0))
        }

        val model = vm(frames = 5)
        model.downloadFrames(context, connected())
        advanceUntilIdle()

        // Only the three missing frames are fetched; the cached ones are reused.
        coVerify(exactly = 3) { viewApi.download(any()) }
        assertEquals((1..5).toList(), model.downloadedFrames.value)
    }

    @Test
    fun `download aborts and reports an error after consecutive failed batches`() = runTest(dispatcher) {
        // 35 frames -> 4 batches of [1..10][11..20][21..30][31..35]. Every request fails, so the loop
        // must bail out after MAX_CONSECUTIVE_FAILED_BATCHES (3) rather than attempting the 4th batch.
        coEvery { viewApi.clipInfo(any()) } returns sampleClipInfo.copy(name = "tl-1", frames = 35)
        coEvery { viewApi.download(any()) } throws RuntimeException("connection lost")
        val events = recordEvents()

        val model = vm(frames = 35)
        model.downloadFrames(context, connected())
        advanceUntilIdle()

        assertFalse(model.isDownloading.value)
        assertTrue(model.downloadedFrames.value.isEmpty())
        coVerify(exactly = 30) { viewApi.download(any()) } // batches 1-3 only; 4th never attempted
        assertTrue(events.any { it.contains("Download failed") }, "expected a failure toast, got $events")
    }

    @Test
    fun `a recovering batch resets the failure counter and the download completes`() = runTest(dispatcher) {
        // First batch (frames 1-10) fails, the rest succeed. The counter must reset so the download is
        // NOT aborted, and the recovered frames land.
        coEvery { viewApi.clipInfo(any()) } returns sampleClipInfo.copy(name = "tl-1", frames = 30)
        var call = 0
        coEvery { viewApi.download(any()) } answers {
            call++
            if (call <= 10) throw RuntimeException("transient blip") else byteArrayOf(0x1)
        }

        val model = vm(frames = 30)
        model.downloadFrames(context, connected())
        advanceUntilIdle()

        assertFalse(model.isDownloading.value)
        assertEquals((11..30).toList(), model.downloadedFrames.value)
        coVerify(exactly = 30) { viewApi.download(any()) } // all batches attempted, none skipped
    }

    @Test
    fun `a second download is ignored while one is already in progress`() = runTest(dispatcher) {
        coEvery { viewApi.clipInfo(any()) } returns sampleClipInfo.copy(name = "tl-1", frames = 3)
        coEvery { viewApi.download(any()) } returns byteArrayOf(0x1)

        val model = vm(frames = 3)
        model.downloadFrames(context, connected())
        // isDownloading is set synchronously before the coroutine is launched.
        assertTrue(model.isDownloading.value)
        model.downloadFrames(context, connected()) // must be a no-op
        advanceUntilIdle()

        coVerify(exactly = 1) { viewApi.clipInfo(any()) }
        assertFalse(model.isDownloading.value)
    }

    @Test
    fun `stopDownload ends downloading but keeps already-downloaded frames playable`() = runTest(dispatcher) {
        coEvery { viewApi.clipInfo(any()) } returns sampleClipInfo.copy(name = "tl-1", frames = 3)
        coEvery { viewApi.download(any()) } returns byteArrayOf(0x1)

        val model = vm(frames = 3)
        model.downloadFrames(context, connected())
        advanceUntilIdle()
        assertEquals(3, model.downloadedFrames.value.size)

        model.stopDownload()

        assertFalse(model.isDownloading.value)
        // Frames are retained so the player can still show/play them (no blank screen).
        assertEquals((1..3).toList(), model.downloadedFrames.value)
    }
}
