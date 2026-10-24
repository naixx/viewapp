package github.naixx.core

/**
 * Where a clip's frames (JPEGs) live on this platform: files under the app's storage on Android, memory in the
 * browser. [clip] is the time-lapse name (`tl-329`); implementations treat it case-insensitively, as the device
 * does. Frame numbers are the device's, starting at 1.
 */
interface FrameStore {

    /** The frame numbers present for [clip], ascending. */
    suspend fun indices(clip: String): List<Int>

    suspend fun read(clip: String, index: Int): ByteArray?

    suspend fun write(clip: String, index: Int, jpeg: ByteArray)

    /** Removes every frame of [clip]. */
    suspend fun delete(clip: String)

    /** Bytes held for [clip]. */
    suspend fun size(clip: String): Long
}

/** A frame that just became available in the [FrameStore] (a live thumbnail written during a time-lapse). */
data class LiveFrame(val clip: String, val index: Int)

/** The device's path of frame [index] of [clip] for `GET /download`: `<clip>/cam-1-<NNNNN>.jpg`, lower-case. */
fun frameFileName(clip: String, index: Int): String = "$clip/cam-1-${index.toString().padStart(5, '0')}.jpg".lowercase()
