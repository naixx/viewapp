package com.github.naixx.viewapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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

@Composable
fun MainScreen(conn: ConnectionState, nav: Navigation) {
    Scaffold(
        bottomBar = {
            // A clip is shown on its own, over the tabs, as it was when it was pushed on the root navigator.
            if (nav.current !is ClipInfoRoute) {
                NavigationBar(Modifier.height(60.dp), windowInsets = WindowInsets(0, 8, 0, 0)) {
                    TabItem("Status", Icons.Default.Timelapse, selected = nav.tab == StatusRoute) { nav.switchTo(StatusRoute) }

                    Box(
                        modifier = Modifier.size(4.dp).clip(CircleShape).background(
                            when (conn) {
                                is ConnectionState.Connected -> Color(0xFF4AE723)
                                ConnectionState.Connecting   -> Color(0xFFFFA500)
                                else                         -> Color.Red
                            }
                        )
                    )
                    TabItem("Clips", Icons.Default.VideoLibrary, selected = nav.tab == ClipsRoute) { nav.switchTo(ClipsRoute) }
                }
            }
        }
    ) { padding ->
        NavDisplay(
            backStack = nav.backStack,
            onBack = { nav.back() },
            // The entries hold inset-aware parts of their own (ClipInfoScreen draws a TopAppBar over the player; until step 13
            // it sat on the root navigator, outside this Scaffold), so the insets this padding already covers are consumed:
            // otherwise the bar pads for the status bar a second time, one bar height too low.
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                // screenViewModel(): a store per entry, cleared when the entry leaves the back stack; they hang off the
                // shell's store (the Activity, the page), the one activityViewModel() uses directly.
                rememberViewModelStoreNavEntryDecorator<Route>(LocalActivityViewModelStoreOwner.current),
            ),
            entryProvider = entryProvider {
                entry<StatusRoute> { StatusScreen() }
                entry<ClipsRoute> { ClipsScreen(onOpen = nav::open) }
                entry<ClipInfoRoute> { ClipInfoScreen(it.clip, onBack = nav::back) }
            },
        )
    }
}

@Composable
private fun RowScope.TabItem(title: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        alwaysShowLabel = false,
        label = { Text(title) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = title) }
    )
}

@Preview
@Composable
fun PreviewMainScreen() {
    ViewAppTheme {
        MainScreen(Connecting, rememberNavigation())
    }
}
