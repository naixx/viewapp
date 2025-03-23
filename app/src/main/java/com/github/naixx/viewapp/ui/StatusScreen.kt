package com.github.naixx.viewapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.screen.Screen
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.utils.activityViewModel
import github.naixx.network.ConnectionState

class StatusScreen(
    private val messages: List<String>,
) : Screen {

    @Composable
    override fun Content() {
        val viewModel = activityViewModel<MainViewModel>()
        val conn by viewModel.connectionState.collectAsState()
        val connected by viewModel.connectedMessage.collectAsState()
        val settings by viewModel.settingsMessage.collectAsState()
        val battery by viewModel.batteryMessage.collectAsState()

        //todo this is the worst shittycode I wrote, but it works
        val c = LocalContext.current as MainActivity
        val bound by c.isBound.collectAsState()
        LL.e(battery)
        val str = when(val con = conn) {
            is ConnectionState.Connected     -> con.address.address
            is ConnectionState.LoginRequired -> con.address.fromUrl
            else-> con
        }

        Column {
            if (!bound)
                VButton("Start $str") {
                    c.startService()
                }
            else
                VButton("Stop $str") {
                    c.stopService()
                }
            connected?.let {
                Text(it.model + settings?.let { " " + it.settings.battery.toInt() + "%" } + (battery?.let { ", VIEW battery " + it.percentage.toInt() + "%" }
                    ?: ""))
            }
        }
    }
}
