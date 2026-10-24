package com.github.naixx.viewapp.encoding

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.naixx.logger.LL
import github.naixx.core.ExportResult
import github.naixx.core.VideoExportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * The Android [VideoExportRepository]: MediaCodec through [VideoEncoderFactory], the file under `cacheDir/export/` plus
 * the encoder's copy in MediaStore, which [open] shows. The export runs in the process [scope], so it outlives the
 * screen that started it; [cancel] asks the encoder to stop, and the result is [ExportResult.Canceled].
 */
class VideoExportRepositoryImpl(
    private val context: Context,
    private val videoEncoderFactory: VideoEncoderFactory,
    private val scope: CoroutineScope,
) : VideoExportRepository {

    override val progress: StateFlow<Float>
        field = MutableStateFlow(0f)

    override val result: StateFlow<ExportResult?>
        field = MutableStateFlow<ExportResult?>(null)

    private var job: Job? = null
    private var current: VideoEncoder? = null

    /** MediaStore URIs of the exported videos by path, for [open]. */
    private val videos = HashMap<String, Uri>()

    override fun export(clip: String, frames: List<Int>, fps: Int) {
        if (job?.isActive == true) return
        job = scope.launch {
            result.value = null
            progress.value = 0f
            val outputPath = "${context.cacheDir.absolutePath}/export/${clip}_${System.currentTimeMillis()}.mp4"
            File(outputPath).parentFile?.mkdirs()
            val encoder = videoEncoderFactory.createEncoder().also { current = it }
            result.value = try {
                when (val encoded = encoder.encodeFramesToVideo(clip, frames, outputPath, fps) { progress.value = it }) {
                    is EncodingResult.Success -> {
                        encoded.uri?.let { videos[encoded.outputPath] = it }
                        ExportResult.Success(clip, encoded.outputPath)
                    }

                    is EncodingResult.Error -> ExportResult.Error(encoded.message, encoded.exception)
                    EncodingResult.Canceled -> ExportResult.Canceled
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LL.e("Error during video export: ${e.message}")
                ExportResult.Error("Export failed: ${e.message}", e)
            } finally {
                progress.value = 0f
            }
        }
    }

    override fun cancel() {
        current?.cancel()
        progress.value = 0f
    }

    override fun open(video: ExportResult.Success) {
        val uri = videos[video.path] ?: return
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
