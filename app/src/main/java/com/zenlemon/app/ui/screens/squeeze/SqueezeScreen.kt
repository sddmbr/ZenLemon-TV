package com.zenlemon.app.ui.screens.squeeze

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SqueezeScreen(
    viewModel: SqueezeViewModel = hiltViewModel()
) {
    val currentVideoId by viewModel.currentVideoId.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()

    Row(modifier = Modifier.fillMaxSize()) {
        // Player section
        Box(modifier = Modifier.weight(0.7f).fillMaxHeight()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                    }
                },
                update = { webView ->
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <body style="margin:0;padding:0;background-color:#000;">
                            <iframe width="100%" height="100%" 
                                src="https://www.youtube.com/embed/$currentVideoId?autoplay=1&controls=1&showinfo=1&modestbranding=1&fs=1" 
                                frameborder="0" 
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                                allowfullscreen>
                            </iframe>
                        </body>
                        </html>
                    """.trimIndent()
                    
                    webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                }
            )
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
