package com.github.naixx.viewapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import github.naixx.network.Clip
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

/** The destinations: the two tabs and the clip the Clips tab opens. Serializable, so the back stack survives a config change. */
@Serializable
sealed interface Route : NavKey

@Serializable
data object StatusRoute : Route

@Serializable
data object ClipsRoute : Route

@Serializable
data class ClipInfoRoute(val clip: Clip) : Route

@Composable
fun rememberNavigation(): Navigation {
    val clipsTab = rememberSaveable { mutableStateOf(false) }
    // The routes are a sealed hierarchy, so their serializer knows every subclass; `rememberNavBackStack` would have
    // needed a hand-written module because it types the stack by the open `NavKey`.
    val clips = rememberSerializable(serializer = NavBackStackSerializer(serializer<Route>())) { NavBackStack<Route>(ClipsRoute) }
    return remember { Navigation(clipsTab, clips) }
}

/**
 * What the shell shows: the current tab and what the Clips tab has pushed. The Status tab pushes nothing, so it is the
 * one entry it always was; the Clips tab's stack is `[Clips, ClipInfo?]`. [backStack] puts Status first whatever the
 * tab, so the user leaves the app through Status ("exit through home") and back on the Clips root returns to it.
 */
class Navigation(private val clipsTab: MutableState<Boolean>, private val clips: NavBackStack<Route>) {

    val tab: Route get() = if (clipsTab.value) ClipsRoute else StatusRoute

    /** The entry on top. */
    val current: Route get() = if (clipsTab.value) clips.last() else StatusRoute

    /** The list `NavDisplay` renders: `[Status]`, or `[Status, Clips, ClipInfo?]` on the Clips tab. */
    val backStack: List<Route> get() = if (clipsTab.value) listOf(StatusRoute) + clips else listOf(StatusRoute)

    fun switchTo(tab: Route) {
        clipsTab.value = tab == ClipsRoute
    }

    fun open(clip: Clip) {
        clips.add(ClipInfoRoute(clip))
    }

    fun back() {
        if (clips.size > 1) clips.removeLastOrNull() else clipsTab.value = false
    }
}
