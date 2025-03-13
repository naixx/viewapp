package com.github.naixx.viewapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.screen.Screen
import com.github.naixx.viewapp.utils.activityViewModel
import github.naixx.network.ConnectionState

class StatusScreen(
    private val messages: List<String>,
) : Screen {

    @Composable
    override fun Content() {
        val viewModel = activityViewModel<MainViewModel>()
        val conn by viewModel.connectionState.collectAsState()

        //todo this is the worst shittycode I wrote, but it works
        val c = LocalContext.current as MainActivity
        val bound by c.isBound.collectAsState()

        Column {
            if (!bound)
                VButton("Start") {
                    c.startService()
                }
            else
                VButton("Stop") {
                    c.stopService()
                }
            Text(conn.toString())

            if (conn !is ConnectionState.Connected) {
                messages.forEach {
                    Greeting(name = it)
                }
            }
        }
    }
}
