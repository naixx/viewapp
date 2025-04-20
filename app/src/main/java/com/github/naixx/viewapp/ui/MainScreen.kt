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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.*
import com.github.naixx.compose.painter
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import com.github.naixx.viewapp.utils.activityViewModel
import github.naixx.network.ConnectionState

object MainScreen : Screen {

    @Composable
    override fun Content() {
        MainScreen()
    }
}

@Composable
fun MainScreen(

) {

    val viewModel = activityViewModel<MainViewModel>()
    val conn by viewModel.connectionState.collectAsState()

    val tabs = listOf(
        StatusTab(listOf()),
        ClipsTab()
    )
    TabNavigator(tabs.first()) {
        Scaffold(
            bottomBar = {
                NavigationBar(Modifier.height(60.dp), windowInsets = WindowInsets(0, 8, 0, 0)) {
                    TabNavigationItem(tabs[0])

                    Box(
                        modifier = Modifier.size(4.dp).clip(CircleShape).background(
                            when (conn) {
                                is ConnectionState.Connected -> Color(0xFF4AE723)
                                ConnectionState.Connecting   -> Color(0xFFFFA500)
                                else                         -> Color.Red
                            }
                        )
                    )
                    TabNavigationItem(tabs[1])

                }
            }
        ) {
            Box(modifier = Modifier.padding(it)) {
                CurrentTab()
            }
        }
    }
}

@Composable
fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        alwaysShowLabel = false,
        label = { Text(tab.options.title) },
        selected = tabNavigator.current.key == tab.key,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(painter = tab.options.icon!!, contentDescription = tab.options.title) }
    )
}

private data class StatusTab(
    val messages: List<String>,
) : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = "Status",
            icon = Icons.Default.Timelapse.painter
        )

    @Composable
    override fun Content() {
        StatusScreen().Content()
    }
}

private class ClipsTab() : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 1u,
            title = "Clips",
            icon = Icons.Default.VideoLibrary.painter
        )

    @Composable
    override fun Content() {
        ClipsScreen().Content()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ViewAppTheme {
        Greeting("Android")
    }
}

@Preview
@Composable
fun PreviewMainScreen() {
    ViewAppTheme {
        MainScreen()
    }
}
