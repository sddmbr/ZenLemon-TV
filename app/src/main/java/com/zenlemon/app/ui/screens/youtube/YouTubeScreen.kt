package com.zenlemon.app.ui.screens.youtube

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeScreen(
    viewModel: YouTubeViewModel = hiltViewModel()
) {
    val currentVideoId by viewModel.currentVideoId.collectAsStateWithLifecycle()

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
            // Building standard HTML to render an iframe, just like a website
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