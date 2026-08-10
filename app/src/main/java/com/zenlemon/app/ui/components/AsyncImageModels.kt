package com.zenlemon.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.zenlemon.app.ui.accessibility.rememberReducedMotionEnabled
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun rememberCrossfadeImageModel(data: Any?): Any? {
    val context = LocalContext.current
    val reducedMotionEnabled = rememberReducedMotionEnabled()
    return remember(context, data, reducedMotionEnabled) {
        data?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(!reducedMotionEnabled)
                .build()
        }
    }
}