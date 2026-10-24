package com.github.naixx.viewweb

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import github.naixx.core.DeviceState

@Composable
fun App(device: DeviceState) {
    ViewAppTheme {
        Surface(Modifier.fillMaxSize()) {
            StatusScreen(device)
        }
    }
}
