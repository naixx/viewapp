package com.github.naixx.viewapp.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.navigator.tab.*
import com.github.naixx.viewapp.R
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import github.naixx.network.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    conn: ConnectionState,
    messages: List<String>,
    modifier: Modifier = Modifier,
    onSend: () -> Unit
) {
    ViewAppTheme {
        val tabs = listOf(
            StatusTab(messages),
            ClipsTab()
        )
        TabNavigator(tabs.first()) {
            Scaffold(
                content = {
                    CurrentTab()
                },
                bottomBar = {
                    NavigationBar {
                        tabs.forEach { tab ->
                            TabNavigationItem(tab)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        selected = tabNavigator.current == tab,
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
            icon = painterResource(id = R.drawable.ic_status)
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
            icon = painterResource(id = R.drawable.ic_clips)
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
        MainScreen(ConnectionState.Disconnected, listOf("Hello", "two"), modifier = Modifier, onSend = { })
    }
}

@Preview
@Composable
fun PreviewConnectedMainScreen() {
    ViewAppTheme {
        MainScreen(
            ConnectionState.Connected(
                AddressResponse.Address("test"),
                true
            ),
            listOf(),
            modifier = Modifier,
            onSend = { }
        )
    }
}
