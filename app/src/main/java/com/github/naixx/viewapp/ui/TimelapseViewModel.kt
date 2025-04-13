package com.github.naixx.viewapp.ui

import android.content.Context
import androidx.lifecycle.*
import com.github.naixx.compose.*
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.encoding.*
import github.naixx.db.TimelapseClipInfoDao
import github.naixx.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.core.component.*
import org.koin.core.parameter.parametersOf
import java.io.File

class TimelapseViewModel(private val clip: Clip) : ViewModel(), KoinComponent {

    val downloadedFrames: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    val currentFrameIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val exportProgress: MutableStateFlow<Float> = MutableStateFlow(0f)

    val exportResult = MutableSharedFlow<EncodingResult>(extraBufferCapacity = 1)

    private var downloadJob: Job? = null
    private var exportJob: Job? = null
    private var totalFrames: Int = clip.frames
    private val videoExportRepository: VideoExportRepository by inject()
    private val timelapseClipInfoDao: TimelapseClipInfoDao by inject()

    val clipInfo: StateFlow<TimelapseClipInfo?> = timelapseClipInfoDao
        .getTimelapseClipInfo(clip.name)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Returns whether frames are currently being downloaded
     */
    fun isDownloading(): Boolean = downloadJob?.isActive == true

    /**
     * Checks for existing frames without starting download
     * Returns Pair(downloadedCount, totalFrames)
     */
    suspend fun checkExistingFrames(context: Context): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            val cacheDir = File(context.filesDir, "timelapses/${clip.name}")
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
        if (isPlaying.value || isDownloading()) return

        downloadJob = viewModelScope.launch {
            try {
                val cacheDir = File(context.filesDir, "timelapses/${clip.name}").apply { mkdirs() }

                (connectionState as? ConnectionState.Connected)?.let { state ->
                    val viewApi: ViewApi = get { parametersOf(state.address.fromUrl) }
                    val info = viewApi.clipInfo(clip.name).also {
                        timelapseClipInfoDao.insertTimelapseClipInfo(listOf(it))
                    }

                    totalFrames = info.frames ?: return@let

                    val downloadedIndices = (1..totalFrames).filter { i ->
                        File(cacheDir, "frame_$i.jpg").exists()
                    }.toMutableList()

                    downloadedFrames.value = downloadedIndices.sorted()

                    val framesToDownload = (1..totalFrames) - downloadedIndices
                    if (framesToDownload.isEmpty()) return@launch

                    val batchSize = 10
                    framesToDownload.chunked(batchSize).forEach { batch ->
                        if (!isActive) {
                            downloadedFrames.value = downloadedIndices.sorted()
                            return@launch
                        }

                        val results = batch.map { i ->
                            async(Dispatchers.IO) {
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
                                } catch (e: Exception) {
                                    LL.e("Failed to download frame $i: ${e.message}")
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()

                        downloadedIndices.addAll(results)
                        downloadedFrames.value = downloadedIndices.sorted()
                    }
                }
            } catch (e: Exception) {
                LL.d("Download error: ${e.message}")
                downloadedFrames.value = emptyList()
            } finally {
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
            downloadedFrames.value = emptyList()
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
