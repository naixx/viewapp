package com.github.naixx.viewapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import github.naixx.network.ConnectionState

@Composable
fun OnConnectedEffect(c: ConnectionState, action: suspend (ConnectionState.Connected) -> Unit) {
    if (c is ConnectionState.Connected)
        LaunchedEffect(c) {
            action(c)
        }
}
