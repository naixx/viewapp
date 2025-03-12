package com.github.naixx.viewapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

        Column {
            Button(onClick = {
                // onSend()
            }) {
                Text("Send")
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
