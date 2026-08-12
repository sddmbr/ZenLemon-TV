package com.zenlemon.app.ui.screens.squeeze

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenlemon.app.ui.components.PlayerRenderView
import com.zenlemon.player.PlayerRenderSurfaceType
import com.zenlemon.player.PlayerSurfaceResizeMode

@Composable
fun SqueezeScreen(
    viewModel: SqueezeViewModel = hiltViewModel()
) {
    val currentVideoId by viewModel.currentVideoId.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val playerEngine by viewModel.playerEngine.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Row(modifier = Modifier.fillMaxSize()) {
        // Player section
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val engine = playerEngine
            if (engine != null) {
                PlayerRenderView(
                    playerEngine = engine,
                    resizeMode = PlayerSurfaceResizeMode.FIT,
                    surfaceType = PlayerRenderSurfaceType.AUTO,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Queue section
        Surface(
            modifier = Modifier.weight(0.3f).fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ZenLemon Squeeze",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(queue) { video ->
                        SqueezeQueueItem(
                            video = video,
                            isSelected = video.id == currentVideoId,
                            onClick = { viewModel.playVideo(video.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SqueezeQueueItem(
    video: SqueezeVideo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = video.title,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}
