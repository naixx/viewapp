package com.github.naixx.viewapp

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import github.naixx.network.ConnectionState

@Composable
fun MainScreen(
    conn: ConnectionState,
    messages: List<String>,
    modifier: Modifier = Modifier,
    onSend: () -> Unit
) {
    Column(modifier = modifier) {
        Button({
            //                            out.tryEmit("test")
            onSend()
        }) { Text("Send") }
        Text(conn.toString())

        messages.forEach {
            Greeting(
                name = it,
            )
        }
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
