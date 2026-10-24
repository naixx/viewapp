package com.github.naixx.viewweb

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.github.naixx.viewapp.ui.StatusScreen
import com.github.naixx.viewapp.ui.components.frames
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import com.github.naixx.viewapp.utils.ClipMapper
import com.github.naixx.viewapp.utils.LocalActivityViewModelStoreOwner
import com.github.naixx.viewapp.utils.ToastMessageHandler
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import github.naixx.core.FrameStore

/**
 * The page: the shared Status screen, set up the way MainActivity sets up the Android one. The page is the browser's
 * Activity, so [page] (one store for its lifetime) serves `activityViewModel()` and, with no navigator yet,
 * `screenViewModel()` too.
 */
@Composable
fun App(viewModelFactory: MetroViewModelFactory, frameStore: FrameStore, page: ViewModelStoreOwner) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(ClipMapper()) // thumbnails and clips carry their JPEG as base64
                frames(frameStore)
            }
            .build()
    }
    CompositionLocalProvider(
        LocalMetroViewModelFactory provides viewModelFactory,
        LocalActivityViewModelStoreOwner provides page,
        LocalViewModelStoreOwner provides page,
    ) {
        ViewAppTheme {
            Surface(Modifier.fillMaxSize()) {
                StatusScreen().Content()
            }
            ToastMessageHandler()
        }
    }
}
