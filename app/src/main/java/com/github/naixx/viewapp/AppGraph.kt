package com.github.naixx.viewapp

import android.app.Application
import android.content.Context
import com.github.naixx.viewapp.encoding.VideoEncoderFactory
import com.github.naixx.viewapp.encoding.VideoEncoderFactoryImpl
import com.github.naixx.viewapp.encoding.VideoExportRepository
import com.github.naixx.viewapp.encoding.VideoExportRepositoryImpl
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import github.naixx.core.FileSystemFrameStore
import github.naixx.core.FrameStore
import kotlinx.coroutines.Dispatchers
import kotlinx.io.files.Path

/**
 * The Android app's dependency graph, built once by [App]. Everything in `:network`, `:core` and `:db` arrives
 * through `AppScope` contributions; the Android-only bindings are in [AppBindings]. The Activity and the service are
 * constructor-injected: `MetroAppComponentFactory` (manifest, API 28+) takes them from the
 * [MetroAppComponentProviders] maps this graph fills (`@ActivityKey` / `@ServiceKey` + `@ContributesIntoMap`). The view
 * models are the [ViewModelGraph] maps (`@ViewModelKey` + `@ContributesIntoMap`) behind [AppViewModelFactory]; they stay
 * unscoped, so each `ViewModelStore` owns its instances.
 */
@DependencyGraph(AppScope::class)
interface AppGraph : MetroAppComponentProviders, ViewModelGraph {

    @DependencyGraph.Factory
    fun interface Factory {

        fun create(@Provides application: Application): AppGraph
    }
}

@ContributesTo(AppScope::class)
@BindingContainer
object AppBindings {

    @Provides
    fun context(application: Application): Context = application

    @Provides
    @SingleIn(AppScope::class)
    fun frameStore(application: Application): FrameStore =
        FileSystemFrameStore(Path(application.filesDir.path), Dispatchers.IO)

    @Provides
    @SingleIn(AppScope::class)
    fun videoEncoderFactory(context: Context, frames: FrameStore): VideoEncoderFactory =
        VideoEncoderFactoryImpl(context, frames)

    @Provides
    @SingleIn(AppScope::class)
    fun videoExportRepository(context: Context, encoders: VideoEncoderFactory): VideoExportRepository =
        VideoExportRepositoryImpl(context, encoders)
}
