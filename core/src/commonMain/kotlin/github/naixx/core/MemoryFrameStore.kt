package github.naixx.core

/**
 * Frames kept in memory: the browser's store (nothing survives a reload, which refetches from the device anyway)
 * and the test double. Not thread-safe; the browser is single-threaded and tests run on one dispatcher.
 */
class MemoryFrameStore : FrameStore {

    private val clips = mutableMapOf<String, MutableMap<Int, ByteArray>>()

    private fun frames(clip: String) = clips.getOrPut(clip.lowercase()) { mutableMapOf() }

    override suspend fun indices(clip: String): List<Int> = frames(clip).keys.sorted()

    override suspend fun read(clip: String, index: Int): ByteArray? = frames(clip)[index]

    override suspend fun write(clip: String, index: Int, jpeg: ByteArray) {
        frames(clip)[index] = jpeg
    }

    override suspend fun delete(clip: String) {
        clips.remove(clip.lowercase())
    }

    override suspend fun size(clip: String): Long = frames(clip).values.sumOf { it.size.toLong() }
}
