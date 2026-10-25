package com.github.naixx.viewapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@Composable
fun ClipsScreen(selected: Clip?, onOpen: (Clip) -> Unit) {
    val viewModel = screenViewModel<ClipsViewModel>()
    val mainViewModel = activityViewModel<MainViewModel>()
    val conn by mainViewModel.connectionState.collectAsState()

    OnConnectedEffect(conn) {
        viewModel.refreshClips()
    }

    val clips by viewModel.clips.collectAsState()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 240.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(clips) { clip ->
            val model =
                if (!clip.imageBase64.isNullOrBlank())
                    clip
                else
                    (conn as? ConnectionState.Connected)?.address?.fromUrl + "download?file=${clip.name.lowercase()}%2fcam-1-00001.jpg"
            ClipItem(clip, model, selected = clip.name == selected?.name, onClick = { onOpen(clip) })
        }
    }
}

@Composable
fun NoClipSelected() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Choose a clip", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ClipItem(clip: Clip, model: Any?, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
