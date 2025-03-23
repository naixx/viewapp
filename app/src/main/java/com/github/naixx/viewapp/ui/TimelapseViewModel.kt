package com.github.naixx.viewapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.naixx.compose.UiState
import com.github.naixx.compose.catching
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.encoding.EncodingResult
import com.github.naixx.viewapp.encoding.VideoExportRepository
import github.naixx.network.Clip
import github.naixx.network.ConnectionState
import github.naixx.network.TimelapseClipInfo
import github.naixx.network.ViewApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.io.File
import java.util.Collections

class TimelapseViewModel(private val clip: Clip) : ViewModel(), KoinComponent {

    val downloadedFrames: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    val downloadProgress: MutableStateFlow<Float> = MutableStateFlow(0f)
    val currentFrameIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _exportProgress: MutableStateFlow<Float> = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    val exportResult = MutableSharedFlow<EncodingResult>(extraBufferCapacity = 1)

    private var downloadJob: Job? = null
    private var exportJob: Job? = null
    private var totalFrames: Int = clip.frames
    private val videoExportRepository: VideoExportRepository by inject()

    /**
     * Returns whether frames are currently being downloaded
     */
    fun isDownloading(): Boolean {
        return downloadJob != null && downloadJob!!.isActive
    }

    /**
     * Checks for existing frames without starting download
     * Returns Pair(downloadedCount, totalFrames)
     */
    suspend fun checkExistingFrames(context: Context, connectionState: ConnectionState?): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            val cacheDir = File(context.filesDir, "timelapses/${clip.name}")
            val downloadedIndices = mutableListOf<Int>()

            // Check which files are already downloaded
            if (cacheDir.exists()) {
                for (i in 1..totalFrames) {
                    val frameFile = File(cacheDir, "frame_$i.jpg")
                    if (frameFile.exists()) {
                        downloadedIndices.add(i)
                    }
                }
            }

            // Update the state flows with the found frames
            if (downloadedIndices.isNotEmpty()) {
                downloadedFrames.value = downloadedIndices.sorted()

                // Update progress only if we know the total frames
                if (totalFrames > 0) {
                    downloadProgress.value = if (downloadedIndices.size >= totalFrames) 1f
                                           else downloadedIndices.size.toFloat() / totalFrames
                }

                // Set current frame to first frame if not already set
                if (currentFrameIndex.value <= 0) {
                    setCurrentFrame(1)
                }
            }

            Pair(downloadedIndices.size, totalFrames)
        }
    }

    /**
     * Get information about the current clip
     */
    suspend fun clipInfo(connectionState: ConnectionState?): UiState<TimelapseClipInfo?> = catching {
        (connectionState as? ConnectionState.Connected)?.let { state ->
            val viewApi: ViewApi = get<ViewApi> { parametersOf(state.address.fromUrl) }
            viewApi.clipInfo(clip.name)
        }
    }

    /**
     * Download all frames for the current clip
     */
    fun downloadFrames(context: Context, connectionState: ConnectionState?) {
        // Don't start download if we're playing or already downloading
        if (isPlaying.value) return
        if (isDownloading()) return

        downloadJob = viewModelScope.launch {
            try {
                val cacheDir = File(context.filesDir, "timelapses/${clip.name}")
                cacheDir.mkdirs()

                (connectionState as? ConnectionState.Connected)?.let { state ->
                    val viewApi: ViewApi = get<ViewApi> { parametersOf(state.address.fromUrl) }
                    val info = viewApi.clipInfo(clip.name)
                    totalFrames = info.frames ?: return@let

                    downloadProgress.value = 0f

                    val downloadedIndices = Collections.synchronizedList(mutableListOf<Int>())
                    val mutex = Mutex() // For synchronizing progress updates

                    // First, check which files are already downloaded
                    for (i in 1..totalFrames) {
                        val frameFile = File(cacheDir, "frame_$i.jpg")
                        if (frameFile.exists()) {
                            downloadedIndices.add(i)
                        }
                    }

                    // Update initial progress based on already downloaded files
                    if (downloadedIndices.isNotEmpty()) {
                        mutex.withLock {
                            downloadProgress.value = downloadedIndices.size.toFloat() / totalFrames
                            downloadedFrames.value = downloadedIndices.toList()
                        }
                    }

                    // Create list of frames that need to be downloaded
                    val framesToDownload = (1..totalFrames).filter { !downloadedIndices.contains(it) }

                    // If everything is already downloaded, set progress to 100% and return
                    if (framesToDownload.isEmpty()) {
                        downloadProgress.value = 1f
                        return@launch
                    }

                    // Set up batching - process N frames at a time
                    val batchSize = 10 // Adjust based on your server's capabilities
                    val batches = framesToDownload.chunked(batchSize)

                    for (batch in batches) {
                        // Check if download was canceled
                        if (!isActive) {
                            mutex.withLock {
                                downloadProgress.value = if (downloadedIndices.isEmpty()) 0f
                                                       else downloadedIndices.size.toFloat() / totalFrames
                            }
                            return@launch
                        }

                        // Process each batch in parallel
                        val deferreds = batch.map { i ->
                            async(Dispatchers.IO) {
                                val frameFile = File(cacheDir, "frame_$i.jpg")
                                try {
                                    val frameBytes = viewApi.download("${clip.name}/cam-1-${i.toString().padStart(5, '0')}.jpg".lowercase())
                                    frameFile.writeBytes(frameBytes)
                                    i // Return the frame index if successful
                                } catch (e: Exception) {
                                    LL.e("Failed to download frame $i: ${e.message}")
                                    null // Return null if failed
                                }
                            }
                        }

                        // Wait for all downloads in this batch to complete
                        val completedFrames = deferreds.awaitAll().filterNotNull()

                        // Update progress atomically
                        mutex.withLock {
                            downloadedIndices.addAll(completedFrames)
                            downloadProgress.value = downloadedIndices.size.toFloat() / totalFrames
                            downloadedFrames.value = downloadedIndices.toList()
                        }
                    }

                    mutex.withLock {
                        downloadProgress.value = 1f
                    }
                }
            } catch (e: Exception) {
                LL.e("Download error: ${e.message}")
                downloadProgress.value = 0f
            } finally {
                // Remove reference to the job when done
                downloadJob = null
            }
        }
    }

    /**
     * Stop the current download
     */
    fun stopDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloadProgress.value = 0f
        downloadedFrames.value = emptyList()
    }

    /**
     * Set the current frame index for playback
     */
    fun setCurrentFrame(index: Int) {
        currentFrameIndex.value = index
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
        val file = File(context.filesDir, "timelapses/${clip.name}/frame_$frameIndex.jpg")
        return if (file.exists()) file else null
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
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = File(context.filesDir, "timelapses/${clip.name}")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
                cacheDir.delete()
            }

            // Reset download state
            withContext(Dispatchers.Main) {
                downloadedFrames.value = emptyList()
                downloadProgress.value = 0f
            }
        }
    }

    /**
     * Gets the size of the cache for this clip
     */
    suspend fun getCacheSize(context: Context): Long {
        return withContext(Dispatchers.IO) {
            val cacheDir = File(context.filesDir, "timelapses/${clip.name}")
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
                        _exportProgress.value = progress
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
        _exportProgress.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        downloadJob?.cancel()
        exportJob?.cancel()
        videoExportRepository.cancelExport()
    }
}
