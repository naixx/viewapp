package com.github.naixx.viewapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import com.github.naixx.viewapp.utils.LocalActivityViewModelStoreOwner
import com.github.naixx.viewapp.utils.activityViewModel
import github.naixx.network.ConnectionState
import github.naixx.network.ConnectionState.Connecting

/** The shell's one screen: the Status and Clips tabs, and the clip the Clips tab opens over them. */
@Composable
fun MainScreen() {
    val viewModel = activityViewModel<MainViewModel>()
    val conn by viewModel.connectionState.collectAsState()

    MainScreen(conn, rememberNavigation())
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreen(conn: ConnectionState, nav: Navigation) {
    val window = currentWindowAdaptiveInfoV2()
    val wide = window.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val directive = calculatePaneScaffoldDirective(window)
    val listDetail = rememberListDetailSceneStrategy<Route>(directive = directive)
    val paned = directive.maxHorizontalPartitions > 1
    Scaffold(
        bottomBar = { if (!wide && nav.current !is ClipInfoRoute) TabBar(conn, nav) }
    ) { padding ->
        Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (wide) TabRail(conn, nav)
            NavDisplay(
                backStack = nav.backStack,
                onBack = { nav.back() },
                modifier = Modifier.weight(1f),
                sceneStrategies = listOf(listDetail),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator<Route>(LocalActivityViewModelStoreOwner.current),
                ),
                entryProvider = entryProvider {
                    entry<StatusRoute> { StatusScreen() }
                    entry<ClipsRoute>(metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { NoClipSelected() })) {
                        ClipsScreen(selected = (nav.current as? ClipInfoRoute)?.clip, onOpen = nav::open)
                    }
                    entry<ClipInfoRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                        ClipInfoScreen(it.clip, onBack = if (paned) null else nav::back)
                    }
                },
            )
        }
    }
}

@Composable
private fun TabBar(conn: ConnectionState, nav: Navigation) {
    NavigationBar(Modifier.height(60.dp), windowInsets = WindowInsets(0, 8, 0, 0)) {
        Tabs(conn, nav, dot = Modifier) { title, icon, selected, onClick ->
            NavigationBarItem(
                alwaysShowLabel = false,
                label = { Text(title) },
                selected = selected,
                onClick = onClick,
                icon = { Icon(icon, contentDescription = title) },
            )
        }
    }
}

@Composable
private fun TabRail(conn: ConnectionState, nav: Navigation) {
    NavigationRail {
        Tabs(conn, nav, dot = Modifier.padding(vertical = 12.dp)) { title, icon, selected, onClick ->
            NavigationRailItem(
                alwaysShowLabel = false,
                label = { Text(title) },
                selected = selected,
                onClick = onClick,
                icon = { Icon(icon, contentDescription = title) },
            )
        }
    }
}

@Composable
private fun Tabs(
    conn: ConnectionState,
    nav: Navigation,
    dot: Modifier,
    item: @Composable (title: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) -> Unit,
) {
    item("Status", Icons.Default.Timelapse, nav.tab == StatusRoute) { nav.switchTo(StatusRoute) }
    Box(
        dot.size(4.dp).clip(CircleShape).background(
            when (conn) {
                is ConnectionState.Connected -> Color(0xFF4AE723)
                ConnectionState.Connecting   -> Color(0xFFFFA500)
                else                         -> Color.Red
            }
        )
    )
    item("Clips", Icons.Default.VideoLibrary, nav.tab == ClipsRoute) { nav.switchTo(ClipsRoute) }
}

@Preview
@Composable
fun PreviewMainScreen() {
    ViewAppTheme {
        MainScreen(Connecting, rememberNavigation())
    }
}
