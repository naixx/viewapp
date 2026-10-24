package com.github.naixx.viewapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import dev.zacsweers.metrox.viewmodel.metroViewModel

/*
 * Two scopes, named at the call site. A bare metroViewModel() would take whatever LocalViewModelStoreOwner is current
 * and hide which of the two is meant — on Android Voyager gives every Screen its own store (AndroidScreenLifecycleOwner),
 * so "shared" and "per screen" differ. Both build the view model through the graph's factory (LocalMetroViewModelFactory).
 */

/**
 * The store behind [activityViewModel]: the Activity on Android, the page in the browser. The shell provides it once,
 * above the navigator, because inside a screen `LocalViewModelStoreOwner` is already the screen's own store.
 */
val LocalActivityViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> {
    error("LocalActivityViewModelStoreOwner is not provided: the shell sets it above the navigator")
}

/**
 * A view model shared by every screen: it lives in the shell's `ViewModelStore` ([LocalActivityViewModelStoreOwner]), so
 * `MainScreen`, `StatusScreen` and `ClipsScreen` see one instance until the Activity is finished (`MainViewModel`,
 * `ProgramViewModel`).
 */
@Composable
inline fun <reified VM : ViewModel> activityViewModel(): VM =
    metroViewModel(viewModelStoreOwner = LocalActivityViewModelStoreOwner.current)

/**
 * A view model owned by the current screen: Voyager's per-`Screen` store, cleared when the screen leaves the navigator —
 * what `rememberScreenModel` used to give (`ClipsViewModel`). Per-clip view models use `assistedMetroViewModel(key =
 * clip.name)` on the same store.
 */
@Composable
inline fun <reified VM : ViewModel> screenViewModel(key: String? = null): VM = metroViewModel(key = key)
