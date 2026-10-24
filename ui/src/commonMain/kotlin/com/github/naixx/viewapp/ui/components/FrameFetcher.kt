package com.github.naixx.viewapp.ui.components

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import github.naixx.core.FrameStore
import okio.Buffer

/** A cached time-lapse frame as a Coil model; the `ImageLoader` of the shell resolves it with [FrameFetcher]. */
data class FrameKey(val clip: String, val index: Int)

/**
 * Reads the JPEG of a frame from the [FrameStore]; Coil decodes it, samples it to the size of the view and keeps it in
 * its memory cache, so seeking back and forth does not decode the same frame twice. A frame that is not cached yields
 * null: Coil reports an error and the player draws nothing for it.
 */
class FrameFetcher(private val frames: FrameStore) : Fetcher.Factory<FrameKey> {

    override fun create(data: FrameKey, options: Options, imageLoader: ImageLoader) = Fetcher {
        frames.read(data.clip, data.index)?.let { jpeg ->
            SourceFetchResult(
                source = ImageSource(Buffer().write(jpeg), options.fileSystem),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK,
            )
        }
    }
}

/** What a shell adds to its `ImageLoader` so that `TimelapsePlayer` can show frames: `components { frames(store) }`. */
fun ComponentRegistry.Builder.frames(frames: FrameStore) {
    add(FrameFetcher(frames))
    add({ data: FrameKey, _ -> "frame:${data.clip}/${data.index}" })
}
