package com.github.naixx.viewapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.*
import coil3.compose.AsyncImagePainter.State.Empty.painter
import com.github.naixx.compose.painter
import com.github.naixx.viewapp.R
import com.github.naixx.viewapp.ui.theme.ViewAppTheme

object MainScreen : Screen {

    @Composable
    override fun Content() {
        MainScreen()
    }
}

@Composable
fun MainScreen(

) {

    val tabs = listOf(
        StatusTab(listOf()),
        ClipsTab()
    )
    TabNavigator(tabs.first()) {
        Scaffold(
            bottomBar = {
                NavigationBar(Modifier.height(60.dp), windowInsets = WindowInsets(0,8,0,0)) {
                    tabs.forEach { tab ->
                        TabNavigationItem(tab)
                    }
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
        StatusScreen(messages).Content()
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
