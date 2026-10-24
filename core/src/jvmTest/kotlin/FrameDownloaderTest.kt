import github.naixx.core.*
import github.naixx.network.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * The behaviours that were buggy in the field: grinding through every batch on a dead connection, and not
 * resuming from what is already cached.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FrameDownloaderTest {

    private fun FakeApi(fetch: suspend (String) -> ByteArray) = FakeViewApi(onDownload = fetch)

    private val store = MemoryFrameStore()
    private val progress = mutableListOf<List<Int>>()

    @Test
    fun fetchesEveryMissingFrameUnderTheDevicesName() = runTest {
        val api = FakeApi { byteArrayOf(1, 2) }
        val downloader = FrameDownloader(api, store, StandardTestDispatcher(testScheduler))

        val result = downloader.download("tL-1", totalFrames = 5) { progress += it }

        assertEquals(DownloadResult.Complete, result)
        assertEquals((1..5).toList(), store.indices("tl-1"))
        assertEquals("tl-1/cam-1-00003.jpg", api.requested[2])
        assertEquals(emptyList(), progress.first()) // the initial scan
        assertEquals((1..5).toList(), progress.last())
    }

    @Test
    fun skipsFramesTheStoreAlreadyHas() = runTest {
        store.write("tl-1", 1, byteArrayOf(0))
        store.write("tl-1", 2, byteArrayOf(0))
        val api = FakeApi { byteArrayOf(1) }
        val downloader = FrameDownloader(api, store, StandardTestDispatcher(testScheduler))

        downloader.download("tl-1", totalFrames = 5) { progress += it }

        assertEquals(3, api.requested.size)
        assertEquals(listOf(1, 2), progress.first())
        assertEquals((1..5).toList(), store.indices("tl-1"))
    }

    @Test
    fun givesUpAfterThreeDeadBatchesAndKeepsNothingItDidNotGet() = runTest {
        // 35 frames -> batches [1..10][11..20][21..30][31..35]; every request fails, so the 4th batch is never tried.
        val api = FakeApi { throw RuntimeException("connection lost") }
        val downloader = FrameDownloader(api, store, StandardTestDispatcher(testScheduler))

        val result = downloader.download("tl-1", totalFrames = 35) { progress += it }

        assertIs<DownloadResult.Failed>(result)
        assertTrue(result.message.contains("check the connection"))
        assertEquals(30, api.requested.size)
        assertEquals(emptyList(), store.indices("tl-1"))
    }

    @Test
    fun aRecoveringBatchResetsTheFailureCounter() = runTest {
        // The first batch (1-10) fails, the rest succeed: not aborted, and the recovered frames land.
        var call = 0
        val api = FakeApi { if (++call <= 10) throw RuntimeException("transient blip") else byteArrayOf(1) }
        val downloader = FrameDownloader(api, store, StandardTestDispatcher(testScheduler))

        val result = downloader.download("tl-1", totalFrames = 30) { progress += it }

        assertEquals(DownloadResult.Complete, result)
        assertEquals(30, api.requested.size)
        assertEquals((11..30).toList(), store.indices("tl-1"))
    }

    @Test
    fun nothingToDoWhenEverythingIsCached() = runTest {
        (1..3).forEach { store.write("tl-1", it, byteArrayOf(0)) }
        val api = FakeApi { error("must not be called") }
        val downloader = FrameDownloader(api, store, StandardTestDispatcher(testScheduler))

        assertEquals(DownloadResult.Complete, downloader.download("tl-1", totalFrames = 3) { progress += it })
        assertEquals(listOf(listOf(1, 2, 3)), progress)
    }
}
