package com.github.naixx.viewapp.ui

import androidx.lifecycle.*
import com.github.naixx.compose.*
import com.github.naixx.logger.LL
import github.naixx.network.*
import kotlinx.collections.immutable.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.*
import org.koin.core.parameter.parametersOf
import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections

class MainViewModel : ViewModel(), KoinComponent {

    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val clips = MutableStateFlow<ImmutableList<Clip>>(persistentListOf())

    // Maps clipName -> list of downloaded frame indices
     val downloadedFrames = MutableStateFlow<Map<String, List<Int>>>(emptyMap())

    // Maps clipName -> current download progress (0.0 - 1.0)
     val downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())

    // Currently selected frame index for playback
     val currentFrameIndex = MutableStateFlow<Int>(0)

    // Playback state
    val isPlaying = MutableStateFlow(false)

    // Track download jobs to allow cancellation
    private val downloadJobs = mutableMapOf<String, Job>()

    fun requestClips(connection: ConnectionState.Connected) {
        viewModelScope.launch {
            try {
                val viewApi: ViewApi = get<ViewApi> { parametersOf(connection.address.fromUrl) }
                clips.value = viewApi.clips().toImmutableList()
            } catch (e: Exception) {
                LL.e(e)
            }
        }
    }

    suspend fun clipInfo(clip: Clip): UiState<TimelapseClipInfo?> = catching {
        (connectionState.value as? ConnectionState.Connected)?.let { state ->
            val viewApi: ViewApi = get<ViewApi> { parametersOf(state.address.fromUrl) }
            viewApi.clipInfo(clip.name)
        }
    }

    fun downloadFrames(clipName: String, context: Context) {
        if (isPlaying.value) return
        if (downloadProgress.value[clipName] != null && downloadProgress.value[clipName]!! > 0 && downloadProgress.value[clipName]!! < 1f) return

        val job = viewModelScope.launch {
            try {
                val cacheDir = File(context.filesDir, "timelapses/$clipName")
                cacheDir.mkdirs()

                (connectionState.value as? ConnectionState.Connected)?.let { state ->
                    val viewApi: ViewApi = get<ViewApi> { parametersOf(state.address.fromUrl) }
                    val info = viewApi.clipInfo(clipName)
                    val totalFrames = info.frames ?: return@let

                    downloadProgress.value = downloadProgress.value.toMutableMap().apply {
                        put(clipName, 0f)
                    }

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
                            downloadProgress.value = downloadProgress.value.toMutableMap().apply {
                                put(clipName, downloadedIndices.size.toFloat() / totalFrames)
                            }

                            downloadedFrames.value = downloadedFrames.value.toMutableMap().apply {
                                put(clipName, downloadedIndices.toList())
                            }
                        }
                    }

                    // Create list of frames that need to be downloaded
                    val framesToDownload = (1..totalFrames).filter { index ->
                        !downloadedIndices.contains(index)
                    }

                    // If everything is already downloaded, set progress to 100% and return
                    if (framesToDownload.isEmpty()) {
                        downloadProgress.value = downloadProgress.value.toMutableMap().apply {
                            put(clipName, 1f)
                        }
                        return@launch
                    }

                    // Set up batching - process N frames at a time
                    val batchSize = 10 // Adjust based on your server's capabilities
                    val batches = framesToDownload.chunked(batchSize)

                    for (batch in batches) {
                        // Check if download was canceled
                        if (!isActive) {
                            mutex.withLock {
                                downloadProgress.value = downloadProgress.value.toMutableMap().apply {
                                    put(clipName, if (downloadedIndices.isEmpty()) 0f else downloadedIndices.size.toFloat() / totalFrames)
                                }
                            }
                            return@launch
                        }

                        // Process each batch in parallel
                        val deferreds = batch.map { i ->
                            async(Dispatchers.IO) {
                                val frameFile = File(cacheDir, "frame_$i.jpg")
                                try {
                                    val frameBytes = viewApi.download("$clipName/cam-1-${i.toString().padStart(5, '0')}.jpg".lowercase())
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

                            downloadProgress.value = downloadProgress.value.toMutableMap().apply {
                                put(clipName, downloadedIndices.size.toFloat() / totalFrames)
                            }

                            downloadedFrames.value = downloadedFrames.value.toMutableMap().apply {
                                put(clipName, downloadedIndices.toList())
                            }
                        }
                    }

                    mutex.withLock {
                        downloadProgress.value = downloadProgress.value.toMutableMap().apply {
                            put(clipName, 1f)
                        }
                    }
                }
            } catch (e: Exception) {
                LL.e("Download error: ${e.message}")
                downloadProgress.value = downloadProgress.value.toMutableMap().apply {
                    put(clipName, 0f)
                }
            } finally {
                // Remove reference to the job when done
                downloadJobs.remove(clipName)
            }
        }

        // Store the job for potential cancellation
        downloadJobs[clipName] = job
    }

    fun stopDownload(clipName: String) {
        downloadJobs[clipName]?.cancel()
        downloadJobs.remove(clipName)

        downloadProgress.value = downloadProgress.value.toMutableMap().apply {
            put(clipName, 0f)
        }

        // Also clear the downloaded frames for that clip
        downloadedFrames.value = downloadedFrames.value.toMutableMap().apply {
            remove(clipName)  // This is the key fix
        }
    }

    fun stopAllDownloads() {
        downloadJobs.forEach { (clipName, job) ->
            job.cancel()
            downloadProgress.value = downloadProgress.value.toMutableMap().apply {
                put(clipName, 0f)
            }
        }
        downloadJobs.clear()
    }

    fun setCurrentFrame(index: Int) {
        currentFrameIndex.value = index
    }

    fun togglePlayback(playing: Boolean) {
        isPlaying.value = playing
    }

    fun getFrameFile(clipName: String, frameIndex: Int, context: Context): File? {
        val file = File(context.filesDir, "timelapses/$clipName/frame_$frameIndex.jpg")
        return if (file.exists()) file else null
    }

    /**
     * Deletes all cached frames for a specific clip
     */
    fun deleteCache(clipName: String, context: Context) {
        // First stop any ongoing download
        stopDownload(clipName)

        // Reset playback
        if (isPlaying.value) {
            togglePlayback(false)
        }
        setCurrentFrame(0)

        // Delete all cached frames
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = File(context.filesDir, "timelapses/$clipName")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
                cacheDir.delete()
            }

            // Reset download state
            withContext(Dispatchers.Main) {
                downloadedFrames.value = downloadedFrames.value.toMutableMap().apply {
                    remove(clipName)
                }
                downloadProgress.value = downloadProgress.value.toMutableMap().apply {
                    put(clipName, 0f)
                }
            }
        }
    }

    /**
     * Gets the size of the cache for a specific clip
     */
    suspend fun getCacheSize(clipName: String, context: Context): Long {
        return withContext(Dispatchers.IO) {
            val cacheDir = File(context.filesDir, "timelapses/$clipName")
            var size = 0L
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    size += file.length()
                }
            }
            size
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel all pending downloads when viewModel is cleared
        viewModelScope.coroutineContext.cancelChildren()
    }
}
