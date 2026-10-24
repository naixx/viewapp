package com.github.naixx.viewapp.ui

import android.content.Context
import androidx.lifecycle.*
import com.github.naixx.compose.*
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.encoding.*
import com.github.naixx.viewapp.utils.EventBus
import com.github.naixx.viewapp.utils.lumaHistogram
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import github.naixx.core.ClipRepository
import github.naixx.core.DeviceState
import github.naixx.core.DownloadResult
import github.naixx.core.FileSystemFrameStore
import github.naixx.core.FrameDownloader
import github.naixx.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * One clip's frames on this device: what is cached, the download, playback position, the histogram of a frame and
 * the MP4 export. Frames live in the [FileSystemFrameStore]; a running time-lapse's frames arrive through
 * [DeviceState.frameWritten]. The `context` parameters are kept for the screens until they move to the shared UI.
 * Created per clip through [Factory].
 */
@AssistedInject
class TimelapseViewModel(
    @Assisted private val clip: Clip,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    // The concrete store: the player and the exporter still want java.io.Files (see getFrameFile) until step 10.
    private val frames: FileSystemFrameStore,
    private val device: DeviceState,
    private val videoExportRepository: VideoExportRepository,
    private val clips: ClipRepository,
    private val api: ViewApi,
) : ViewModel() {

    /** Per clip, from a composable: `assistedMetroViewModel<TimelapseViewModel, Factory>(key = clip.name) { create(clip) }`. */
    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {

        fun create(clip: Clip): TimelapseViewModel
    }

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

    /** Frame histograms by frame number; ~1 KB each, computed once so playback doesn't redo them. */
    private val histograms = ConcurrentHashMap<Int, List<Int>>()

    val clipInfo: StateFlow<TimelapseClipInfo?> = clips
        .clipInfo(clip.name)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Frames of a running time-lapse are cached by DeviceState as they arrive; show each at once.
        device.frameWritten
            .filter { it.clip.equals(clip.name, ignoreCase = true) }
            .onEach { onLiveFrame(it.index) }
            .launchIn(viewModelScope)
    }

    /**
     * Checks for existing frames without starting download
     * Returns Pair(downloadedCount, totalFrames)
     */
    suspend fun checkExistingFrames(context: Context, totalFrames: Int): Pair<Int, Int> {
        val downloadedIndices = frames.indices(clip.name).filter { it in 1..totalFrames }

        // Update the state flows with the found frames
        if (downloadedIndices.isNotEmpty()) {
            downloadedFrames.value = downloadedIndices

            // Set current frame to first frame if not already set
            if (currentFrameIndex.value <= 0) {
                setCurrentFrame(1)
            }
        }

        return Pair(downloadedIndices.size, totalFrames)
    }

    /** A live frame landed in the store: list it and advance to it, unless the user has scrubbed back. */
    private fun onLiveFrame(frameIndex: Int) {
        val prevLatest = downloadedFrames.value.maxOrNull() ?: 0
        val followLatest = currentFrameIndex.value >= prevLatest
        downloadedFrames.value = (downloadedFrames.value + frameIndex).distinct().sorted()
        if (followLatest) currentFrameIndex.value = frameIndex
    }

    /**
     * Get information about the current clip
     */
    suspend fun clipInfo(): UiState<TimelapseClipInfo?> = catching {
        val info = api.clipInfo(clip.name)

        // Remember the fetched info
        viewModelScope.launch {
            clips.saveClipInfo(info)
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
        if (connectionState !is ConnectionState.Connected) {
            EventBus.error("Not connected to a view server")
            return
        }
        LL.d("downloading")
        isDownloading.value = true
        downloadJob = viewModelScope.launch {
            val thisJob = coroutineContext[Job]
            try {
                val info = api.clipInfo(clip.name).also {
                    clips.saveClipInfo(it)
                }
                val totalFrames = info.frames ?: return@launch

                val result = FrameDownloader(api, frames, ioDispatcher).download(clip.name, totalFrames) {
                    downloadedFrames.value = it
                }
                if (result is DownloadResult.Failed) EventBus.error(result.message)
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
     * [onLiveFrame]'s "follow latest" check keeps advancing the player as new thumbnails arrive.
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
     * Get the file for a specific frame if it exists (the exporter wants java.io.Files)
     */
    fun getFrameFile(context: Context, frameIndex: Int): File? =
        File(frames.path(clip.name, frameIndex).toString()).takeIf { it.exists() }

    /**
     * Luma histogram of a cached frame, computed from its JPEG (the VIEW's `histogram` message carries no
     * frame number, exists only for auto-ramped frames, and never for frames downloaded afterwards).
     */
    suspend fun histogram(context: Context, frameIndex: Int): List<Int>? =
        histograms[frameIndex] ?: withContext(ioDispatcher) {
            frames.read(clip.name, frameIndex)?.let(::lumaHistogram)?.also { histograms[frameIndex] = it }
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

        viewModelScope.launch {
            frames.delete(clip.name)
            histograms.clear()
            downloadedFrames.value = emptyList()
        }
    }

    /**
     * Gets the size of the cache for this clip
     */
    suspend fun getCacheSize(context: Context): Long = frames.size(clip.name)

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
