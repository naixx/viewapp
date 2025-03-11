package com.github.naixx.viewapp

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.naixx.logger.LL
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import github.naixx.network.*
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.get

@Composable
fun MainScreen(
    conn: ConnectionState,
    messages: List<String>,
    modifier: Modifier = Modifier,
    onSend: () -> Unit
) {
    val clips = remember { mutableStateOf<List<Clip>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(conn) {
        if (conn is ConnectionState.Connected) {
            coroutineScope.launch {
                try {
                    val viewApi: ViewApi = get(ViewApi::class.java) { parametersOf(conn.address.fromUrl) }
                    clips.value = viewApi.clips()
                } catch (e: Exception) {
                    LL.e(e)
                }
            }
        }
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Button({
            //                            out.tryEmit("test")
            onSend()
        }) { Text("Send") }
        Text(conn.toString())

        if (conn is ConnectionState.Connected && clips.value.isNotEmpty()) {
            Text("Clips:")
            clips.value.forEach { clip ->
                ClipItem(clip = clip)
            }
        } else {
            messages.forEach {
                Greeting(
                    name = it,
                )
            }
        }
    }
}

@Composable
fun ClipItem(clip: Clip, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Column(
            modifier = Modifier.padding(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (clip.imageBase64.isNotEmpty()) {
                val decodedBitmap = remember(clip.id) {
                    try {
                        val cleanBase64 = clip.imageBase64.trim()
                        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }
                // Now use the prepared data in composables without try-catch
                if (decodedBitmap != null) {
                    // Display the bitmap directly
                    Image(
                        bitmap = decodedBitmap.asImageBitmap(),
                        contentDescription = clip.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            /*.height(200.dp)*/,
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Text("Name: ${clip.name}")
            Text("Frames: ${clip.frames}")
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
