package github.naixx.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.write

/**
 * The [FrameStore] for platforms with a file system (Android, JVM, native): `<root>/timelapses/<clip>/frame_<N>.jpg`,
 * the layout the Android app has always used, so caches from before the shared modules stay valid. Not for the
 * browser: kotlinx-io's [SystemFileSystem] exists there only under Node.js.
 *
 * @param io where the blocking file calls run (Dispatchers.IO on the JVM).
 */
class FileSystemFrameStore(private val root: Path, private val io: CoroutineDispatcher) : FrameStore {

    private val fs = SystemFileSystem

    private fun dir(clip: String) = Path(root, "timelapses", clip.lowercase())

    /** The frame's path; for code that still needs a file (the Android player and exporter, for now). */
    fun path(clip: String, index: Int): Path = Path(dir(clip), "frame_$index.jpg")

    override suspend fun indices(clip: String): List<Int> = withContext(io) {
        val dir = dir(clip)
        if (!fs.exists(dir)) emptyList()
        else fs.list(dir).mapNotNull { FRAME_FILE.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }.sorted()
    }

    override suspend fun read(clip: String, index: Int): ByteArray? = withContext(io) {
        val path = path(clip, index)
        if (!fs.exists(path)) null else fs.source(path).buffered().use { it.readByteArray() }
    }

    override suspend fun write(clip: String, index: Int, jpeg: ByteArray) = withContext(io) {
        fs.createDirectories(dir(clip))
        fs.sink(path(clip, index)).buffered().use { it.write(jpeg) }
    }

    override suspend fun delete(clip: String) = withContext(io) {
        val dir = dir(clip)
        if (fs.exists(dir)) {
            fs.list(dir).forEach { fs.delete(it) } // frames only; the clip folder holds nothing else
            fs.delete(dir)
        }
    }

    override suspend fun size(clip: String): Long = withContext(io) {
        val dir = dir(clip)
        if (!fs.exists(dir)) 0L else fs.list(dir).sumOf { fs.metadataOrNull(it)?.size ?: 0L }
    }

    private companion object {
        val FRAME_FILE = Regex("""frame_(\d+)\.jpg""")
    }
}
