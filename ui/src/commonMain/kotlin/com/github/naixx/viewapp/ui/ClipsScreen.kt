package com.github.naixx.viewapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.naixx.viewapp.ui.components.*
import com.github.naixx.viewapp.utils.activityViewModel
import com.github.naixx.viewapp.utils.screenViewModel
import github.naixx.network.*

/** The Clips tab: the device's clips; [onOpen] shows one (MainScreen pushes its route). */
@Composable
fun ClipsScreen(onOpen: (Clip) -> Unit) {
    val viewModel = screenViewModel<ClipsViewModel>()
    val mainViewModel = activityViewModel<MainViewModel>()
    val conn by mainViewModel.connectionState.collectAsState()

    OnConnectedEffect(conn) {
        viewModel.refreshClips()
    }

    val clips = viewModel.clips.collectAsState()
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        clips.value.forEach { clip ->
            val model =
                if (!clip.imageBase64.isNullOrBlank())
                    clip
                else
                    (conn as? ConnectionState.Connected)?.address?.fromUrl + "download?file=${clip.name.lowercase()}%2fcam-1-00001.jpg"
            ClipItem(clip, model, onClick = { onOpen(clip) })
        }
    }
}

@Composable
private fun ClipItem(clip: Clip, model: Any?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(8.dp), onClick = onClick) {
        Column(
            modifier = Modifier.padding(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = model,
                contentDescription = clip.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )
            Text("Name: ${clip.name}")
            Text("Frames: ${clip.frames}")
        }
    }
}
