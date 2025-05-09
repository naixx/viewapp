package com.github.naixx.viewapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import com.github.naixx.viewapp.ui.components.*
import com.github.naixx.viewapp.utils.activityViewModel
import github.naixx.network.*

class ClipsScreen() : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { ClipsViewModel() }
        val mainViewModel = activityViewModel<MainViewModel>()
        val conn by mainViewModel.connectionState.collectAsState()

        OnConnectedEffect(conn) {
            viewModel.refreshClips(it)
        }

        val clips = viewModel.clips.collectAsState()
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            clips.value.forEach { clip ->
                val model =
                    if (!clip.imageBase64.isNullOrBlank())
                        clip
                    else
                        (conn as? ConnectionState.Connected)?.address?.fromUrl + "download?file=${clip.name.lowercase()}%2fcam-1-00001.jpg"
                ClipItem(clip = clip, model)
            }

        }
    }
}

@Composable
private fun ClipItem(clip: Clip, model: Any?, modifier: Modifier = Modifier) {
    val navigator = SuperNav

    Card(modifier = modifier.fillMaxWidth().padding(8.dp), onClick = {
        navigator?.push(ClipInfoScreen(clip))
    }) {
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
