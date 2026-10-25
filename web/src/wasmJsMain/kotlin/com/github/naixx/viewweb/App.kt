package com.github.naixx.viewweb

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.github.naixx.viewapp.ui.MainScreen
import com.github.naixx.viewapp.ui.components.frames
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import com.github.naixx.viewapp.utils.ClipMapper
import com.github.naixx.viewapp.utils.LocalActivityViewModelStoreOwner
import com.github.naixx.viewapp.utils.ToastMessageHandler
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import github.naixx.core.FrameStore

/**
 * The page: MainScreen, set up the way MainActivity sets up the Android one. The page is the browser's Activity:
 * [page] (one store for its lifetime) serves `activityViewModel()` and holds the per-entry stores of `screenViewModel()`
 * (Navigation 3 in MainScreen, the same on every platform).
 */
@Composable
fun App(viewModelFactory: MetroViewModelFactory, frameStore: FrameStore, page: ViewModelStoreOwner) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory()) // clip previews by URL (/download)
                add(ClipMapper()) // thumbnails and clips carry their JPEG as base64
                frames(frameStore)
            }
            .build()
    }
    CompositionLocalProvider(
        LocalMetroViewModelFactory provides viewModelFactory,
        LocalActivityViewModelStoreOwner provides page,
    ) {
        ViewAppTheme {
            Surface(Modifier.fillMaxSize()) {
                MainScreen()
            }
            ToastMessageHandler()
        }
    }
}
