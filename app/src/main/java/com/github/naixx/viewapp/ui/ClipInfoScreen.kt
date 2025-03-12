package com.github.naixx.viewapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.*
import com.github.naixx.compose.*
import com.github.naixx.viewapp.utils.activityViewModel
import github.naixx.network.*
import java.time.*
import java.time.format.DateTimeFormatter

class ClipInfoScreen(val clip: Clip) : Screen {

    @Composable
    override fun Content() {
        val viewModel = activityViewModel<MainViewModel>()
        val conn by viewModel.connectionState.collectAsState()
        var clipInfo: UiState<TimelapseClipInfo?> by remember { mutableStateOf(UiState.Loading) }
        val navigator = LocalNavigator.currentOrThrow

        OnConnectedEffect(conn) {
            clipInfo = viewModel.clipInfo(clip)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            AppBarWithBack(title = clip.name, onBackClick = { navigator.pop() })

            clipInfo.Render({
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading clip info...")
                }
            }) {
                it?.let { info ->
                    InfoView(info)
                }
            }
        }
    }
}

@Composable
private fun InfoView(info: TimelapseClipInfo) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${info.name} Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Format date properly
                val formattedDate = formatDate(info.date)
                Text(text = formattedDate, color = Color.LightGray)

                Spacer(modifier = Modifier.height(8.dp))

                info.frames?.let { frames ->
                    Text(
                        text = "$frames frames (${calculateDuration(frames, 30)} at ${30}fps)",
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val shutterValue = info.status.cameraSettings.shutter
                val apertureValue = info.status.cameraSettings.aperture
                val isoValue = info.status.cameraSettings.iso
                Text(
                    text = "Start exposure: $shutterValue f/$apertureValue $isoValue ISO",
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Divider(color = Color.DarkGray, thickness = 0.5.dp)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Interval: (${info.program.intervalMode}) day ${info.program.dayInterval}s, night ${info.program.nightInterval}s",
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { /* Download functionality */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.LightGray
                        )
                    }

                    IconButton(onClick = { /* Delete functionality */ }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy, HH:mm:ss a")
        localDateTime.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

private fun calculateDuration(frames: Int, fps: Int): String {
    val seconds = frames / fps

    return "${seconds}s"
}

@Composable
fun OnConnectedEffect(c: ConnectionState, action: suspend (ConnectionState.Connected) -> Unit) {
    if (c is ConnectionState.Connected)
        LaunchedEffect(c) {
            action(c)
        }
}

@Preview(showSystemUi = true)
@Composable
fun InfoViewPreview() {
    MaterialTheme(darkColorScheme()) {
//    ViewAppTheme {
        InfoView(sampleClipInfo)
    }
}
