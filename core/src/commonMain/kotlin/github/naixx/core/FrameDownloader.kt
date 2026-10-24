package github.naixx.core

import com.github.naixx.logger.LL
import github.naixx.network.ViewApi
import kotlinx.coroutines.*

/**
 * How many consecutive fully-failed batches are tolerated before a download is abandoned. On a flaky field
 * connection every request blocks up to the HTTP timeout, so without this the download would grind through
 * every remaining batch with no progress and no feedback.
 */
internal const val MAX_CONSECUTIVE_FAILED_BATCHES = 3

sealed interface DownloadResult {
    data object Complete : DownloadResult
    data class Failed(val message: String) : DownloadResult
}

/**
 * Fetches a clip's frames from the device into a [FrameStore], [batchSize] at a time on [io], skipping what the
 * store already has. [onProgress] gets the full ascending list of available frames after the initial scan and
 * after every batch, so a player can start as frames land. Frames that arrived stay in the store whatever the
 * outcome, so a later run resumes instead of starting over. Cancellation propagates.
 */
class FrameDownloader(
    private val api: ViewApi,
    private val store: FrameStore,
    private val io: CoroutineDispatcher,
) {

    suspend fun download(
        clip: String,
        totalFrames: Int,
        batchSize: Int = 10,
        onProgress: suspend (List<Int>) -> Unit,
    ): DownloadResult = coroutineScope {
        val have = store.indices(clip).filter { it in 1..totalFrames }.toMutableSet()
        onProgress(have.sorted())

        val missing = (1..totalFrames).filter { it !in have }
        if (missing.isEmpty()) return@coroutineScope DownloadResult.Complete

        var consecutiveFailedBatches = 0
        try {
            for (batch in missing.chunked(batchSize)) {
                ensureActive()
                val landed = batch.map { i -> async(io) { fetch(clip, i) } }.awaitAll().filterNotNull()
                have += landed
                onProgress(have.sorted())

                if (landed.isEmpty()) {
                    // A whole batch failed — bail out instead of grinding through every remaining batch (each
                    // request blocking on the HTTP timeout) with no progress or feedback.
                    if (++consecutiveFailedBatches >= MAX_CONSECUTIVE_FAILED_BATCHES)
                        return@coroutineScope DownloadResult.Failed("Download failed — check the connection to the VIEW and try again")
                } else {
                    consecutiveFailedBatches = 0
                }
            }
            DownloadResult.Complete
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LL.e("Download error: ${e.message}")
            DownloadResult.Failed("Download failed: ${e.message}")
        }
    }

    /** One frame; null when it failed, so a batch can report what did land. */
    private suspend fun fetch(clip: String, index: Int): Int? = try {
        store.write(clip, index, api.download(frameFileName(clip, index)))
        index
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LL.e("Failed to download frame $index: ${e.message}")
        null
    }
}
