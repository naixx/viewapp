import github.naixx.core.FileSystemFrameStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random
import kotlin.test.*

class FileSystemFrameStoreTest {

    private val root = Path(SystemTemporaryDirectory, "viewapp-frames-${Random.nextLong()}")
    private val store = FileSystemFrameStore(root, Dispatchers.IO)

    @AfterTest
    fun cleanUp() = runTest {
        store.delete("tl-1")
        if (SystemFileSystem.exists(Path(root, "timelapses"))) SystemFileSystem.delete(Path(root, "timelapses"))
        if (SystemFileSystem.exists(root)) SystemFileSystem.delete(root)
    }

    @Test
    fun keepsTheAndroidLayoutAndTreatsTheClipNameCaseInsensitively() = runTest {
        store.write("tL-1", 3, byteArrayOf(1, 2, 3))

        assertTrue(SystemFileSystem.exists(Path(root, "timelapses", "tl-1", "frame_3.jpg")))
        assertEquals(Path(root, "timelapses", "tl-1", "frame_3.jpg"), store.path("TL-1", 3))
        assertContentEquals(byteArrayOf(1, 2, 3), store.read("TL-1", 3))
    }

    @Test
    fun listsSizesAndDeletes() = runTest {
        assertEquals(emptyList(), store.indices("tl-1"))
        assertEquals(0L, store.size("tl-1"))
        assertNull(store.read("tl-1", 1))

        store.write("tl-1", 10, ByteArray(100))
        store.write("tl-1", 2, ByteArray(50))
        store.write("tl-1", 2, ByteArray(60)) // overwrite

        assertEquals(listOf(2, 10), store.indices("tl-1"))
        assertEquals(160L, store.size("tl-1"))

        store.delete("tl-1")
        assertEquals(emptyList(), store.indices("tl-1"))
        assertFalse(SystemFileSystem.exists(Path(root, "timelapses", "tl-1")))
        store.delete("tl-1") // deleting what isn't there is fine
    }
}
