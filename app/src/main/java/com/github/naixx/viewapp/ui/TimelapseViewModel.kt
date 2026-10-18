package com.github.naixx.viewapp.ui

import android.content.Context
import android.util.Base64
import androidx.lifecycle.*
import com.github.naixx.compose.*
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.encoding.*
import com.github.naixx.viewapp.utils.EventBus
import com.github.naixx.viewapp.utils.lumaHistogram
import github.naixx.db.TimelapseClipInfoDao
import github.naixx.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.core.component.*
import org.koin.core.parameter.parametersOf
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * How many consecutive fully-failed batches we tolerate before aborting a download. On a flaky
 * field connection every request blocks up to the HTTP timeout, so without this the download would
 * grind through every remaining batch with no progress and no feedback.
 */
private const val MAX_CONSECUTIVE_FAILED_BATCHES = 3

class TimelapseViewModel(
    private val clip: Clip,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), KoinComponent {

    val downloadedFrames: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    val currentFrameIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** Observable download state so the UI always reflects an in-flight download, even before any frame lands. */
    val isDownloading: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val exportProgress: MutableStateFlow<Float> = MutableStateFlow(0f)

    val exportResult = MutableSharedFlow<EncodingResult>(extraBufferCapacity = 1)

    private var downloadJob: Job? = null
    private var exportJob: Job? = null
    private var totalFrames: Int = clip.frames
    private val videoExportRepository: VideoExportRepository by inject()
    private val timelapseClipInfoDao: TimelapseClipInfoDao by inject()

    /** Frame histograms by frame number; ~1 KB each, computed once so playback doesn't redo them. */
    private val histograms = ConcurrentHashMap<Int, List<Int>>()

    val clipInfo: StateFlow<TimelapseClipInfo?> = timelapseClipInfoDao
        .getTimelapseClipInfo(clip.name)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Checks for existing frames without starting download
     * Returns Pair(downloadedCount, totalFrames)
     */
    suspend fun checkExistingFrames(context: Context, totalFrames: Int): Pair<Int, Int> {
        return withContext(ioDispatcher) {
            val cacheDir = File(context.filesDir, "timelapses/${clip.name.lowercase()}")
            val downloadedIndices = (1..totalFrames).filter { i ->
                File(cacheDir, "frame_$i.jpg").exists()
            }

            // Update the state flows with the found frames
            if (downloadedIndices.isNotEmpty()) {
                downloadedFrames.value = downloadedIndices.sorted()

                // Set current frame to first frame if not already set
                if (currentFrameIndex.value <= 0) {
                    setCurrentFrame(1)
                }
            }

            Pair(downloadedIndices.size, totalFrames)
        }
    }

    /**
     * Ingest a frame that arrived live (as a Thumbnail) during a running time-lapse: cache it to disk
     * (if the foreground service hasn't already), add it to [downloadedFrames] and advance to it so it
     * shows in the player immediately. Auto-follows the newest frame unless the user has scrubbed back.
     */
    suspend fun addLiveFrame(context: Context, frameIndex: Int, imageBase64: String) {
        withContext(ioDispatcher) {
            val dir = File(context.filesDir, "timelapses/${clip.name.lowercase()}").apply { mkdirs() }
            val file = File(dir, "frame_$frameIndex.jpg")
            if (!file.exists()) {
                runCatching { file.writeBytes(Base64.decode(imageBase64, Base64.DEFAULT)) }
                    .onFailure { LL.e("Error caching live frame: ${it.message}") }
            }
        }
        val prevLatest = downloadedFrames.value.maxOrNull() ?: 0
        val followLatest = currentFrameIndex.value >= prevLatest
        downloadedFrames.value = (downloadedFrames.value + frameIndex).distinct().sorted()
        if (followLatest) currentFrameIndex.value = frameIndex
    }

    /**
     * Get information about the current clip
     */
    suspend fun clipInfo(state: ConnectionState.Connected): UiState<TimelapseClipInfo?> = catching {
        val viewApi: ViewApi = get<ViewApi> { parametersOf(state.address.fromUrl) }
        val info = viewApi.clipInfo(clip.name)

        // Store the fetched info in the database
        viewModelScope.launch {
            timelapseClipInfoDao.insertTimelapseClipInfo(listOf(info))
        }

        info
    }

    /**
     * Download all frames for the current clip
     */
    fun downloadFrames(context: Context, connectionState: ConnectionState?) {
        LL.d("downloading $connectionState " + isPlaying.value + isDownloading.value)
        if (isPlaying.value || isDownloading.value) return

        // Check the connection up front so a tap while disconnected gives immediate feedback instead
        // of silently launching a coroutine that fails inside.
        val state = connectionState as? ConnectionState.Connected
        if (state == null) {
            EventBus.error("Not connected to a view server")
            return
        }
        LL.d("downloading")
        isDownloading.value = true
        downloadJob = viewModelScope.launch {
            val thisJob = coroutineContext[Job]
            try {
                val cacheDir = File(context.filesDir, "timelapses/${clip.name.lowercase()}").apply { mkdirs() }
                val viewApi: ViewApi = get { parametersOf(state.address.fromUrl) }
                val info = viewApi.clipInfo(clip.name).also {
                    timelapseClipInfoDao.insertTimelapseClipInfo(listOf(it))
                }

                totalFrames = info.frames ?: return@launch

                val downloadedIndices = (1..totalFrames).filter { i ->
                    File(cacheDir, "frame_$i.jpg").exists()
                }.toMutableList()

                downloadedFrames.value = downloadedIndices.sorted()

                val framesToDownload = (1..totalFrames) - downloadedIndices
                if (framesToDownload.isEmpty()) return@launch

                val batchSize = 10
                var consecutiveFailedBatches = 0
                framesToDownload.chunked(batchSize).forEach { batch ->
                    if (!isActive) return@launch

                    val results = batch.map { i ->
                        async(ioDispatcher) {
                            try {
                                val frameFile = File(cacheDir, "frame_$i.jpg")
                                if (!frameFile.exists()) {
                                    val frameBytes = viewApi.download(
                                        "${clip.name}/cam-1-${i.toString().padStart(5, '0')}.jpg"
                                            .lowercase()
                                    )
                                    frameFile.writeBytes(frameBytes)
                                }
                                i
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                LL.e("Failed to download frame $i: ${e.message}")
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()

                    downloadedIndices.addAll(results)
                    downloadedFrames.value = downloadedIndices.sorted()

                    if (results.isEmpty()) {
                        // A whole batch failed — bail out instead of grinding through every remaining
                        // batch (each request blocking on the HTTP timeout) with no progress or feedback.
                        if (++consecutiveFailedBatches >= MAX_CONSECUTIVE_FAILED_BATCHES) {
                            EventBus.error("Download failed — check the connection to the VIEW and try again")
                            return@launch
                        }
                    } else {
                        consecutiveFailedBatches = 0
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LL.e("Download error: ${e.message}")
                EventBus.error("Download failed: ${e.message}")
                // Keep whatever frames already landed so the user can resume rather than start over.
            } finally {
                // Only clear shared state if we're still the active job — a newer download may have
                // replaced us after a cancel/restart, and we must not clobber its state.
                if (downloadJob === thisJob) {
                    downloadJob = null
                    isDownloading.value = false
                }
            }
        }
    }

    /**
     * Stop the current download
     */
    fun stopDownload() {
        downloadJob?.cancel()
        downloadJob = null
        isDownloading.value = false
        // Keep the frames already fetched so the user can still play them (and resume later) instead
        // of being left with a blank screen.
    }

    /**
     * Set the current frame index for playback
     */
    fun setCurrentFrame(index: Int) {
        currentFrameIndex.value = index
    }

    /**
     * Jump to — and resume following — the newest captured frame. Reads the authoritative frame list
     * (not a possibly-stale UI snapshot) and targets the real frame *number*, not the frame count, so
     * [addLiveFrame]'s "follow latest" check keeps advancing the player as new thumbnails arrive.
     */
    fun goLive() {
        togglePlayback(false)
        downloadedFrames.value.maxOrNull()?.let { currentFrameIndex.value = it }
    }

    /**
     * Toggle playback state
     */
    fun togglePlayback(playing: Boolean) {
        isPlaying.value = playing
    }

    /**
     * Get the file for a specific frame if it exists
     */
    fun getFrameFile(context: Context, frameIndex: Int): File? {
        val file = File(context.filesDir, "timelapses/${clip.name.lowercase()}/frame_$frameIndex.jpg")
        return if (file.exists()) file else null
    }

    /**
     * Luma histogram of a cached frame, computed from its JPEG (the VIEW's `histogram` message carries no
     * frame number, exists only for auto-ramped frames, and never for frames downloaded afterwards).
     */
    suspend fun histogram(context: Context, frameIndex: Int): List<Int>? =
        histograms[frameIndex] ?: withContext(ioDispatcher) {
            getFrameFile(context, frameIndex)?.let(::lumaHistogram)?.also { histograms[frameIndex] = it }
        }

    /**
     * Delete all cached frames for this clip
     */
    fun deleteCache(context: Context) {
        // First stop any ongoing download
        stopDownload()

        // Reset playback
        if (isPlaying.value) {
            togglePlayback(false)
        }
        setCurrentFrame(0)

        // Delete all cached frames
        viewModelScope.launch(ioDispatcher) {
            val cacheDir = File(context.filesDir, "timelapses/${clip.name.lowercase()}")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
                cacheDir.delete()
            }
            histograms.clear()
            downloadedFrames.value = emptyList()
        }
    }

    /**
     * Gets the size of the cache for this clip
     */
    suspend fun getCacheSize(context: Context): Long {
        return withContext(ioDispatcher) {
            val cacheDir = File(context.filesDir, "timelapses/${clip.name.lowercase()}")
            var size = 0L
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    size += file.length()
                }
            }
            size
        }
    }

    fun exportVideo(context: Context, fps: Int = 30) {
        if (downloadedFrames.value.isEmpty() || exportJob?.isActive == true) return

        exportJob = viewModelScope.launch {
            try {
                val frameIndices = downloadedFrames.value
                if (frameIndices.isEmpty()) {
                    exportResult.emit(EncodingResult.Error("No frames downloaded"))
                    return@launch
                }

                val frameFiles = frameIndices.mapNotNull { index -> getFrameFile(context, index) }
                if (frameFiles.isEmpty()) {
                    exportResult.emit(EncodingResult.Error("No frame files found"))
                    return@launch
                }

                val outputFileName = "${clip.name}_${System.currentTimeMillis()}.mp4"

                val progressJob = launch {
                    videoExportRepository.progress.collect { progress ->
                        exportProgress.value = progress
                    }
                }

                videoExportRepository.exportVideo(frameFiles, outputFileName, fps).let { result ->
                    progressJob.cancel()
                    exportResult.emit(result)
                }
            } catch (e: Exception) {
                LL.e("Export error: ${e.message}")
                exportResult.emit(EncodingResult.Error("Export failed: ${e.message}", e))
            } finally {
                exportJob = null
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        exportJob = null
        videoExportRepository.cancelExport()
        exportProgress.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        downloadJob?.cancel()
        exportJob?.cancel()
        videoExportRepository.cancelExport()
    }
}
