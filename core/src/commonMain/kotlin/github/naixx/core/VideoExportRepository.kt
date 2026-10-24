package github.naixx.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Turns cached frames of a clip into a video and owns the running export: one at a time, outliving the screen that
 * started it. The shell decides whether it exists: `:app` binds the MediaCodec one as `VideoExportRepository?` (the key
 * of `TimelapseViewModel.export`; Metro keys include nullability), `:web` binds nothing and the view model's default
 * null hides the feature.
 */
interface VideoExportRepository {

    /** 0..1 while an export runs, 0 otherwise. */
    val progress: StateFlow<Float>

    /** The outcome of the last export; null until one has finished, and again while the next one runs. */
    val result: StateFlow<ExportResult?>

    /** Encodes [frames] (frame numbers of [clip] in the [FrameStore]) into a video; ignored while an export runs. */
    fun export(clip: String, frames: List<Int>, fps: Int = 30)

    fun cancel()

    /** Shows [video] in the platform's player. */
    fun open(video: ExportResult.Success)
}

sealed class ExportResult {

    data class Success(val clip: String, val path: String) : ExportResult()
    data class Error(val message: String, val cause: Throwable? = null) : ExportResult()
    object Canceled : ExportResult()
}
